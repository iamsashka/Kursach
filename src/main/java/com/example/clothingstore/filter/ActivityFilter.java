package com.example.clothingstore.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
public class ActivityFilter extends HttpFilter {
    private static final long INACTIVITY_SECONDS = 3 * 60;
    private static final long ABSOLUTE_SECONDS = 15 * 60;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Instant now = Instant.now();
            Instant created = Instant.ofEpochMilli(session.getCreationTime());
            Instant last = session.getAttribute("LAST_ACTIVITY") == null ? created : (Instant) session.getAttribute("LAST_ACTIVITY");

            long inactive = Duration.between(last, now).getSeconds();
            long alive = Duration.between(created, now).getSeconds();

            if (inactive > INACTIVITY_SECONDS || alive > ABSOLUTE_SECONDS) {
                session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login?timeout");
                return;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                session.setAttribute("LAST_ACTIVITY", now);
            } else {
                session.setAttribute("LAST_ACTIVITY", now);
            }
        }

        chain.doFilter(request, response);
    }
}
