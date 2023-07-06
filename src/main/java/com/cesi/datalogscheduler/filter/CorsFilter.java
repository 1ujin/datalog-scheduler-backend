package com.cesi.datalogscheduler.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CorsFilter implements Filter {
    @Value("${cors-filter.access-control-allow-origin}")
    private String accessControlAllowOrigin;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws ServletException, IOException {
        HttpServletResponse hsp = (HttpServletResponse) res;
        if (accessControlAllowOrigin != null) {
            hsp.setHeader("Access-Control-Allow-Origin", accessControlAllowOrigin);
        }
        hsp.setHeader("Access-Control-Allow-Headers", "*");
        hsp.setHeader("Access-Control-Allow-Methods", "*");
        hsp.setHeader("Access-Control-Allow-Credentials", "true");
        chain.doFilter(req, res);
    }
}
