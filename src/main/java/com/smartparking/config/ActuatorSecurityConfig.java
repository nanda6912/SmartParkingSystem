package com.smartparking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class ActuatorSecurityConfig {

    @Bean
    @Order(1) // Ensure this security config has highest precedence
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        // Configure actuator endpoints security
        http.securityMatcher("/actuator/**")
            .authorizeHttpRequests(authorizeRequests -> 
                authorizeRequests.anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless API endpoints
            .httpBasic(org.springframework.security.config.Customizer.withDefaults());
        
        return http.build();
    }
}
