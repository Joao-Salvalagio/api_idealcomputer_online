package com.idealcomputer.crud_basico.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ PERMITIR MÚLTIPLAS ORIGENS (localhost + Vercel)
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",              // Qualquer porta localhost
                "https://*.vercel.app"             // Qualquer subdomínio do Vercel
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // ✅ ROTAS PÚBLICAS (sem autenticação)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/recommendations/**").permitAll()

                        // ✅ ROTAS PROTEGIDAS (qualquer usuário logado pode acessar)
                        .requestMatchers("/api/builds/**").authenticated()

                        // ✅ ROTAS ADMIN (só ADMINISTRADOR)
                        .requestMatchers("/api/usuarios/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers("/api/cpus/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers("/api/gpus/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers("/api/placas-mae/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers("/api/memorias-ram/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers("/api/armazenamentos/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers("/api/fontes/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers("/api/gabinetes/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers("/api/refrigeracoes/**").hasAuthority("ADMINISTRADOR")

                        // ✅ QUALQUER OUTRA ROTA PRECISA ESTAR AUTENTICADA
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}