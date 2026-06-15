package ltd.newbee.mall.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Web 安全配置：注册 XSS / SQL 注入防护过滤器，并为响应统一附加安全相关 HTTP 头。
 * <p>
 * 开关与排除路径由 {@code newbee.security.*} 配置项控制，默认全部开启，
 * 因此各环境（dev/test/prod）均具备基础防护，生产环境可结合加密配置进一步收紧。
 *
 * @author newbee-mall release-config
 */
@Configuration
public class WebSecurityConfig {

    @Value("${newbee.security.xss.enabled:true}")
    private boolean xssEnabled;

    @Value("${newbee.security.sql-injection.enabled:true}")
    private boolean sqlInjectionEnabled;

    /**
     * XSS 过滤排除路径（逗号分隔），默认放行后台静态资源与文件上传相关接口。
     */
    @Value("${newbee.security.xss.exclusions:/admin/dist/**,/admin/plugins/**,/upload/**,/goods-img/**}")
    private String xssExclusions;

    /**
     * 注册输入侧安全过滤器（XSS 转义 + SQL 注入拦截）。
     */
    @Bean
    public FilterRegistrationBean<XssFilter> xssFilterRegistration() {
        List<String> exclusions = new ArrayList<>();
        if (xssExclusions != null && !xssExclusions.trim().isEmpty()) {
            for (String item : xssExclusions.split(",")) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    exclusions.add(trimmed);
                }
            }
        }
        FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new XssFilter(xssEnabled, sqlInjectionEnabled, exclusions));
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    /**
     * 注册响应安全头过滤器，缓解点击劫持、MIME 嗅探、反射型 XSS 等风险。
     */
    @Bean
    public FilterRegistrationBean<Filter> securityHeadersFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter((request, response, chain) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            httpResponse.setHeader("Referrer-Policy", "no-referrer-when-downgrade");
            chain.doFilter(request, response);
        });
        registration.addUrlPatterns("/*");
        registration.setName("securityHeadersFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }
}
