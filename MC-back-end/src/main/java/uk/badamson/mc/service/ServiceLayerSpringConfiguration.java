package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2018-20.
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import uk.badamson.mc.repository.PlayerRepository;

/**
 * <p>
 * The Spring Boot configuration of the Mission Command game.
 * </p>
 */
@Configuration
public class ServiceLayerSpringConfiguration {

   @Bean
   public PasswordEncoder passwordEncoder() {
      return PasswordEncoderFactories.createDelegatingPasswordEncoder();
   }

   @Bean
   public ReactiveAuthenticationManager reactiveAuthenticationManager(
            @NonNull final PasswordEncoder passwordEncoder,
            @NonNull final ReactiveUserDetailsService userDetailsService) {
      final var provider = new UserDetailsRepositoryReactiveAuthenticationManager(
               userDetailsService);
      provider.setPasswordEncoder(passwordEncoder);
      return provider;
   }

   /**
    * <p>
    * Create the the service layer.
    * </p>
    *
    * @return the service layer.
    */
   @Bean
   public Service service(@NonNull final PasswordEncoder passwordEncoder,
            @NonNull final PlayerRepository playerRepository,
            @NonNull @Value("${administrator.password:${random.uuid}}") final String administratorPassword) {
      return new ServiceImpl(passwordEncoder, playerRepository,
               administratorPassword);
   }
}
