package ltd.newbee.mall.config.security;

import java.util.regex.Pattern;

/**
 * SQL 注入检测工具。
 * <p>
 * 项目持久层基于 MyBatis 的 {@code #{}} 预编译占位符，本身可有效防止 SQL 注入；
 * 本工具作为请求入口处的额外防线，仅匹配“明显的”注入特征（联合查询、恒真条件、
 * 堆叠的危险语句、延时盲注、注释绕过等），尽量避免对正常业务参数产生误杀。
 *
 * @author newbee-mall release-config
 */
public final class SqlInjectionUtil {

    private SqlInjectionUtil() {
    }

    /**
     * 明显的 SQL 注入特征。所有匹配均忽略大小写，且要求组合特征以降低误报。
     */
    private static final Pattern[] INJECTION_PATTERNS = new Pattern[]{
            // union [all] select
            Pattern.compile("\\bunion\\b(\\s+\\ball\\b)?\\s+\\bselect\\b", Pattern.CASE_INSENSITIVE),
            // or/and 恒真/恒假条件，如 or 1=1 、 and '1'='1' （兼容操作数被引号包裹的情形）
            Pattern.compile("\\b(or|and)\\b\\s+['\"]?\\s*\\d+\\s*['\"]?\\s*=\\s*['\"]?\\s*\\d+", Pattern.CASE_INSENSITIVE),
            // 堆叠执行危险语句，如 ; drop table 、 ; delete from
            Pattern.compile(";\\s*(drop|truncate|alter|create)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile(";\\s*(delete\\s+from|insert\\s+into|update\\s+\\w+\\s+set)\\b", Pattern.CASE_INSENSITIVE),
            // 延时盲注
            Pattern.compile("\\b(sleep|benchmark|pg_sleep)\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwaitfor\\b\\s+\\bdelay\\b", Pattern.CASE_INSENSITIVE),
            // 内联注释绕过 /* ... */
            Pattern.compile("/\\*.*?\\*/", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            // 执行系统命令
            Pattern.compile("\\bxp_cmdshell\\b", Pattern.CASE_INSENSITIVE)
    };

    /**
     * 判断字符串是否包含明显的 SQL 注入特征。
     *
     * @param value 待检测内容（通常为请求参数值）
     * @return 命中任一注入特征返回 {@code true}
     */
    public static boolean isInjection(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }
}
