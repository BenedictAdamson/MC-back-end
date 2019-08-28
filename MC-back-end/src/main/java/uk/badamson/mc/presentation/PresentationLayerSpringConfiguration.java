package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2018-19.
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.config.EnableWebFlux;

import uk.badamson.mc.Authority;

/**
 * <p>
 * The Spring Boot configuration for the presentation layer of the Mission
 * Command game.
 * </p>
 */
@EnableWebFlux
@EnableWebFluxSecurity
@ComponentScan("uk.badamson.mc.presentation")
public class PresentationLayerSpringConfiguration {

   private void authorizeAdministration(final ServerHttpSecurity http) {
      http.authorizeExchange().pathMatchers(HttpMethod.POST, "/api/player")
               .hasAuthority(Authority.ROLE_ADMIN.getAuthority());
   }

   private void authorizePublic(final ServerHttpSecurity http) {
      http.authorizeExchange().pathMatchers(HttpMethod.GET, "/", "/login",
               "/logout", "/api/player").permitAll();
      http.authorizeExchange()
               .pathMatchers(HttpMethod.POST, "/login", "/logout").permitAll();
   }

   @Bean
   public SecurityWebFilterChain securityWebFilterChain(
            final ServerHttpSecurity http) {
      http.formLogin();
      authorizeAdministration(http);
      authorizePublic(http);
      // All other POSTs, PUTs and DELETEs will be Forbidden.
      // Ensure Not Found response for GET unknown resources:
      http.authorizeExchange().anyExchange().permitAll();
      return http.build();
   }
}
