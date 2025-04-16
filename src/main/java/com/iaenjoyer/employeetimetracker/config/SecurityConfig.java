package com.iaenjoyer.employeetimetracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.iaenjoyer.employeetimetracker.controller.CustomAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final AccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(AccessDeniedHandler accessDeniedHandler) {
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // Static resources
            .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
            
            // Public routes
            .requestMatchers("/", "/login", "/register", "/h2-console/**", "/logout").permitAll()
            
            // Admin routes
            .requestMatchers("/admin/**").hasRole("ADMIN")
            
            // Employee routes
            .requestMatchers("/employee/**").hasAnyRole("EMPLOYEE", "ADMIN")
            
            // Dashboard route
            .requestMatchers("/dashboard").authenticated()
            
            // Secure all other routes
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .successHandler(new CustomAuthenticationSuccessHandler()) // Usar el manejador personalizado
            .failureUrl("/login?error=true")
            .permitAll()
        )
        .logout(logout -> logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
            .logoutSuccessUrl("/login?logout=true")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll()
        )
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/h2-console/**")
            .ignoringRequestMatchers("/admin/**")  // Allow POST/DELETE in admin endpoints
        )
        .exceptionHandling(ex -> ex
            .accessDeniedHandler(accessDeniedHandler)
        );

    return http.build();
}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
