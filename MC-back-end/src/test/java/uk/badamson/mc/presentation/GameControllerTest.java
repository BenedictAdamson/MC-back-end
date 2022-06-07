package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2020,22.
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.badamson.mc.Authority;
import uk.badamson.mc.Game;
import uk.badamson.mc.TestConfiguration;
import uk.badamson.mc.User;
import uk.badamson.mc.repository.GameRepository;
import uk.badamson.mc.service.GameService;
import uk.badamson.mc.service.ScenarioService;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>
 * Unit tests of the {@link GameController} class.
 * <p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class GameControllerTest {

   @Nested
   public class Create {
      @Test
      public void knownScenario() throws Exception {
         final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
         assertThat("scenario", scenarioOptional.isPresent());
         final var scenario = scenarioOptional.get();
         final var gamesForScenario0 = gameService.getGameIdentifiers()
                 .filter(gi -> scenario.equals(gi.getScenario())).collect(Collectors.toUnmodifiableSet());

         final var response = testAuthenticated(scenario,
                  USER_WITH_ALL_AUTHORITIES);

         final var id = gameService.getGameIdentifiers()
                  .filter(gi -> scenario.equals(gi.getScenario()))
                 .filter(gi -> !gamesForScenario0.contains(gi))
                 .findAny();
         final var location = response.andReturn().getResponse()
                  .getHeaderValue("Location");
         assertAll(
                  () -> assertTrue(id.isPresent(),
                           "created a game for the scenario"),
                  () -> response.andExpect(status().isFound()));
         assertEquals(GameController.createPathFor(id.get()),
                 location, "redirection location");
      }

      @Test
      public void noAuthentication() throws Exception {
         final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
         assertThat("scenario", scenarioOptional.isPresent());
         final var scenario = scenarioOptional
                  .get();
         final var nGames0 = gameService.getGameIdentifiers().count();
         final var request = post(GameController.createPathForGames(scenario))
                  .with(csrf());

         final var response = mockMvc.perform(request);

         final var nGames = gameService.getGameIdentifiers().count();
         assertAll(
                 () -> assertThat("Did not create a game", nGames, is(nGames0)),
                  () -> response.andExpect(status().isUnauthorized()));
      }

      @Test
      public void noCsrfToken() throws Exception {
         final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
         assertThat("scenario", scenarioOptional.isPresent());
         final var scenario = scenarioOptional.get();
         final var nGames0 = gameService.getGameIdentifiers().count();
         final var request = post(GameController.createPathForGames(scenario))
                  .with(user(USER_WITH_ALL_AUTHORITIES));

         final var response = mockMvc.perform(request);

         final var nGames = gameService.getGameIdentifiers().count();
         assertAll(
                  () -> assertThat("Did not create a game", nGames, is(nGames0)),
                  () -> response.andExpect(status().isForbidden()));
      }

      @Test
      public void notPermitted() throws Exception {
         final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
         assertThat("scenario", scenarioOptional.isPresent());
         final var scenario = scenarioOptional.get();
         final var authorities = EnumSet
                  .complementOf(EnumSet.of(Authority.ROLE_MANAGE_GAMES));
         final var user = new User(ID_A, "allan", "letmein", authorities, true,
                  true, true, true);
         final var nGames0 = gameService.getGameIdentifiers().count();

         final var response = testAuthenticated(scenario, user);

         final var nGames = gameService.getGameIdentifiers().count();
         assertAll(
                 () -> assertThat("Did not create a game", nGames, is(nGames0)),
                  () -> response.andExpect(status().is4xxClientError()));
      }

      private ResultActions testAuthenticated(final UUID scenario,
               final User user) throws Exception {
         final var request = post(GameController.createPathForGames(scenario))
                  .with(user(user)).with(csrf());

         return mockMvc.perform(request);
      }

      @Test
      public void unknowScenario() throws Exception {
         final var scenario = UUID.randomUUID();

         final var response = testAuthenticated(scenario,
                  USER_WITH_ALL_AUTHORITIES);

         response.andExpect(status().is4xxClientError());
      }

   }// class

   @Nested
   public class GetGame {

      @Nested
      public class Valid {

         @Test
         public void asGamesManager() throws Exception {
            test(Authority.ROLE_MANAGE_GAMES);
         }

         @Test
         public void asPlayer() throws Exception {
            test(Authority.ROLE_PLAYER);
         }

         private void test(final Authority authority)
                  throws Exception {
            final var id = createGame();
            final var user = createUser(EnumSet.of(authority));

            final var response = perform(id, user);

            response.andExpect(status().isOk());
            final var jsonResponse = response.andReturn().getResponse()
                     .getContentAsString();
            final var game = objectMapper.readValue(jsonResponse, Game.class);
            assertEquals(id, game.getIdentifier(), "game has the requested ID");
         }

      }// class

      @Test
      public void absent() throws Exception {
         final var id = new Game.Identifier(UUID.randomUUID(), Instant.now());
         /* Tough test: user is authorised. */
         final var response = perform(id, USER_WITH_ALL_AUTHORITIES);

         response.andExpect(status().isNotFound());
      }

      @Test
      public void noAuthentication() throws Exception {
         /* Tough test: game exists. */
         final var id = createGame();

         final var response = perform(id, null);

         response.andExpect(status().isUnauthorized());
      }

      @Test
      public void notAuthorised() throws Exception {
         /* Tough test: game exists and user has all other authorities */
         final Set<Authority> authorities = EnumSet.complementOf(EnumSet
                  .of(Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES));
         final var user = createUser(authorities);
         final var id = createGame();

         final var response = perform(id, user);

         response.andExpect(status().isForbidden());
      }

      private ResultActions perform(final Game.Identifier id, final User user)
               throws Exception {
         final var request = get(GameController.createPathFor(id))
                  .accept(MediaType.APPLICATION_JSON);
         if (user != null) {
            request.with(user(user));
         }

         return mockMvc.perform(request);
      }

   }// class

   @Nested
   public class GetGames {

      @Test
      public void absent() throws Exception {
         final var scenario = UUID.randomUUID();

         final var response = perform(scenario);

         response.andExpect(status().isNotFound());
      }

      @Test
      public void one() throws Exception {
         final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
         assertThat("scenario", scenarioOptional.isPresent());
         final var scenario = scenarioOptional.get();
         final var created = gameService.create(scenario).getIdentifier()
                  .getCreated();

         final var response = perform(scenario);

         response.andExpect(status().isOk());
         final var jsonResponse = response.andReturn().getResponse()
                  .getContentAsString();
         assertThat("Creation time is in ISO format", jsonResponse,
                  containsString(created.toString()));
         final var creationTimes = objectMapper.readValue(jsonResponse,
                  INSTANT_LIST);
         assertThat("creation times", creationTimes, hasItem(created));
      }

      private ResultActions perform(final UUID scenario) throws Exception {
         final var path = GameController.createPathForGames(scenario);
         final var request = get(path).accept(MediaType.APPLICATION_JSON);

         return mockMvc.perform(request);
      }

   }// class

   private static final UUID ID_A = UUID.randomUUID();

   private static final TypeReference<List<Instant>> INSTANT_LIST = new TypeReference<>() {
   };

   private static final User USER_WITH_ALL_AUTHORITIES = new User(
            UUID.randomUUID(), "jeff", "letmein", Authority.ALL, true, true,
            true, true);

   @Autowired
   GameRepository gameRepository;

   @Autowired
   ScenarioService scenarioService;

   @Autowired
   GameService gameService;

   @Autowired
   private ObjectMapper objectMapper;

   @Autowired
   private MockMvc mockMvc;

   private Game.Identifier createGame() {
      final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
      assertThat("scenario", scenarioOptional.isPresent());
      final var scenario = scenarioOptional.get();
      return gameService.create(scenario).getIdentifier();
   }

   private User createUser(final Set<Authority> authorities) {
      return new User(ID_A, "Allan", "secret", authorities, true, true, true,
               true);
   }
}
