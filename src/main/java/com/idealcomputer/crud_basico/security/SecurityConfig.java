package com.idealcomputer.crud_basico.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

        // ✅ TODAS AS ORIGENS PERMITIDAS (Localhost + Vercel + Domínio)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",                                          // Desenvolvimento local
                "https://frontendidealcomputeronline-vercel.vercel.app",         // Vercel
                "https://idealcomputer.com.br",                                   // Domínio principal
                "https://www.idealcomputer.com.br"                                // Domínio com www
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
                        // ========================================
                        // 🔓 ROTAS PÚBLICAS (SEM AUTENTICAÇÃO)
                        // ========================================
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/recommendations/**").permitAll()

                        // ========================================
                        // 📖 COMPONENTES: GET PÚBLICO, POST/PUT/DELETE ADMIN
                        // ========================================
                        // ✅ Qualquer usuário logado pode LISTAR (GET)
                        .requestMatchers(HttpMethod.GET, "/api/cpus/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/gpus/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/placas-mae/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/memorias-ram/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/armazenamentos/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/fontes/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/gabinetes/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/refrigeracoes/**").authenticated()

                        // ❌ Apenas ADMIN pode CRIAR/EDITAR/DELETAR
                        .requestMatchers(HttpMethod.POST, "/api/cpus/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/cpus/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/cpus/**").hasAuthority("ADMINISTRADOR")

                        .requestMatchers(HttpMethod.POST, "/api/gpus/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/gpus/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/gpus/**").hasAuthority("ADMINISTRADOR")

                        .requestMatchers(HttpMethod.POST, "/api/placas-mae/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/placas-mae/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/placas-mae/**").hasAuthority("ADMINISTRADOR")

                        .requestMatchers(HttpMethod.POST, "/api/memorias-ram/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/memorias-ram/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/memorias-ram/**").hasAuthority("ADMINISTRADOR")

                        .requestMatchers(HttpMethod.POST, "/api/armazenamentos/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/armazenamentos/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/armazenamentos/**").hasAuthority("ADMINISTRADOR")

                        .requestMatchers(HttpMethod.POST, "/api/fontes/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/fontes/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/fontes/**").hasAuthority("ADMINISTRADOR")

                        .requestMatchers(HttpMethod.POST, "/api/gabinetes/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/gabinetes/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/gabinetes/**").hasAuthority("ADMINISTRADOR")

                        .requestMatchers(HttpMethod.POST, "/api/refrigeracoes/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/refrigeracoes/**").hasAuthority("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/refrigeracoes/**").hasAuthority("ADMINISTRADOR")

                        // ========================================
                        // 👥 USUÁRIOS: APENAS ADMIN
                        // ========================================
                        .requestMatchers("/api/usuarios/**").hasAuthority("ADMINISTRADOR")

                        // ========================================
                        // 🔐 BUILDS: USUÁRIO AUTENTICADO
                        // ========================================
                        .requestMatchers("/api/builds/**").authenticated()

                        // ========================================
                        // 🔒 QUALQUER OUTRA ROTA: PRECISA ESTAR AUTENTICADO
                        // ========================================
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}