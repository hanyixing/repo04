package ltd.newbee.mall.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SqlInjectionUtil} 单元测试（纯 JUnit，无需 Spring 容器与数据库）。
 * 既验证常见注入特征可被识别，也验证正常业务参数不会被误杀。
 *
 * @author newbee-mall release-config
 */
class SqlInjectionUtilTest {

    @Test
    @DisplayName("可识别明显的 SQL 注入特征")
    void shouldDetectObviousInjection() {
        assertTrue(SqlInjectionUtil.isInjection("1' or 1=1 -- "));
        assertTrue(SqlInjectionUtil.isInjection("admin' AND '1'='1"));
        assertTrue(SqlInjectionUtil.isInjection("1 UNION SELECT username, password FROM tb_newbee_mall_user"));
        assertTrue(SqlInjectionUtil.isInjection("1; DROP TABLE tb_newbee_mall_user"));
        assertTrue(SqlInjectionUtil.isInjection("1); delete from tb_newbee_mall_order"));
        assertTrue(SqlInjectionUtil.isInjection("1 AND sleep(5)"));
        assertTrue(SqlInjectionUtil.isInjection("name=1/* comment */"));
    }

    @Test
    @DisplayName("正常业务参数不应被误判为注入")
    void shouldNotFlagLegitimateInput() {
        assertFalse(SqlInjectionUtil.isInjection(null));
        assertFalse(SqlInjectionUtil.isInjection(""));
        assertFalse(SqlInjectionUtil.isInjection("newbee mall"));
        assertFalse(SqlInjectionUtil.isInjection("华为 Mate 60 Pro"));
        assertFalse(SqlInjectionUtil.isInjection("12345"));
        assertFalse(SqlInjectionUtil.isInjection("orderNo=202306150001"));
        assertFalse(SqlInjectionUtil.isInjection("price>100 and price<200")); // 业务文案，非数字恒等式
    }
}
