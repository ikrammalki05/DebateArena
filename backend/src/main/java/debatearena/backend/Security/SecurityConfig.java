package debatearena.backend.Security;

import debatearena.backend.Service.CustomUtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUtilisateurService customUtilisateurService;
    private final JwtUtil jwtUtil;

    public SecurityConfig(CustomUtilisateurService customUtilisateurService,
                          JwtUtil jwtUtil) {
        this.customUtilisateurService = customUtilisateurService;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Routes publiques pour auth et l'app mobile
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/**").permitAll()
                // Routes admin sécurisées
                .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN")
                // Tout le reste autorisé (pour test)
                .anyRequest().permitAll()
            )
            // JWT Filter pour /api/** si besoin
            .addFilterBefore(
                new JwtFilter(customUtilisateurService, jwtUtil),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
