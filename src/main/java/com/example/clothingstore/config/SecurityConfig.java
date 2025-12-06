package com.example.clothingstore.config;

import com.example.clothingstore.security.JwtAuthenticationFilter;
import com.example.clothingstore.service.CustomUserDetailsService;
import com.example.clothingstore.service.MetricsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true
)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MetricsService metricsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          MetricsService metricsService) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.metricsService = metricsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configure(http))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/webjars/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**", "/actuator/prometheus").permitAll()
                        .requestMatchers(
                                "/css/**", "/js/**", "/img/**", "/images/**", "/favicon.ico", "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/toggle-theme").permitAll()
                        .requestMatchers(
                                "/", "/home", "/home/**",
                                "/catalog/**", "/products/**",
                                "/register", "/login",
                                "/subscribe", "/toggle-theme",
                                "/access-denied"
                        ).permitAll()
                        .requestMatchers(
                                "/swagger-ui/**", "/v3/api-docs/**",
                                "/api-docs/**", "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/health",
                                "/api/products/public/**",
                                "/api/brands/public/**",
                                "/api/categories/public/**",
                                "/api/public/**",
                                "/api/favorites/check-auth"

                        ).permitAll()
                        .requestMatchers(
                                "/audit/**", "/api/audit/**"
                        ).hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(
                                "/admin/**", "/api/admin/**",
                                "/users/**", "/api/users/**"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                "/manager/**", "/api/manager/**",
                                "/analytics/**", "/api/analytics/**",
                                "/orders/manage/**", "/api/orders/manage/**"
                        ).hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(
                                "/profile/**", "/api/profile/**",
                                "/cart/**", "/api/cart/**",
                                "/favorites/**", "/api/favorites/**",
                                "/orders/**", "/api/orders/**", "/order-history/**","/checkout/**","/settings/**"
                        ).hasAnyRole("CUSTOMER", "MANAGER", "ADMIN")
                        .requestMatchers(
                                "/api/products/**", "/api/brands/**",
                                "/api/categories/**", "/api/orders/**"
                        ).authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            metricsService.userLoggedIn();
                            if (authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                                            a.getAuthority().equals("ROLE_MANAGER"))) {
                                response.sendRedirect("/index");
                            } else {
                                response.sendRedirect("/home");
                            }
                        })
                        .failureUrl("/login?error=true")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .addLogoutHandler((request, response, authentication) -> {
                            metricsService.userLoggedOut();
                        })
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access-denied")
                );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}