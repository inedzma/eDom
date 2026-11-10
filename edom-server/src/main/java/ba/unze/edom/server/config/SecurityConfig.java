package ba.unze.edom.server.config;

import ba.unze.edom.server.security.JwtFilter;
import ba.unze.edom.server.security.PrijavaUspjeh;
import ba.unze.edom.server.security.Uloge;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    private final PrijavaUspjeh prijavaUspjeh;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /** LANAC 1: /api/** — za desktop klijent. Bez sesije, samo JWT. */
    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/login").permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                // bez httpBasic: prijava je samo preko JWT-a (/api/auth/login → token)
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                // nema tokena ili je istekao → 401 (desktop tada traži ponovnu prijavu)
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** LANAC 2: sve ostalo — za web. Sesija i login forma. */
    @Bean
    @Order(2)
    public SecurityFilterChain webChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/", "/login", "/registracija", "/error", "/rang-lista",
                                "/zaboravljena-lozinka", "/zaboravljena-lozinka/**",
                                "/css/**", "/js/**", "/img/**", "/upute", "/upute/**", "/dokumenti/**", "/usluge", "/galerija/**",
                                "/kontakt", "/o-ustanovi", "/obavjestenja", "/obavjestenja/**").permitAll()
                        .requestMatchers("/urednik/**").access((auth, ctx1) ->
                                new AuthorizationDecision(Uloge.jeAdmin(auth.get())))
                        .requestMatchers("/student/**").hasRole("Student")
                        .anyRequest().authenticated()
                )
                .formLogin(f -> f
                        .loginPage("/login")
                        .successHandler(prijavaUspjeh)
                        .permitAll()
                )
                .exceptionHandling(e -> e.accessDeniedHandler((req, res, ex) -> {
                    if (ex instanceof org.springframework.security.web.csrf.CsrfException) {
                        // forma je "istekla" (npr. poslije restarta) -> vrati na istu stranicu s porukom
                        res.sendRedirect(req.getContextPath() + req.getServletPath() + "?istekla");
                    } else {
                        res.sendError(403);
                    }
                }))
                .logout(l -> l.logoutSuccessUrl("/").permitAll());
        return http.build();
    }
}