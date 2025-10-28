/*
 * Copyright (c) 2018-2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.config;

import io.lighty.core.controller.springboot.services.UserAccessService;
import io.lighty.core.controller.springboot.services.dto.UserData;
import org.casbin.jcasbin.main.Enforcer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class JCasBinFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(JCasBinFilter.class);

    private final Enforcer enforcer;
    private final UserAccessService userAccessService;

    public JCasBinFilter(Enforcer enforcer, UserAccessService userAccessService) {
        this.enforcer = enforcer;
        this.userAccessService = userAccessService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.info("initializing ...");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)request;
        String sessionId = httpServletRequest.getSession(true).getId();
        Optional<UserData> userData = userAccessService.isAuthenticated(sessionId);
        if (userData.isPresent()) {

            String user = userData.get().getUserName();
            String method = httpServletRequest.getMethod();
            String path = httpServletRequest.getRequestURI();

            if (enforcer.enforce(user, path, method)) {

                LOG.info("session is authorized: {} {} {} {}", sessionId, userData.get().getUserName(),
                        httpServletRequest.getMethod(), httpServletRequest.getRequestURI());
                List<String> rolesForUser = enforcer.getRolesForUser(user);
                SecurityContext securityContext = SecurityContextHolder.getContext();
                securityContext.setAuthentication(new AuthenticationImpl(userData.get().getUserName(), rolesForUser));
                HttpSession session = httpServletRequest.getSession();
                session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
                chain.doFilter(request, response);

            } else {

                LOG.error("session is not authorized: {} {} {} {}", sessionId, userData.get().getUserName(),
                        httpServletRequest.getMethod(), httpServletRequest.getRequestURI());
                HttpServletResponse httpServletResponse = (HttpServletResponse)response;
                httpServletResponse.setStatus(HttpStatus.FORBIDDEN.value());
            }

        } else {
            LOG.error("session is not authenticated: {}", sessionId);
            HttpServletResponse httpServletResponse = (HttpServletResponse)response;
            httpServletResponse.setStatus(HttpStatus.FORBIDDEN.value());
        }
    }

    @Override
    public void destroy() {
        LOG.info("destroy.");
    }
}
