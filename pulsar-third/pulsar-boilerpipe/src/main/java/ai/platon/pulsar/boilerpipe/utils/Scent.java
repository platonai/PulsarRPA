package ai.platon.pulsar.boilerpipe.utils;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import ai.platon.pulsar.boilerpipe.document.BlockLabels;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by vincent on 16-10-27.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class Scent {
    public static final int MIN_DATE_TIME_STR_LENGTH = 15;
    public static final int MAX_META_STR_LENGTH = 200;

    public static final String DOC_FIELD_PUBLISH_TIME = "publish_time";
    public static final String DOC_FIELD_MODIFIED_TIME = "modified_time";
    public static final String DOC_FIELD_ARTICLE_TILE = "article_title";
    public static final String DOC_FIELD_PAGE_TITLE = "page_title";
    public static final String DOC_FIELD_CONTENT_TITLE = "article_title";
    public static final String DOC_FIELD_PAGE_CATEGORY = "page_category";
    public static final String DOC_FIELD_LINKS_COUNT = "links_count";
    public static final String DOC_FIELD_HTML_CONTENT = "html_content";
    public static final String DOC_FIELD_TEXT_CONTENT = "text_content";
    public static final String DOC_FIELD_TEXT_CONTENT_LENGTH = "text_content_length";
    public static final String DOC_FIELD_HTML_CONTENT_LENGTH = "html_content_length";

    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows; U; Win 9x 4.90; SG; rv:1.9.2.4) Gecko/20101104 Netscape/9.1.0285";

    public static final Pattern[] PATTERNS_SHORT =
            new Pattern[]{
                    Pattern.compile("^[0-9 \\,\\./]*\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)?\\b[0-9 \\,\\:apm\\./]*([CPSDMGET]{2,3})?$"),
                    Pattern.compile("^[Bb]y "),
            };

    public static final char[] CHINESE_FAMILY_NAME_CHARS = new char[]{
            '赵', '钱', '孙', '李', '周', '吴', '郑', '王', '冯', '陈', '楮', '卫', '蒋', '沈', '韩', '杨',
            '朱', '秦', '尤', '许', '何', '吕', '施', '张', '孔', '曹', '严', '华', '金', '魏', '陶', '姜',
            '戚', '谢', '邹', '喻', '柏', '水', '窦', '章', '云', '苏', '潘', '葛', '奚', '范', '彭', '郎',
            '鲁', '韦', '昌', '马', '苗', '凤', '花', '方', '俞', '任', '袁', '柳', '酆', '鲍', '史', '唐',
            '费', '廉', '岑', '薛', '雷', '贺', '倪', '汤', '滕', '殷', '罗', '毕', '郝', '邬', '安', '常',
            '乐', '于', '时', '傅', '皮', '卞', '齐', '康', '伍', '余', '元', '卜', '顾', '孟', '平', '黄',
            '和', '穆', '萧', '尹', '姚', '邵', '湛', '汪', '祁', '毛', '禹', '狄', '米', '贝', '明', '臧',
            '计', '伏', '成', '戴', '谈', '宋', '茅', '庞', '熊', '纪', '舒', '屈', '项', '祝', '董', '梁',
            '杜', '阮', '蓝', '闽', '席', '季', '麻', '强', '贾', '路', '娄', '危', '江', '童', '颜', '郭',
            '梅', '盛', '林', '刁', '锺', '徐', '丘', '骆', '高', '夏', '蔡', '田', '樊', '胡', '凌', '霍',
            '虞', '万', '支', '柯', '昝', '管', '卢', '莫', '经', '房', '裘', '缪', '干', '解', '应', '宗',
            '丁', '宣', '贲', '邓', '郁', '单', '杭', '洪', '包', '诸', '左', '石', '崔', '吉', '钮', '龚',
            '程', '嵇', '邢', '滑', '裴', '陆', '荣', '翁', '荀', '羊', '於', '惠', '甄', '麹', '家', '封',
            '芮', '羿', '储', '靳', '汲', '邴', '糜', '松', '井', '段', '富', '巫', '乌', '焦', '巴', '弓',
            '牧', '隗', '山', '谷', '车', '侯', '宓', '蓬', '全', '郗', '班', '仰', '秋', '仲', '伊', '宫',
            '宁', '仇', '栾', '暴', '甘', '斜', '厉', '戎', '祖', '武', '符', '刘', '景', '詹', '束', '龙',
            '叶', '幸', '司', '韶', '郜', '黎', '蓟', '薄', '印', '宿', '白', '怀', '蒲', '邰', '从', '鄂',
            '索', '咸', '籍', '赖', '卓', '蔺', '屠', '蒙', '池', '乔', '阴', '郁', '胥', '能', '苍', '双',
            '闻', '莘', '党', '翟', '谭', '贡', '劳', '逄', '姬', '申', '扶', '堵', '冉', '宰', '郦', '雍',
            '郤', '璩', '桑', '桂', '濮', '牛', '寿', '通', '边', '扈', '燕', '冀', '郏', '浦', '尚', '农',
            '温', '别', '庄', '晏', '柴', '瞿', '阎', '充', '慕', '连', '茹', '习', '宦', '艾', '鱼', '容',
            '向', '古', '易', '慎', '戈', '廖', '庾', '终', '暨', '居', '衡', '步', '都', '耿', '满', '弘',
            '匡', '国', '文', '寇', '广', '禄', '阙', '东', '欧', '殳', '沃', '利', '蔚', '越', '夔', '隆',
            '师', '巩', '厍', '聂', '晁', '勾', '敖', '融', '冷', '訾', '辛', '阚', '那', '简', '饶', '空',
            '曾', '毋', '沙', '乜', '养', '鞠', '须', '丰', '巢', '关', '蒯', '相', '查', '后', '荆', '红',
            '游', '竺', '权', '逑', '盖', '益', '桓', '公', '仉', '督', '晋', '楚', '阎', '法', '汝', '鄢',
            '涂', '钦', '岳', '帅', '缑', '亢', '况', '后', '有', '琴', '归', '海', '墨', '哈', '谯', '笪',
            '年', '爱', '阳', '佟', '商', '牟', '佘', '佴', '伯', '赏'
    };

    public static final CharSequence[] CHINESE_S_FAMILY_NAMES = Stream.of(CHINESE_FAMILY_NAME_CHARS)
            .map(String::valueOf).collect(Collectors.toList()).toArray(new CharSequence[0]);

    public static final CharSequence[] CHINESE_D_FAMILY_NAMES = new String[]{
            "万俟", "司马", "上官", "欧阳", "夏侯", "诸葛", "闻人", "东方", "赫连", "皇甫", "尉迟", "公羊",
            "澹台", "公冶", "宗政", "濮阳", "淳于", "单于", "太叔", "申屠", "公孙", "仲孙", "轩辕", "令狐",
            "锺离", "宇文", "长孙", "慕容", "鲜于", "闾丘", "司徒", "司空", "丌官", "司寇", "子车", "微生",
            "颛孙", "端木", "巫马", "公西", "漆雕", "乐正", "壤驷", "公良", "拓拔", "夹谷", "宰父", "谷梁",
            "段干", "百里", "东郭", "南门", "呼延", "羊舌", "梁丘", "左丘", "东门", "西门", "南宫"
    };

    public static final ListMultimap<String, String> LABELED_FIELD_RULES = LinkedListMultimap.create();
    public static final ListMultimap<String, String> REGEX_FIELD_RULES = LinkedListMultimap.create();
    public static final Set<String> TERMINATING_BLOCKS_CONTAINS = Sets.newTreeSet();
    public static final Set<String> TERMINATING_BLOCKS_STARTS_WITH = Sets.newTreeSet();
    public static final Map<String, Integer> MAX_FIELD_LENGTH_MAP = new TreeMap<>();

    public static final String[] REGEX_FIELD_BOUNDERS = {"作者", "责任编辑", "编辑", "来源"};

    public static final String[] BAD_PHRASE_IN_NAME = new String[]{
            "等", "介绍", "了解", "在", "采访", "发现", "独家", "获悉", "获知",
            "回忆", "关注", "提醒", "各位", "管窥", "到了", "昨天", "今天", "最近", "打开"
    };

    public static final String[] BAD_DATE_TIME_STRING_CONTAINS = new String[]{
            "GMT+8",
            "UTC+8",
            "Processed",
            "访问",
            "刷新",
            "visit"
    };

    public static final String[] BAD_PHRASE_IN_TITLE = new String[]{
            "主流媒体 山西门户",
    };

    static {
        LABELED_FIELD_RULES.put(DOC_FIELD_CONTENT_TITLE, BlockLabels.CONTENT_TITLE);

        // TODO : Regex explain : (作者|记者)\s{0,}：{0,1}\s{0,}([一-龥]{2,6})(作者|责任|编辑|来源){0,1}
        REGEX_FIELD_RULES.put("author", "(作者|记者)\\s{0,5}：{0,1}\\s{0,}([一-龥]{2,4})(作者|责编|责任编辑|编辑|来源){0,1}");
        REGEX_FIELD_RULES.put("director", "(编辑)\\s{0,5}：{0,1}\\s{0,}([一-龥]{2,4})(作者|责编|责任编辑|编辑|来源){0,1}");
        REGEX_FIELD_RULES.put("reference", "(来源|来源于|来自|转载|转载自)\\s{0,5}：{0,1}\\s{0,}([一-龥]{2,7})(作者|责编|责任编辑|编辑|来源){0,1}");

        REGEX_FIELD_RULES.put("keywords", "(关键词)\\s{0,5}：{0,1}\\s{0,}(.+)");
        REGEX_FIELD_RULES.put("keywords", "(标签)\\s{0,5}：{0,1}\\s{0,}(.+)");

        MAX_FIELD_LENGTH_MAP.put("author", 4);
        MAX_FIELD_LENGTH_MAP.put("director", 4);
        MAX_FIELD_LENGTH_MAP.put("reference", 100);

        TERMINATING_BLOCKS_CONTAINS.addAll(Lists.newArrayList(
                "All Rights Reserved",
                "Copyright ©",
                "what you think...",
                "add your comment",
                "add comment",
                "reader views",
                "have your say",
                "reader comments",
                "comments",
                "© reuters",
                "please rate this",
                "post a comment",
                "参与讨论",
                "精彩评论",
                "网友跟贴",
                "登录发帖",
                "版权所有",
                "京ICP证",
                "网友跟贴",
                "版权所有",
                "网络传播视听节目许可证号",
                "增值电信业务经营许可证",
                "相关阅读",
                "免责声明",
                "互联网违法和不良信息举报方式",
                "转载请注明",
                "技术支持热线",
                "站长统计"
        ));

        TERMINATING_BLOCKS_STARTS_WITH.addAll(Lists.newArrayList(
        ));
    }
}
