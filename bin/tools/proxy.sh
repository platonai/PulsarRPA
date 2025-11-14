#!/bin/bash

# A tool to set proxy environment.
# Defaults (override with flags or env PROXY_* vars)
# Typically local clients expose HTTP proxy at 7890 and SOCKS5 at 7890/7891.
# We'll set:
#   - all_proxy (socks5)
#   - http_proxy / https_proxy (http)
#   - no_proxy
# Applies to: current shell session, git, VS Code (if installed)


# The proxy settings will be applied to:
# 1. current shell session
# 2. git commands
# 3. vscode if installed

set -euo pipefail

SCRIPT_NAME=$(basename "$0")

# Defaults can be overridden by environment variables
PROXY_HOST=${PROXY_HOST:-127.0.0.1}
HTTP_PORT=${HTTP_PORT:-7890}
SOCKS_PORT=${SOCKS_PORT:-7890}
NO_PROXY_LIST=${NO_PROXY_LIST:-localhost,127.0.0.1,127.0.0.0/8,::1,localaddress,.localdomain.com}

VS_CODE_SETTINGS_CANDIDATES=(
	"$HOME/.config/Code/User/settings.json"
	"$HOME/.config/Code - OSS/User/settings.json"
	"$HOME/.config/Code - Insiders/User/settings.json"
	"$HOME/.config/VSCodium/User/settings.json"
)

have_cmd() { command -v "$1" >/dev/null 2>&1; }

warn_not_sourced() {
	# If the script is not sourced, env vars won't persist in the caller's shell
	# Detect common case: when BASH_SOURCE[0] == $0, it's executed, not sourced
	if [[ "${BASH_SOURCE[0]-$0}" == "$0" ]]; then
		echo "[info] To persist environment variables in current shell, source this script:" >&2
		echo "       source bin/tools/proxy.sh on" >&2
	fi
}

usage() {
	cat <<EOF
Usage: source $SCRIPT_NAME on [--host HOST] [--http-port N] [--socks-port N] [--no-proxy LIST]
			 $SCRIPT_NAME off
			 $SCRIPT_NAME status

Options (for 'on'):
	--host HOST         Proxy host (default: $PROXY_HOST)
	--http-port N       HTTP proxy port (default: $HTTP_PORT)
	--socks-port N      SOCKS5 proxy port (default: $SOCKS_PORT)
	--no-proxy LIST     Comma-separated no_proxy list (default: $NO_PROXY_LIST)

Notes:
	- To apply env vars to your current shell, you must source this script: '. bin/tools/proxy.sh on'
	- This script also configures 'git config --global http.proxy/https.proxy'.
	- VS Code user settings will be updated if a settings.json is found.
EOF
}

parse_on_args() {
	# allows overriding defaults via flags
	while [[ $# -gt 0 ]]; do
		case "$1" in
			--host)
				PROXY_HOST="$2"; shift 2 ;;
			--http-port)
				HTTP_PORT="$2"; shift 2 ;;
			--socks-port)
				SOCKS_PORT="$2"; shift 2 ;;
			--no-proxy)
				NO_PROXY_LIST="$2"; shift 2 ;;
			-h|--help)
				usage; exit 0 ;;
			*)
				echo "Unknown option: $1" >&2; usage; exit 1 ;;
		esac
	done
}

export_env_proxies() {
	local host="$1" http_port="$2" socks_port="$3" no_proxy_list="$4"

	# lowercase
	export http_proxy="http://$host:$http_port"
	export https_proxy="http://$host:$http_port"
	export all_proxy="socks5://$host:$socks_port"
	export no_proxy="$no_proxy_list"

	# uppercase (some tools only read uppercase)
	export HTTP_PROXY="$http_proxy"
	export HTTPS_PROXY="$https_proxy"
	export ALL_PROXY="$all_proxy"
	export NO_PROXY="$no_proxy_list"
}

unset_env_proxies() {
	unset http_proxy https_proxy all_proxy no_proxy
	unset HTTP_PROXY HTTPS_PROXY ALL_PROXY NO_PROXY
}

set_git_proxies() {
	local host="$1" http_port="$2" socks_port="$3"
	# Prefer HTTP proxy for git, but allow SOCKS5 via all_proxy if desired.
	# Git supports socks5 scheme in http[s].proxy values too; we set HTTP here by default.
	git config --global http.proxy "http://$host:$http_port" || true
	git config --global https.proxy "http://$host:$http_port" || true
}

unset_git_proxies() {
	git config --global --unset http.proxy 2>/dev/null || true
	git config --global --unset https.proxy 2>/dev/null || true
}

ensure_file() {
	local f="$1"; mkdir -p "$(dirname "$f")"; [[ -f "$f" ]] || echo '{}' > "$f"
}

set_vscode_proxies_file() {
	local file="$1" host="$2" http_port="$3"
	ensure_file "$file"
	if have_cmd jq; then
		tmp="${file}.tmp.$$"
		jq \
			--arg proxy "http://$host:$http_port" \
			'. + {"http.proxy": $proxy, "http.proxyStrictSSL": false, "http.proxySupport": "on"}' \
			"$file" > "$tmp" && mv "$tmp" "$file"
	elif have_cmd python3; then
		python3 - "$file" "$host" "$http_port" <<'PY'
import json, sys
path, host, port = sys.argv[1:4]
try:
		with open(path, 'r', encoding='utf-8') as f:
				data = json.load(f)
except Exception:
		data = {}
data['http.proxy'] = f'http://{host}:{port}'
data['http.proxyStrictSSL'] = False
data['http.proxySupport'] = 'on'
with open(path, 'w', encoding='utf-8') as f:
		json.dump(data, f, indent=2, ensure_ascii=False)
PY
	else
		echo "[warn] Neither jq nor python3 found; cannot auto-update VS Code settings at $file" >&2
	fi
}

unset_vscode_proxies_file() {
	local file="$1"
	[[ -f "$file" ]] || return 0
	if have_cmd jq; then
		tmp="${file}.tmp.$$"
		jq 'del(."http.proxy", ."http.proxyStrictSSL", ."http.proxySupport")' "$file" > "$tmp" && mv "$tmp" "$file"
	elif have_cmd python3; then
		python3 - "$file" <<'PY'
import json, sys
path = sys.argv[1]
try:
		with open(path, 'r', encoding='utf-8') as f:
				data = json.load(f)
except Exception:
		data = {}
for k in ['http.proxy', 'http.proxyStrictSSL', 'http.proxySupport']:
		if k in data:
				del data[k]
with open(path, 'w', encoding='utf-8') as f:
		json.dump(data, f, indent=2, ensure_ascii=False)
PY
	else
		echo "[warn] Neither jq nor python3 found; cannot remove VS Code proxy settings at $file" >&2
	fi
}

set_vscode_proxies() {
	local host="$1" http_port="$2"
	local updated=0
	for f in "${VS_CODE_SETTINGS_CANDIDATES[@]}"; do
		if [[ -f "$f" ]] || [[ -d "$(dirname "$f")" ]]; then
			set_vscode_proxies_file "$f" "$host" "$http_port"
			updated=1
		fi
	done
	if [[ "$updated" -eq 1 ]]; then
		echo "[ok] VS Code proxy settings updated"
	else
		echo "[info] VS Code settings.json not found; skipping"
	fi
}

unset_vscode_proxies() {
	local updated=0
	for f in "${VS_CODE_SETTINGS_CANDIDATES[@]}"; do
		if [[ -f "$f" ]]; then
			unset_vscode_proxies_file "$f"
			updated=1
		fi
	done
	if [[ "$updated" -eq 1 ]]; then
		echo "[ok] VS Code proxy settings removed"
	else
		echo "[info] VS Code settings.json not found; nothing to remove"
	fi
}

status() {
	echo "== Environment (current shell) =="
	env | grep -E '^(http_proxy|https_proxy|all_proxy|no_proxy|HTTP_PROXY|HTTPS_PROXY|ALL_PROXY|NO_PROXY)=' || echo "(none)"
	echo
	echo "== Git (global config) =="
	git config --global --get http.proxy 2>/dev/null || echo "http.proxy: (unset)"
	git config --global --get https.proxy 2>/dev/null || echo "https.proxy: (unset)"
	echo
	echo "== VS Code settings (if present) =="
	local any=0
	for f in "${VS_CODE_SETTINGS_CANDIDATES[@]}"; do
		if [[ -f "$f" ]]; then
			any=1
			echo "-- $f --"
			if have_cmd jq; then
				jq -r '."http.proxy" // "http.proxy: (unset)"' "$f" | sed 's/^/  /'
			elif have_cmd python3; then
				python3 - "$f" <<'PY'
import json, sys
path=sys.argv[1]
try:
	with open(path,'r',encoding='utf-8') as f: d=json.load(f)
	print('  ' + str(d.get('http.proxy','http.proxy: (unset)')))
except Exception as e:
	print('  (error reading)')
PY
			else
				echo "  (install jq or python3 to view)"
			fi
		fi
	done
	[[ "$any" -eq 1 ]] || echo "(no settings.json found)"
}

cmd=${1:-}
case "$cmd" in
	on)
		shift || true
		parse_on_args "$@"
		export_env_proxies "$PROXY_HOST" "$HTTP_PORT" "$SOCKS_PORT" "$NO_PROXY_LIST"
		set_git_proxies "$PROXY_HOST" "$HTTP_PORT" "$SOCKS_PORT"
		set_vscode_proxies "$PROXY_HOST" "$HTTP_PORT"
		echo "[ok] Proxy ON"
		echo "       http(s): http://$PROXY_HOST:$HTTP_PORT"
		echo "       socks5 : socks5://$PROXY_HOST:$SOCKS_PORT"
		warn_not_sourced
		;;
	off)
		unset_env_proxies
		unset_git_proxies
		unset_vscode_proxies
		echo "[ok] Proxy OFF"
		warn_not_sourced
		;;
	status)
		status
		;;
	-h|--help|"")
		usage
		;;
	*)
		echo "Unknown command: $cmd" >&2
		usage
		exit 1
		;;
esac

