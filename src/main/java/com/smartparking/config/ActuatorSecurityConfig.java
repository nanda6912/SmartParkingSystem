package com.smartparking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("prod") // Only apply in production
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        // Configure actuator endpoints security
        http.securityMatcher(request -> 
                request.getRequestURI().startsWith("/actuator")
            )
            .authorizeHttpRequests(authorizeRequests -> 
                authorizeRequests.anyRequest().authenticated()
            )
            .httpBasic();
        
        return http.build();
    }
}
