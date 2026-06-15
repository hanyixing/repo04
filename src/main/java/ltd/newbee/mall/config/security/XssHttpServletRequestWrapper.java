package ltd.newbee.mall.config.security;

import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * XSS 防护请求包装器：对请求参数（及部分请求头）进行 HTML 转义，
 * 使得反射型脚本注入（如 {@code <script>}、事件属性等）在回显时失去执行能力。
 *
 * @author newbee-mall release-config
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        return clean(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] cleaned = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleaned[i] = clean(values[i]);
        }
        return cleaned;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> original = super.getParameterMap();
        Map<String, String[]> cleaned = new LinkedHashMap<>(original.size());
        for (Map.Entry<String, String[]> entry : original.entrySet()) {
            String[] values = entry.getValue();
            String[] cleanedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanedValues[i] = clean(values[i]);
            }
            cleaned.put(entry.getKey(), cleanedValues);
        }
        return cleaned;
    }

    @Override
    public String getHeader(String name) {
        return clean(super.getHeader(name));
    }

    /**
     * 对单个值进行 HTML 转义；空值原样返回。
     */
    private String clean(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return HtmlUtils.htmlEscape(value);
    }
}
