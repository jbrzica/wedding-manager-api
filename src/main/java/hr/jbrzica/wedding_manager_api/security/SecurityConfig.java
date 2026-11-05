package hr.jbrzica.wedding_manager_api.security;

import hr.jbrzica.wedding_manager_api.repository.KorisnikRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtServis jwtServis,
                                           KorisnikRepository korisnikRepository) throws Exception {
        http
                // CSRF zaštita treba kod prijave preko cookieja; mi šaljemo token u zaglavlju
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                // Server ne pamti sesije: svaki zahtjev nosi svoj token
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/registracija",
                                "/api/auth/potvrdi-email",
                                "/api/auth/ponovno-posalji-kod",
                                "/api/auth/prijava",
                                "/api/auth/prihvati-poziv",
                                "/api/auth/zaboravljena-lozinka",
                                "/api/auth/nova-lozinka").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        .anyRequest().authenticated())
                // Neprijavljen korisnik dobije 401
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new JwtFilter(jwtServis, korisnikRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Dopušta frontendu s druge adrese (npr. Angular na localhost:4200) da zove API
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.dozvoljeni}") List<String> dozvoljeniOriginovi) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(dozvoljeniOriginovi);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
