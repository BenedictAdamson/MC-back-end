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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.badamson.mc.Authority;
import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.Game;
import uk.badamson.mc.GamePlayers;
import uk.badamson.mc.TestConfiguration;
import uk.badamson.mc.User;
import uk.badamson.mc.repository.GameRepository;
import uk.badamson.mc.service.GamePlayersService;
import uk.badamson.mc.service.GameService;
import uk.badamson.mc.service.ScenarioService;
import uk.badamson.mc.service.UserService;

/**
 * <p>
 * Unit tests of the {@link GamePlayersController} class.
 * <p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class GamePlayersControllerTest {

   @Nested
   public class EndRecruitment {

      @Test
      public void absent() throws Exception {
         final var game = new Game.Identifier(UUID.randomUUID(), Instant.now());
         // Tough test: user has authority
         final var user = createUser(Authority.ALL);
         final var response = test(game, user, true);

         response.andExpect(status().isNotFound());
      }

      @Test
      public void noAuthentication() throws Exception {
         // Tough test: game exists and CSRF token provided
         final var id = createGame();

         final var response = test(id, null, true);

         response.andExpect(status().isUnauthorized());
      }

      @Test
      public void noCsrfToken() throws Exception {
         // Tough test: game exists and user has all authorities
         final var game = createGame();
         final var user = createUser(Authority.ALL);

         final var response = test(game, user, false);

         response.andExpect(status().isForbidden());
      }

      @Test
      public void notPermitted() throws Exception {
         // Tough test: game exists, user has all other authorities, and CSRF
         // token provided
         final var game = createGame();
         final var authorities = EnumSet.complementOf(EnumSet
                  .of(Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES));
         final var user = createUser(authorities);

         final var response = test(game, user, true);

         response.andExpect(status().isForbidden());
      }

      @Test
      public void permitted() throws Exception {
         final var game = createGame();
         final var expectedRedirectionLocation = GamePlayersController
                  .createPathForGamePlayersOf(game);
         // Tough test: user has a minimum set of authorities
         final var authorities = EnumSet.of(Authority.ROLE_MANAGE_GAMES);
         final var user = createUser(authorities);

         final var response = test(game, user, true);

         final var location = response.andReturn().getResponse()
                  .getHeaderValue("Location");
         assertAll(() -> response.andExpect(status().isFound()),
                  () -> assertEquals(expectedRedirectionLocation, location,
                           "redirection location"));
      }

      private ResultActions test(final Game.Identifier id, final User user,
               final boolean hasCsrfToken) throws Exception {
         final var path = GamePlayersController
                  .createPathForEndRecruitmentOf(id);
         var request = post(path);
         if (user != null) {
            request = request.with(user(user));
         }
         if (hasCsrfToken) {
            request = request.with(csrf());
         }

         return mockMvc.perform(request);
      }

   }// class

   @Nested
   public class GetGamePlayers {

      @Test
      public void absent() throws Exception {
         final var game = new Game.Identifier(UUID.randomUUID(), Instant.now());
         // Tough test: user has authority
         final var user = createUser(Authority.ALL);
         final var response = test(game, user);

         response.andExpect(status().isNotFound());
      }

      @Test
      public void asGamesManager() throws Exception {
         valid(Authority.ROLE_MANAGE_GAMES);
      }

      @Test
      public void asPlayer() throws Exception {
         valid(Authority.ROLE_PLAYER);
      }

      @Test
      public void noAuthentication() throws Exception {
         // Tough test: game exists
         final var id = createGame();

         final var response = test(id, null);

         response.andExpect(status().isUnauthorized());
      }

      @Test
      public void notPermitted() throws Exception {
         // Tough test: game exists and user has all other authorities
         final var authorities = EnumSet.complementOf(EnumSet
                  .of(Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES));
         final var user = createUser(authorities);
         final var game = createGame();

         final var response = test(game, user);

         response.andExpect(status().isForbidden());
      }

      private ResultActions test(final Game.Identifier id, final User user)
               throws Exception {
         final var path = GamePlayersController.createPathForGamePlayersOf(id);
         var request = get(path).accept(MediaType.APPLICATION_JSON);
         if (user != null) {
            request = request.with(user(user));
         }

         return mockMvc.perform(request);
      }

      private void valid(final Authority authority) throws Exception {
         final var id = createGame();
         // Tough test: user has a minimum set of authorities
         final var authorities = EnumSet.of(authority);
         final var user = createUser(authorities);

         final var response = test(id, user);

         response.andExpect(status().isOk());
         final var jsonResponse = response.andReturn().getResponse()
                  .getContentAsString();
         final var gamePlayers = objectMapper.readValue(jsonResponse,
                  GamePlayers.class);
         assertEquals(id, gamePlayers.getGame(),
                  "game-players has the requested ID");
      }

   }// class

   @Nested
   public class MayJoinGame {

      private Boolean expectValidResponseBody(final ResultActions response)
               throws Exception {
         response.andExpect(status().isOk());
         final var jsonResponse = response.andReturn().getResponse()
                  .getContentAsString();
         return objectMapper.readValue(jsonResponse, Boolean.class);
      }

      @Test
      public void may() throws Exception {
         final var game = createGame();
         // Tough test: user has a minimum set of authorities
         final var user = createUser(EnumSet.of(Authority.ROLE_PLAYER));

         final var response = test(game, user);

         final var may = expectValidResponseBody(response);
         assertTrue(may);
      }

      @Test
      public void noAuthentication() throws Exception {
         // Tough test: game exists
         final var id = createGame();

         final var response = test(id, null);

         response.andExpect(status().isUnauthorized());
      }

      @Test
      public void recruitmentEnded() throws Exception {
         final var game = createGame();
         gamePlayersService.endRecruitment(game);
         // Tough test: user has full authority
         final var user = createUser(Authority.ALL);

         final var response = test(game, user);

         final var may = expectValidResponseBody(response);
         assertFalse(may);
      }

      private ResultActions test(final Game.Identifier game, final User user)
               throws Exception {
         final var path = GamePlayersController
                  .createPathForMayJoinQueryOf(game);
         var request = get(path).accept(MediaType.APPLICATION_JSON)
                  .with(csrf());
         if (user != null) {
            request = request.with(user(user));
         }

         return mockMvc.perform(request);
      }

      @Test
      public void unknownGame() throws Exception {
         final var game = new Game.Identifier(UUID.randomUUID(), Instant.now());
         // Tough test: user has all authorities
         final var user = createUser(Authority.ALL);
         final var response = test(game, user);

         response.andExpect(status().isNotFound());
      }

      @Test
      public void userNotPermitted() throws Exception {
         // Tough test: game exists and user has all other authorities
         final var authorities = EnumSet
                  .complementOf(EnumSet.of(Authority.ROLE_PLAYER));
         final var user = createUser(authorities);
         final var id = createGame();

         final var response = test(id, user);

         response.andExpect(status().isForbidden());
      }

   }// class

   @Autowired
   GameRepository gameRepository;

   @Autowired
   ScenarioService scenarioService;

   @Autowired
   GameService gameService;

   @Autowired
   UserService userService;

   @Autowired
   GamePlayersService gamePlayersService;

   @Autowired
   private ObjectMapper objectMapper;

   @Autowired
   private MockMvc mockMvc;

   private Game.Identifier createGame() {
      final var scenario = scenarioService.getScenarioIdentifiers().findAny()
               .get();
      final var game = gameService.create(scenario);
      return game.getIdentifier();
   }

   private User createUser(final Set<Authority> authorities) {
      return userService.add(new BasicUserDetails("Allan", "letmein",
               authorities, true, true, true, true));
   }
}
