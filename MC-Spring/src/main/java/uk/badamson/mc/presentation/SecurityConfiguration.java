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
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.function.Supplier;

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

    private static final class CsrfCookieFilter extends OncePerRequestFilter {

        // Render the token value to a cookie by causing the deferred token to be loaded
        @Override
        protected void doFilterInternal(
                @Nonnull HttpServletRequest request,
                @Nonnull HttpServletResponse response,
                @Nonnull FilterChain filterChain
        ) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
            csrfToken.getToken();

            filterChain.doFilter(request, response);
        }
    }


    private static final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
        private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            /*
             * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
             * the CsrfToken when it is rendered in the response body.
             */
            this.delegate.handle(request, response, csrfToken);
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            /*
             * If the request contains a request header, use CsrfTokenRequestAttributeHandler
             * to resolve the CsrfToken. This applies when a single-page application includes
             * the header value automatically, which was obtained via a cookie containing the
             * raw CsrfToken.
             */
            if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
                return super.resolveCsrfTokenValue(request, csrfToken);
            }
            /*
             * In all other cases (e.g. if the request contains a request parameter), use
             * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
             * when a server-side rendered form includes the _csrf request parameter as a
             * hidden input.
             */
            return this.delegate.resolveCsrfTokenValue(request, csrfToken);
        }
    }

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
        http.csrf(customizer ->
                        customizer.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
    }

    private static void configureHttpBasic(final HttpSecurity http)
            throws Exception {
        // Store the authentication, despite Basic Authentication being stateless, to create a session.
        http.httpBasic(customizer -> customizer.addObjectPostProcessor(new ObjectPostProcessor<BasicAuthenticationFilter>() {
                    @Override
                    public <O extends BasicAuthenticationFilter> O postProcess(O filter) {
                        filter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());
                        return filter;
                    }
                })
        );
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
