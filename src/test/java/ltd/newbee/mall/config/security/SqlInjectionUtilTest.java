package ltd.newbee.mall.config.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SqlInjectionUtil} 单元测试。
 *
 * @author newbee-mall release-config
 */
class SqlInjectionUtilTest {

    // ========== 正常参数不误杀 ==========

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "hello world",
            "newbee-mall",
            "user@example.com",
            "13800138000",
            "2024-01-01",
            "select a product",
            "union street",
            "or maybe not",
            "drop by anytime"
    })
    void normalInput_shouldNotBeFlagged(String input) {
        assertFalse(SqlInjectionUtil.isInjection(input));
    }

    @Test
    void nullInput_shouldNotBeFlagged() {
        assertFalse(SqlInjectionUtil.isInjection(null));
    }

    @Test
    void emptyInput_shouldNotBeFlagged() {
        assertFalse(SqlInjectionUtil.isInjection(""));
    }

    // ========== union select 注入 ==========

    @ParameterizedTest
    @ValueSource(strings = {
            "1 union select username,password from users",
            "UNION SELECT * FROM information_schema.tables",
            "1 UNION ALL SELECT null,null--",
            "union  select 1"
    })
    void unionSelect_shouldBeFlagged(String input) {
        assertTrue(SqlInjectionUtil.isInjection(input));
    }

    // ========== 恒真/恒假条件 ==========

    @ParameterizedTest
    @ValueSource(strings = {
            "1 or 1=1",
            "1 or '1'='1",
            "admin' and 1=1--",
            "' or 0=0"
    })
    void tautologyCondition_shouldBeFlagged(String input) {
        assertTrue(SqlInjectionUtil.isInjection(input));
    }

    // ========== 堆叠危险语句 ==========

    @ParameterizedTest
    @ValueSource(strings = {
            "; drop table users",
            "; truncate table orders",
            "; alter table users add column hack",
            "; delete from users where 1=1",
            "; insert into admin values('hacker','pwd')",
            "; update users set role='admin' where id=1"
    })
    void stackedDangerousStatements_shouldBeFlagged(String input) {
        assertTrue(SqlInjectionUtil.isInjection(input));
    }

    // ========== 延时盲注 ==========

    @ParameterizedTest
    @ValueSource(strings = {
            "1; sleep(5)",
            "benchmark(10000000,sha1('test'))",
            "pg_sleep(10)",
            "waitfor delay '0:0:5'"
    })
    void timeBasedBlind_shouldBeFlagged(String input) {
        assertTrue(SqlInjectionUtil.isInjection(input));
    }

    // ========== 内联注释绕过 ==========

    @ParameterizedTest
    @ValueSource(strings = {
            "1 /*! UNION */ select",
            "/**/ union select",
            "sel/* comment */ect"
    })
    void inlineComment_shouldBeFlagged(String input) {
        assertTrue(SqlInjectionUtil.isInjection(input));
    }

    // ========== 系统命令执行 ==========

    @Test
    void xpCmdshell_shouldBeFlagged() {
        assertTrue(SqlInjectionUtil.isInjection("exec xp_cmdshell('dir')"));
    }
}
