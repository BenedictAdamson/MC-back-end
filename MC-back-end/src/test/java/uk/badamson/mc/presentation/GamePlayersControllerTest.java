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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.EnumSet;
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
import uk.badamson.mc.Game;
import uk.badamson.mc.GamePlayers;
import uk.badamson.mc.TestConfiguration;
import uk.badamson.mc.User;
import uk.badamson.mc.repository.GameRepository;
import uk.badamson.mc.service.GameService;
import uk.badamson.mc.service.ScenarioService;

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
         final var id = new Game.Identifier(UUID.randomUUID(), Instant.now());
         // Tough test: user has authority
         final var response = test(id, USER_WITH_ALL_AUTHORITIES, true);

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
         final var id = createGame();

         final var response = test(id, USER_WITH_ALL_AUTHORITIES, false);

         response.andExpect(status().isForbidden());
      }

      @Test
      public void notPermitted() throws Exception {
         // Tough test: game exists, user has all other authorities, and CSRF
         // token provided
         final var authorities = EnumSet.complementOf(EnumSet
                  .of(Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES));
         final var user = new User(USER_ID_A, "allan", "letmein", authorities,
                  true, true, true, true);
         final var id = createGame();

         final var response = test(id, user, true);

         response.andExpect(status().isForbidden());
      }

      @Test
      public void permitted() throws Exception {
         final var id = createGame();
         final var expectedRedirectionLocation = GamePlayersController
                  .createPathForGamePlayersOf(id);
         // Tough test: user has a minimum set of authorities
         final var authorities = EnumSet.of(Authority.ROLE_MANAGE_GAMES);
         final var user = new User(USER_ID_A, "allan", "letmein", authorities,
                  true, true, true, true);

         final var response = test(id, user, true);

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
         final var id = new Game.Identifier(UUID.randomUUID(), Instant.now());
         // Tough test: user has authority
         final var response = test(id, USER_WITH_ALL_AUTHORITIES);

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
         final var user = new User(USER_ID_A, "allan", "letmein", authorities,
                  true, true, true, true);
         final var id = createGame();

         final var response = test(id, user);

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
         final var user = new User(USER_ID_A, "allan", "letmein", authorities,
                  true, true, true, true);

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

   private static final UUID USER_ID_A = UUID.randomUUID();

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
      final var scenario = scenarioService.getScenarioIdentifiers().findAny()
               .get();
      final var game = gameService.create(scenario);
      return game.getIdentifier();
   }
}
