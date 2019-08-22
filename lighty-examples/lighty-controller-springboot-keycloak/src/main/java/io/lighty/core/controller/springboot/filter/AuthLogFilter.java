package io.lighty.core.controller.springboot.filter;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@Component
public class AuthLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

                if (!(request instanceof HttpServletRequest)) {
                        throw new RuntimeException("Expecting a HTTP request");
                }

                RefreshableKeycloakSecurityContext context = (RefreshableKeycloakSecurityContext) request
                                .getAttribute(KeycloakSecurityContext.class.getName());


                String username = "n/a";
                if (context != null)
                {
                        AccessToken accessToken = context.getToken();
                        username = (String) accessToken.getPreferredUsername();
                        if (username == null || username.trim().isEmpty()) {
                                username = "n/a";
			}
                }

                MDC.put("auth", String.format("[%s]", username));
                chain.doFilter(request, response);
    }
}

