package uk.badamson.mc;
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import uk.badamson.mc.presentation.PresentationLayerSpringConfiguration;
import uk.badamson.mc.repository.GameRepository;
import uk.badamson.mc.repository.GameRepositoryTest;
import uk.badamson.mc.repository.UserRepository;
import uk.badamson.mc.repository.UserRepositoryTest;
import uk.badamson.mc.service.ServiceLayerSpringConfiguration;

/**
 * <p>
 * The Spring Boot configuration for tests of the Mission Command game.
 * </p>
 */
@Configuration
@Import(value = { MethodSecurityConfiguration.class,
         PresentationLayerSpringConfiguration.class,
         ServiceLayerSpringConfiguration.class })
public class TestConfiguration {

   @Bean
   GameRepository gameRepository() {
      return new GameRepositoryTest.Fake();
   }

   @Bean
   UserRepository userRepository() {
      return new UserRepositoryTest.Fake();
   }

}
