package com.tigtech.persfinance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Removes Authorization header from requests to /api/auth/* so that the resource-server
 * bearer filter doesn't reject requests to authentication endpoints when a stale/invalid
 * Authorization header is present (e.g., from Postman).
 */
public class RemoveAuthorizationHeaderFilter extends OncePerRequestFilter {

    @Override
    @SuppressWarnings("deprecation")
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api/auth")) {
            HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("authorization".equalsIgnoreCase(name)) return null;
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("authorization".equalsIgnoreCase(name)) {
                        return Collections.enumeration(Collections.emptyList());
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    List<String> names = Collections.list(super.getHeaderNames()).stream()
                            .filter(n -> !"authorization".equalsIgnoreCase(n))
                            .collect(Collectors.toList());
                    return Collections.enumeration(names);
                }
            };
            filterChain.doFilter(wrapped, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
