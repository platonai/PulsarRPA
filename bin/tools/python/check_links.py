#!/usr/bin/env python3
"""
check_links.py

检查文档中所有链接的脚本（支持 Markdown / HTML / reStructuredText-like 文件）
功能：
  1) 校验内部链接（相对/绝对路径）是否指向存在的文件（并可选检查锚点/heading）
  2) 测试外部链接是否可访问（使用 HTTP HEAD 优先，不下载内容以提高速度）
使用：
  python check_links.py --root . --paths docs README.md --ext md,markdown,html,rst --concurrency 20 --timeout 5

输出：
  - 在终端列出找到的坏链（以及原因）
  - 返回非 0 状态码如果检测到坏链（便于 CI 集成）
依赖：
  - requests
可选优化：在 CI 中并行运行（--concurrency 调整并发数）
"""

import re
import os
import sys
import argparse
import logging
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from collections import defaultdict
from typing import List, Tuple, Dict, Set, Optional
import requests

# ---------- 配置 ----------
MARKDOWN_INLINE_LINK_RE = re.compile(r'(?<!\!)\[[^\]]*\]\(([^)]+)\)')  # 排除图片链接开头的 ![]
MARKDOWN_AUTOLINK_RE = re.compile(r'<(https?://[^ >]+)>')
MARKDOWN_REF_DEF_RE = re.compile(r'^\s*\[([^\]]+)\]:\s*(\S+)', re.MULTILINE)
HTML_A_HREF_RE = re.compile(r'<a\s+[^>]*href=["\']([^"\']+)["\']', re.IGNORECASE)
URL_SCHEME_RE = re.compile(r'^[a-zA-Z][a-zA-Z0-9+.-]*:')  # 匹配 scheme:
IGNORED_SCHEMES = ('mailto:', 'tel:', 'javascript:', 'data:')

# HTTP 检查的默认参数
DEFAULT_TIMEOUT = 5  # seconds
DEFAULT_CONCURRENCY = 20
USER_AGENT = "check-links-script/1.0 (+https://github.com/)"

# ---------- 工具函数 ----------
def is_external_link(url: str) -> bool:
    return url.startswith('http://') or url.startswith('https://')

def is_ignored_scheme(url: str) -> bool:
    lower = url.lower()
    return any(lower.startswith(s) for s in IGNORED_SCHEMES)

def normalize_fs_path(path: str) -> str:
    # 移除 query 或 fragment（对于内部文件链接）
    p = path.split('?')[0].split('#')[0]
    return os.path.normpath(p)

def github_anchor_from_heading(text: str) -> str:
    """
    基于 GitHub 风格，从标题文本生成锚点（近似实现）。
    - 转小写
    - 去掉标点（保留空格和中划线）
    - 空格替换为 -
    - 连续 - 合并
    这不是完全跟随所有 edge-case，但对常见标题有效。
    """
    t = text.strip().lower()
    # 移除 github 通常会移除的标点（保留字母数字、空格、-、_）
    t = re.sub(r'[^\w\s\-]', '', t, flags=re.UNICODE)
    t = re.sub(r'\s+', '-', t)
    t = re.sub(r'-{2,}', '-', t)
    return t

def extract_markdown_headings(content: str) -> List[str]:
    headings = []
    for line in content.splitlines():
        m = re.match(r'^\s{0,3}(#{1,6})\s+(.*)$', line)
        if m:
            headings.append(m.group(2).strip())
    return headings

# ---------- 链接提取 ----------
def extract_links_from_markdown(content: str) -> List[str]:
    links = []
    links.extend(MARKDOWN_INLINE_LINK_RE.findall(content))
    links.extend(MARKDOWN_AUTOLINK_RE.findall(content))
    # reference definitions
    for m in MARKDOWN_REF_DEF_RE.findall(content):
        # m => (id, url)
        links.append(m[1])
    return links

def extract_links_from_html(content: str) -> List[str]:
    return HTML_A_HREF_RE.findall(content)

def extract_links_from_file(path: str) -> List[str]:
    ext = os.path.splitext(path)[1].lower()
    with open(path, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    links = []
    if ext in ('.md', '.markdown', '.rst', '.txt'):
        links = extract_links_from_markdown(content)
    elif ext in ('.html', '.htm'):
        links = extract_links_from_html(content)
    else:
        # 试探：先用 markdown 提取，再用 html 提取
        links = extract_links_from_markdown(content) + extract_links_from_html(content)
    return links

# ---------- 检查函数 ----------
class LinkResult:
    def __init__(self, source_file: str, link: str, kind: str):
        self.source_file = source_file
        self.link = link
        self.kind = kind  # 'internal' or 'external' or 'ignored'
        self.ok = True
        self.detail = ""  # 错误信息或状态码等

    def to_dict(self):
        return {
            "source": self.source_file,
            "link": self.link,
            "kind": self.kind,
            "ok": self.ok,
            "detail": self.detail
        }

def check_internal_link(source_file: str, link: str, repo_root: str, check_anchor: bool=True) -> LinkResult:
    """
    校验内部链接指向的文件是否存在；若包含锚点则可选验证锚点是否存在（仅对 Markdown '#' 标题做简单匹配）。
    """
    res = LinkResult(source_file, link, 'internal')
    # 锚点
    if link.startswith('#'):
        # 指向当前文件的锚点
        target_path = source_file
        anchor = link[1:]
    else:
        parts = link.split('#', 1)
        rel_path = parts[0] if parts[0] != '' else '.'
        anchor = parts[1] if len(parts) > 1 else None
        # 绝对路径（以 / 开头）相对于 repo_root，否则相对于 source_file 的目录
        if rel_path.startswith('/'):
            target_path = os.path.join(repo_root, rel_path.lstrip('/'))
        else:
            source_dir = os.path.dirname(source_file)
            target_path = os.path.normpath(os.path.join(source_dir, rel_path))

    # 如果链接指向目录，尝试补充 index.md 或 README.md
    if os.path.isdir(target_path):
        for candidate in ('index.md', 'README.md', 'index.html'):
            p = os.path.join(target_path, candidate)
            if os.path.exists(p):
                target_path = p
                break

    if not os.path.exists(target_path):
        res.ok = False
        res.detail = f'file not found: {target_path}'
        return res

    if anchor and check_anchor:
        # 尝试在目标文件的标题中查找匹配锚点（仅对 Markdown 标题）
        try:
            with open(target_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
            headings = extract_markdown_headings(content)
            generated = {github_anchor_from_heading(h) for h in headings}
            # 比较锚点（anchor 可能已经是 URL encoded 或者小写）
            anchor_norm = anchor.strip().lower()
            anchor_norm = re.sub(r'%20', '-', anchor_norm)  # decode common encoding for spaces
            if anchor_norm not in generated:
                res.ok = False
                res.detail = f'anchor not found: #{anchor} in {target_path} (found {len(generated)} headings)'
        except Exception as e:
            # 读取/解析失败时不阻塞，给出警告
            res.ok = False
            res.detail = f'failed to read/parse for anchor check: {e}'
    else:
        res.ok = True
    return res

# 缓存外部链接测试结果，避免重复请求
_external_cache_lock = threading.Lock()
_external_cache: Dict[str, Tuple[bool, str]] = {}

def check_external_link_head(url: str, timeout: float) -> Tuple[bool, str]:
    """
    使用 HTTP HEAD 优先测试。若 HEAD 返回 405/501 等不允许的状态，则回退到 GET (stream)。
    返回 (ok, detail)
    """
    headers = {
        "User-Agent": USER_AGENT,
    }
    try:
        r = requests.head(url, allow_redirects=True, timeout=timeout, headers=headers)
        status = r.status_code
        if 200 <= status < 400:
            return True, f'HTTP {status}'
        if status in (405, 501):  # method not allowed / not implemented
            # fallback
            r = requests.get(url, allow_redirects=True, timeout=timeout, headers=headers, stream=True)
            status = r.status_code
            if 200 <= status < 400:
                r.close()
                return True, f'HTTP {status} (GET fallback)'
            else:
                r.close()
                return False, f'HTTP {status} (GET fallback)'
        else:
            return False, f'HTTP {status}'
    except requests.exceptions.SSLError as e:
        return False, f'SSL error: {e}'
    except requests.exceptions.Timeout:
        return False, 'timeout'
    except requests.exceptions.ConnectionError as e:
        return False, f'connection error: {e}'
    except Exception as e:
        return False, f'error: {e}'

def check_external_link_cached(url: str, timeout: float) -> Tuple[bool, str]:
    with _external_cache_lock:
        if url in _external_cache:
            return _external_cache[url]
    ok, detail = check_external_link_head(url, timeout)
    with _external_cache_lock:
        _external_cache[url] = (ok, detail)
    return ok, detail

def gather_doc_files(root: str, paths: List[str], exts: Set[str]) -> List[str]:
    found = []
    if not paths:
        paths = [root]
    for p in paths:
        full = os.path.join(root, p) if not os.path.isabs(p) and root else p
        if os.path.isdir(full):
            for dirpath, dirnames, filenames in os.walk(full):
                for fn in filenames:
                    if os.path.splitext(fn)[1].lower().lstrip('.') in exts:
                        found.append(os.path.join(dirpath, fn))
        elif os.path.isfile(full):
            if os.path.splitext(full)[1].lower().lstrip('.') in exts:
                found.append(os.path.normpath(full))
        else:
            # allow patterns? 目前忽略不存在路径
            pass
    return sorted(set(found))

# ---------- 主流程 ----------
def main(argv=None):
    parser = argparse.ArgumentParser(description="检查文档链接（内部路径 & 外部可用性）")
    parser.add_argument('--root', '-r', default='.', help="仓库/项目根目录（用于解析以 '/' 开头的内部链接）")
    parser.add_argument('--paths', '-p', nargs='*', default=['docs', '.'],
                        help="要扫描的文件或目录（相对于 root），可多次指定；默认 ['docs', '.']，只检查指定扩展名")
    parser.add_argument('--ext', default='md,markdown,html,rst',
                        help="要扫描的文件扩展名，用逗号分隔（不含点），默认 md,markdown,html,rst")
    parser.add_argument('--concurrency', '-c', type=int, default=DEFAULT_CONCURRENCY,
                        help=f"外部链接并发检查数量（默认 {DEFAULT_CONCURRENCY}）")
    parser.add_argument('--timeout', '-t', type=float, default=DEFAULT_TIMEOUT,
                        help=f"HTTP 请求超时时间（秒），默认 {DEFAULT_TIMEOUT}")
    parser.add_argument('--no-anchor-check', dest='anchor_check', action='store_false',
                        help="禁用对内部链接锚点的检查（更快）")
    parser.add_argument('--verbose', '-v', action='store_true')
    args = parser.parse_args(argv)

    logging.basicConfig(level=logging.DEBUG if args.verbose else logging.INFO,
                        format='[%(levelname)s] %(message)s')

    root = os.path.abspath(args.root)
    exts = {e.strip().lower() for e in args.ext.split(',') if e.strip()}
    files = gather_doc_files(root, args.paths, exts)
    if not files:
        logging.error("未找到任何文档文件（检查 root/paths/ext 设置）")
        return 2

    logging.info(f"扫描 {len(files)} 个文件（并提取链接）...")
    # 链接集合： source -> [links]
    per_file_links: Dict[str, List[str]] = {}
    for f in files:
        try:
            links = extract_links_from_file(f)
            per_file_links[f] = links
        except Exception as e:
            logging.warning(f"读取或解析文件失败: {f} : {e}")
            per_file_links[f] = []

    # 分类链接，准备检查
    internal_tasks = []
    external_urls: Set[str] = set()
    ignored = []

    for src, links in per_file_links.items():
        for l in links:
            l = l.strip()
            if not l:
                continue
            # 排除 mailto/tel 等 scheme
            if is_ignored_scheme(l):
                ignored.append((src, l))
                continue
            if is_external_link(l):
                external_urls.add(l)
            else:
                internal_tasks.append((src, l))

    logging.info(f"发现链接：internal={len(internal_tasks)} external_unique={len(external_urls)} ignored={len(ignored)}")

    # 1) 检查内部链接（同步即可）
    internal_results: List[LinkResult] = []
    for src, l in internal_tasks:
        r = check_internal_link(src, l, root, check_anchor=args.anchor_check)
        internal_results.append(r)

    # 2) 并发检查外部链接
    external_results: List[LinkResult] = []
    if external_urls:
        logging.info(f"并发检查外部链接（并发={args.concurrency} timeout={args.timeout}s）...")
        with ThreadPoolExecutor(max_workers=args.concurrency) as ex:
            future_map = {ex.submit(check_external_link_cached, url, args.timeout): url for url in external_urls}
            for fut in as_completed(future_map):
                url = future_map[fut]
                ok, detail = fut.result()
                # 为了输出来源，我们需要映射回所有 source 文件中引用该 url 的位置
                for src, links in per_file_links.items():
                    if url in links:
                        lr = LinkResult(src, url, 'external')
                        lr.ok = ok
                        lr.detail = detail
                        external_results.append(lr)

    # collect ignored as LinkResult
    ignored_results = [LinkResult(src, l, 'ignored') for src, l in ignored]
    for ig in ignored_results:
        ig.ok = True
        ig.detail = 'ignored scheme'

    # 合并并汇总
    all_results = internal_results + external_results + ignored_results
    broken = [r for r in all_results if not r.ok]

    # 输出结果
    if broken:
        logging.error(f"检测到 {len(broken)} 个问题链接：")
        for r in broken:
            print(f"- {r.source_file} -> {r.link}  [{r.kind}]  : {r.detail}")
    else:
        logging.info("全部检测通过：未发现坏链。")

    # 简要统计
    total_links = sum(len(v) for v in per_file_links.values())
    print()
    print("Summary:")
    print(f"  files scanned: {len(files)}")
    print(f"  total links found: {total_links}")
    print(f"  internal checked: {len(internal_results)}")
    print(f"  external checked (unique): {len(external_urls)}")
    print(f"  ignored: {len(ignored)}")
    print(f"  broken: {len(broken)}")

    # 可选：将结果写入文件（JSON/CSV） - 留给用户自行扩展

    return 1 if broken else 0

if __name__ == '__main__':
    sys.exit(main())
