package com.madcamp.moody.config;

import com.madcamp.moody.user.OAuth2UserServiceRouter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OAuth2UserServiceRouter oAuth2UserServiceRouter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/**", "/logout") // API 요청과 로그아웃은 CSRF 제외
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**", "/api/public/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserServiceRouter)
                )
                .successHandler(authenticationSuccessHandler())
                .failureUrl("http://127.0.0.1:3000/?error=true")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("http://127.0.0.1:3000/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );
        
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, 
                                            HttpServletResponse response, 
                                            org.springframework.security.core.Authentication authentication) 
                                            throws IOException, ServletException {
                
                // OAuth2 사용자 정보 가져오기
                if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                    org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                        (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
                    
                    // Spotify 사용자인지 확인
                    String displayName = oauth2User.getAttribute("display_name");
                    String country = oauth2User.getAttribute("country");
                    boolean isSpotifyUser = (displayName != null) || (country != null);
                    
                    System.out.println("=== Authentication Success Handler ===");
                    System.out.println("사용자 타입: " + (isSpotifyUser ? "Spotify" : "카카오"));
                    System.out.println("사용자 ID: " + oauth2User.getAttribute("id"));
                    System.out.println("세션 ID: " + request.getSession().getId());
                    
                    // 세션에 로그인 제공자 정보 저장
                    jakarta.servlet.http.HttpSession session = request.getSession();
                    if (isSpotifyUser) {
                        session.setAttribute("spotify_logged_in", true);
                        session.setAttribute("spotify_user_id", oauth2User.getAttribute("id"));
                        System.out.println("Spotify 로그인 정보를 세션에 저장");
                        
                        // Spotify 사용자도 dashboard로 리다이렉트
                        System.out.println("Spotify 사용자를 dashboard로 리다이렉트");
                    } else {
                        session.setAttribute("kakao_logged_in", true);
                        session.setAttribute("kakao_user_id", oauth2User.getAttribute("id"));
                        System.out.println("카카오 로그인 정보를 세션에 저장");
                    }
                }
                
                getRedirectStrategy().sendRedirect(request, response, "http://127.0.0.1:3000/dashboard");
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://127.0.0.1:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}