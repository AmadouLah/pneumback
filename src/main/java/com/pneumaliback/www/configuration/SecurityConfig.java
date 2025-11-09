package com.pneumaliback.www.configuration;

import com.pneumaliback.www.enums.Role;
import com.pneumaliback.www.security.JwtAuthenticationFilter;
import com.pneumaliback.www.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration de sécurité Spring Security pour l'application PneuMali
 * 
 * Fonctionnalités :
 * - Authentification JWT stateless
 * - CORS configuré dynamiquement
 * - Headers de sécurité (CSP, HSTS, Referrer Policy)
 * - Endpoints publics pour auth, docs et health
 * - Autorisation basée sur les rôles (ADMIN, DEVELOPER, INFLUENCEUR, CLIENT)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        // ========== Constantes ==========

        private static final String[] PUBLIC_ENDPOINTS = {
                        "/api/auth/**",
                        "/api/contact/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/health",
                        "/ws/**"
        };

        private static final String CSP_POLICY = "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'";

        private static final long HSTS_MAX_AGE = 31536000L; // 1 an en secondes

        private static final String[] CORS_METHODS = {
                        "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        };

        // Rôles avec privilèges d'administration (DEVELOPER a tous les privilèges +
        // config système)
        private static final String[] ADMIN_ROLES = {
                        Role.ADMIN.name(),
                        Role.DEVELOPER.name()
        };

        // ========== Dépendances ==========

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final CustomUserDetailsService userDetailsService;

        @Value("${app.cors.allowed-origins:*}")
        private String allowedOriginsString;

        @Value("${app.cors.allowed-origin-patterns:}")
        private String allowedOriginPatternsString;

        // ========== Configuration principale ==========

        /**
         * Configure la chaîne de filtres de sécurité
         */
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                return http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .headers(this::configureSecurityHeaders)
                                .authorizeHttpRequests(this::configureAuthorizations)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider())
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .build();
        }

        // ========== Configuration des headers de sécurité ==========

        /**
         * Configure les headers de sécurité HTTP
         */
        private void configureSecurityHeaders(
                        org.springframework.security.config.annotation.web.configurers.HeadersConfigurer<?> headers) {
                headers
                                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP_POLICY))
                                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                                .frameOptions(frame -> frame.sameOrigin())
                                .httpStrictTransportSecurity(hsts -> hsts
                                                .includeSubDomains(true)
                                                .preload(true)
                                                .maxAgeInSeconds(HSTS_MAX_AGE));
        }

        // ========== Configuration des autorisations ==========

        /**
         * Configure les règles d'autorisation pour les endpoints
         * 
         * Hiérarchie des rôles :
         * - DEVELOPER : Accès total (admin + configuration système)
         * - ADMIN : Accès administration (gestion utilisateurs, produits, commandes)
         * - INFLUENCEUR : Accès espace influenceur (codes promo, commissions)
         * - CLIENT : Accès standard (achats, profil)
         */
        private void configureAuthorizations(
                        org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry auth) {
                auth
                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                // Consultation des produits accessible publiquement (GET uniquement)
                                .requestMatchers(HttpMethod.GET, "/api/products/active").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/products/latest").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/products/search").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/products/filter").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/products/{id}").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/products/popular").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/products/brands").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/products/dimensions").permitAll()
                                .requestMatchers("/api/admin/**").hasAnyRole(ADMIN_ROLES)
                                .requestMatchers("/api/influenceur/**").hasRole(Role.INFLUENCEUR.name())
                                .anyRequest().authenticated();
        }

        // ========== Configuration CORS ==========

        /**
         * Configure CORS dynamiquement selon les origines autorisées
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Parser listes
                List<String> allowedOrigins = (allowedOriginsString == null || allowedOriginsString.trim().isEmpty())
                                ? List.of("*")
                                : Arrays.stream(allowedOriginsString.split(","))
                                                .map(String::trim)
                                                .filter(s -> !s.isEmpty())
                                                .toList();

                List<String> allowedOriginPatterns = (allowedOriginPatternsString == null
                                || allowedOriginPatternsString.trim().isEmpty())
                                                ? List.of()
                                                : Arrays.stream(allowedOriginPatternsString.split(","))
                                                                .map(String::trim)
                                                                .filter(s -> !s.isEmpty())
                                                                .toList();

                boolean isWildcard = allowedOrigins.contains("*");

                if (isWildcard) {
                        // Dev: toutes origines, pas de credentials
                        configuration.setAllowedOriginPatterns(List.of("*"));
                        configuration.setAllowCredentials(false);
                } else {
                        // Prod: origines explicites et/ou motifs, avec credentials
                        if (!allowedOriginPatterns.isEmpty()) {
                                configuration.setAllowedOriginPatterns(allowedOriginPatterns);
                        } else {
                                configuration.setAllowedOrigins(allowedOrigins);
                        }
                        configuration.setAllowCredentials(true);
                }

                configuration.setAllowedMethods(Arrays.asList(CORS_METHODS));
                // Autoriser tous les en-têtes (inclut sec-ch-ua, etc.)
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
                configuration.setMaxAge(3600L); // Cache preflight 1h

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                // Appliquer sur toute l'API (y compris OPTIONS)
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        /**
         * Filtre CORS de haut niveau pour garantir les en-têtes sur toutes les réponses
         */
        @Bean
        public org.springframework.web.filter.CorsFilter corsFilter() {
                return new org.springframework.web.filter.CorsFilter(corsConfigurationSource());
        }

        // ========== Beans d'authentification ==========

        /**
         * Fournisseur d'authentification DAO
         * Combine UserDetailsService et PasswordEncoder pour l'authentification
         * 
         * Note: Les warnings de dépréciation sont supprimés car Spring Security 6.x
         * marque ces méthodes comme dépréciées mais elles restent fonctionnelles et
         * recommandées
         */
        @Bean
        @SuppressWarnings("deprecation")
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
                provider.setUserDetailsService(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
        }

        /**
         * Gestionnaire d'authentification global
         * Utilisé par AuthService pour authentifier les utilisateurs
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        /**
         * Encodeur de mot de passe BCrypt (force 10 par défaut)
         * Utilisé pour hacher les mots de passe et les codes de vérification
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
