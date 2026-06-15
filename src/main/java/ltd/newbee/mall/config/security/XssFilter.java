package ltd.newbee.mall.config.security;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 安全过滤器：在请求进入业务逻辑前，
 * <ol>
 *     <li>对明显的 SQL 注入特征进行拦截（返回 400）；</li>
 *     <li>对请求参数进行 XSS 转义（通过 {@link XssHttpServletRequestWrapper}）。</li>
 * </ol>
 * 是否启用、排除路径均由配置项 {@code newbee.security.*} 控制（见 application-prod.yml）。
 *
 * @author newbee-mall release-config
 */
public class XssFilter implements Filter {

    private final boolean xssEnabled;
    private final boolean sqlInjectionEnabled;
    private final List<String> exclusions;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    public XssFilter(boolean xssEnabled, boolean sqlInjectionEnabled, List<String> exclusions) {
        this.xssEnabled = xssEnabled;
        this.sqlInjectionEnabled = sqlInjectionEnabled;
        this.exclusions = exclusions;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (isExcluded(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        if (sqlInjectionEnabled && containsSqlInjection(httpRequest)) {
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal request parameters");
            return;
        }

        if (xssEnabled) {
            chain.doFilter(new XssHttpServletRequestWrapper(httpRequest), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * 判断当前请求路径是否命中排除规则。
     */
    private boolean isExcluded(HttpServletRequest request) {
        if (exclusions == null || exclusions.isEmpty()) {
            return false;
        }
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        for (String pattern : exclusions) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 扫描所有请求参数值，命中明显注入特征即视为非法请求。
     */
    private boolean containsSqlInjection(HttpServletRequest request) {
        for (String[] values : request.getParameterMap().values()) {
            if (values == null) {
                continue;
            }
            for (String value : values) {
                if (SqlInjectionUtil.isInjection(value)) {
                    return true;
                }
            }
        }
        return false;
    }
}
