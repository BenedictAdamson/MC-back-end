package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2020.
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

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * <p>
 * Spring configuration for Spring Security for web MVC.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

   private static void configureAuthorizedRequests(final HttpSecurity http)
            throws Exception {
      http.authorizeRequests().antMatchers("/api/user/**").authenticated();
      http.authorizeRequests().antMatchers("/login", "/logout").permitAll();
      http.authorizeRequests().antMatchers("/api/scenario/*/game/players")
               .authenticated();
      http.authorizeRequests().antMatchers("/api/scenario/*/game/").permitAll();
   }

   private static void configureCsrfProtection(final HttpSecurity http)
            throws Exception {
      http.csrf().csrfTokenRepository(
               CookieCsrfTokenRepository.withHttpOnlyFalse());
   }

   private static void configureHttpBasic(final HttpSecurity http)
            throws Exception {
      http.httpBasic();
   }

   @Override
   protected void configure(final HttpSecurity http) throws Exception {
      configureHttpBasic(http);
      configureCsrfProtection(http);
      configureAuthorizedRequests(http);
      // login and logout pages are configured by default
   }

}
