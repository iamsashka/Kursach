    package com.example.clothingstore.security;

    import jakarta.servlet.FilterChain;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.security.core.userdetails.UserDetails;
    import org.springframework.security.core.userdetails.UserDetailsService;
    import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
    import org.springframework.stereotype.Component;
    import org.springframework.util.StringUtils;
    import org.springframework.web.filter.OncePerRequestFilter;

    import java.io.IOException;

    @Component
    public class JwtAuthenticationFilter extends OncePerRequestFilter {

        private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

        private final JwtTokenProvider jwtTokenProvider;
        private final UserDetailsService userDetailsService;

        public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                       UserDetailsService userDetailsService) {
            this.jwtTokenProvider = jwtTokenProvider;
            this.userDetailsService = userDetailsService;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            try {
                // ✅ Получаем JWT токен из запроса
                String jwt = getJwtFromRequest(request);

                if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                    // ✅ Валидация токена (требование 3.3 - безопасность)
                    String username = jwtTokenProvider.getUsernameFromToken(jwt);

                    // ✅ Загрузка пользовательских данных (требование 3.3 - RBAC)
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // ✅ Создание аутентификации (требование 3.2 - обработка исключений)
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // ✅ Установка контекста безопасности
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Successfully authenticated user via JWT: {}", username);
                }
            } catch (Exception ex) {
                // ✅ Обработка ошибок (требование 3.2 - человекочитаемые сообщения)
                log.error("Failed to set user authentication in security context", ex);
                // Не прерываем цепочку фильтров - пусть запрос продолжает обработку
            }

            filterChain.doFilter(request, response);
        }

        /**
         * ✅ Извлечение JWT токена из заголовка Authorization
         * Соответствует требованию 3.3 - защита информации
         */
        private String getJwtFromRequest(HttpServletRequest request) {
            String bearerToken = request.getHeader("Authorization");
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
            return null;
        }
    }