package com.utibunna.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de ms-market.
 *
 * Este microservicio NO valida JWT directamente.
 * El API Gateway (puerto 8080) ya verificó el token antes de enrutar
 * la petición hasta aquí, por eso permitimos todas las rutas.
 *
 * Las rutas de Swagger también quedan abiertas para facilitar las pruebas.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()   // El Gateway es la única puerta de entrada
            );
        return http.build();
    }
}
