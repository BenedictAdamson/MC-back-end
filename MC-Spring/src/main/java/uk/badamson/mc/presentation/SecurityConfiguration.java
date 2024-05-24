package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2020-24.
 *
 * This file is part of MC.
 *
 * MC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with MC.  If not, see <https://www.gnu.org/licenses/>.
 */

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * <p>
 * Spring configuration for Spring Security for web MVC.
 * </p>
 */
@Configuration
@EnableWebSecurity
@SuppressFBWarnings(value = "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION",
        justification = "delegates to framework method that does so")
public class SecurityConfiguration {

    @Bean
    @Order(2)
    public SecurityFilterChain  authenticatedPathsSecurityFilterChain(final HttpSecurity http)
            throws Exception {
        return http.securityMatcher("/api/user/**", "/api/game/**").authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
        ).build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain permitAllPathsSecurityFilterChain(final HttpSecurity http)
            throws Exception {
        return http.securityMatcher("/login", "/logout").authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll()
        ).build();
    }


    private static void configureCsrfProtection(final HttpSecurity http)
            throws Exception {
        http.csrf(customizer -> customizer.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
    }

    private static void configureHttpBasic(final HttpSecurity http)
            throws Exception {
        http.httpBasic(Customizer.withDefaults());
    }

    @Bean
    @Order(1)
    public SecurityFilterChain pathlessSecurityFilterChain(HttpSecurity http) throws Exception {
        configureHttpBasic(http);
        configureCsrfProtection(http);
        // login and logout pages are configured by default
        return http.build();
    }

}
