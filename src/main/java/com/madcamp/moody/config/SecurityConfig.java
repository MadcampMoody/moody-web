package com.madcamp.moody.config;

import com.madcamp.moody.user.OAuth2UserServiceRouter;
import com.madcamp.moody.user.User;
import com.madcamp.moody.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;


import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2UserServiceRouter oAuth2UserServiceRouter;
    private final UserRepository userRepository;

    public SecurityConfig(OAuth2UserServiceRouter oAuth2UserServiceRouter, UserRepository userRepository) {
        this.oAuth2UserServiceRouter = oAuth2UserServiceRouter;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/login/oauth2/code/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login**", "/error", "/favicon.ico", "/manifest.json", "/logo192.png", "/logo512.png", "/robots.txt", "/static/**", "/*.js", "/*.css").permitAll()
                        .requestMatchers("/api/auth/spotify-auth-url", "/api/auth/spotify-callback").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserServiceRouter)
                        )
                        .successHandler(authenticationSuccessHandler())
                        .failureHandler((request, response, exception) -> {
                            System.err.println("Authentication failed: " + exception.getMessage());
                            response.sendRedirect("http://127.0.0.1:3000/?error=true");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String oauthId = oauth2User.getAttribute("id").toString();
            
            boolean isSpotifyUser = oauth2User.getAttributes().containsKey("display_name") || oauth2User.getAttributes().containsKey("country");
            User user = isSpotifyUser ? userRepository.findBySpotifyOauthId(oauthId) : userRepository.findByOauthId(oauthId);

            String redirectUrl;
            if (user != null && user.isOnboardingCompleted()) {
                redirectUrl = "http://127.0.0.1:3000/dashboard";
            } else {
                redirectUrl = "http://127.0.0.1:3000/onboarding";
            }
            response.sendRedirect(redirectUrl);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("http://127.0.0.1:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}