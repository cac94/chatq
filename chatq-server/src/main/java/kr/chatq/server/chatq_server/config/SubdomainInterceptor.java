package kr.chatq.server.chatq_server.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

public class SubdomainInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SubdomainInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String company = "chatq"; // Default

        // 1. Try to get from Header (Priority for testing/override)
        String headerCompany = request.getHeader("X-Target-Company");
        if (headerCompany != null && !headerCompany.isEmpty()) {
            company = headerCompany;
        } else {
            // 2. Try to get from Subdomain
            String serverName = request.getServerName();
            if (serverName != null) {
                // e.g., "mycompany.chatq.click" -> "mycompany"
                // e.g., "localhost" -> "localhost" (which might default to chatq later if no
                // match)
                String[] parts = serverName.split("\\.");
                if (parts.length > 2) {
                    // Assuming structure like {subdomain}.domain.com
                    company = parts[0];
                } else if (parts.length > 0 && !"localhost".equals(serverName)) {
                    // Just in case of simple names locally or other structures
                    // But usually, localhost is ignored or handled separately
                    // If it's something like "chatq.click" (2 parts), maybe no subdomain?
                }
            }
        }

        if ("www".equalsIgnoreCase(company)) {
            company = "chatq";
        }

        CompanyContext.setCompany(company);
        logger.debug("Set CompanyContext to: {}", company);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        CompanyContext.clear();
    }
}
