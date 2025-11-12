package com.idealcomputer.crud_basico.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // ✅ Libera TODAS as rotas
                .allowedOrigins(
                        "http://localhost:5173",                      // ✅ Localhost (dev)
                        "https://idealcomputer.vercel.app",           // ✅ Vercel
                        "https://www.idealcomputer.com.br",           // ✅ Domínio com www
                        "https://idealcomputer.com.br"                // ✅ Domínio sem www
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // ✅ Métodos permitidos
                .allowedHeaders("*")                                        // ✅ Todos os headers
                .allowCredentials(true)                                     // ✅ Permite cookies
                .maxAge(3600);                                              // ✅ Cache de 1 hora
    }
}