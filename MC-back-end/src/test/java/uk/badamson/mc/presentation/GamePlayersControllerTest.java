package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2020-22.
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
import uk.badamson.mc.*;
import uk.badamson.mc.repository.GameSpringRepository;
import uk.badamson.mc.rest.GamePlayersResponse;
import uk.badamson.mc.service.GamePlayersSpringService;
import uk.badamson.mc.service.GameSpringService;
import uk.badamson.mc.service.ScenarioSpringService;
import uk.badamson.mc.service.UserSpringService;
import uk.badamson.mc.spring.SpringUser;

import java.time.Instant;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class GamePlayersControllerTest {

    @Autowired
    GameSpringRepository gameRepository;
    @Autowired
    ScenarioSpringService scenarioService;
    @Autowired
    GameSpringService gameService;
    @Autowired
    UserSpringService userService;
    @Autowired
    GamePlayersSpringService gamePlayersService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    private Game.Identifier createGame() {
        final Optional<UUID> gameOptional = scenarioService.getScenarioIdentifiers().findAny();
        if (gameOptional.isEmpty()) {
            throw new NoSuchElementException();
        }
        final var scenario = gameOptional.get();
        final var game = gameService.create(scenario);
        return game.getIdentifier();
    }

    private User createUser(final Set<Authority> authorities) {
        return userService.add(new BasicUserDetails(Fixtures.createUserName(), "password",
                authorities, true, true, true, true));
    }

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
                request = request.with(user(SpringUser.convertToSpring(user)));
            }
            if (hasCsrfToken) {
                request = request.with(csrf());
            }

            return mockMvc.perform(request);
        }

    }

    @Nested
    public class GetCurrentGame {

        @Test
        public void hasCurrentGame() throws Exception {
            final var game = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = createUser(EnumSet.of(Authority.ROLE_PLAYER));
            gamePlayersService.userJoinsGame(user.getId(), game);

            final var response = test(user);

            response.andExpect(status().isTemporaryRedirect()).andExpect(header()
                    .string("Location", GameController.createPathFor(game)));
        }

        @Test
        public void noAuthentication() throws Exception {
            // Tough test: a game exists
            createGame();

            final var response = test(null);

            /*
             * Must return Not Found rather than Unauthorized, because otherwise
             * web browsers will pop up an authentication dialogue
             */
            response.andExpect(status().isNotFound());
        }

        @Test
        public void noGames() throws Exception {
            // Tough test: user has all authorities
            final var user = createUser(Authority.ALL);
            final var response = test(user);

            response.andExpect(status().isNotFound());
        }

        private ResultActions test(final User user) throws Exception {
            var request = get(GamePlayersController.CURRENT_GAME_PATH)
                    .with(csrf());
            if (user != null) {
                request = request.with(user(SpringUser.convertToSpring(user)));
            }

            return mockMvc.perform(request);
        }

    }

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
        public void noAuthentication() throws Exception {
            // Tough test: game exists
            final var id = createGame();

            final var response = test(id, null);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void notPermitted() throws Exception {
            // Tough test: game exists and user has all the other authorities
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
                request = request.with(user(SpringUser.convertToSpring(user)));
            }

            return mockMvc.perform(request);
        }

        @Nested
        public class Valid {

            @Test
            public void asGamesManager() throws Exception {
                test(Authority.ROLE_MANAGE_GAMES, true);
            }

            @Test
            public void asPlayer() throws Exception {
                test(Authority.ROLE_PLAYER, false);
            }

            private void test(final Authority authority,
                              final boolean expectListsOtherPlayer) throws Exception {
                // Tough test: user has a minimum set of authorities
                final var user = createUser(EnumSet.of(authority));
                final var player = userService.add(new BasicUserDetails(Fixtures.createUserName(),
                        "password", EnumSet.of(Authority.ROLE_PLAYER),
                        true, true,
                        true, true));
                final var id = createGame();
                gamePlayersService.userJoinsGame(player.getId(), id);
                final var expectedPlayers = expectListsOtherPlayer
                        ? Set.of(player.getId())
                        : Set.of();

                final var response = GetGamePlayers.this.test(id, user);

                response.andExpect(status().isOk());
                final var jsonResponse = response.andReturn().getResponse()
                        .getContentAsString();
                final var gamePlayersResponse = objectMapper.readValue(jsonResponse,
                        GamePlayersResponse.class);
                assertAll(
                        () -> assertThat("scenario", gamePlayersResponse.game().scenario(), is(id.getScenario())),
                        () -> assertThat("created", gamePlayersResponse.game().created(), is(id.getCreated())),
                        () -> assertThat("players",
                                Set.copyOf(gamePlayersResponse.users().values()),
                                is(expectedPlayers)));
            }

        }

    }

    @Nested
    public class JoinGame {

        @Test
        public void absent() throws Exception {
            final var game = new Game.Identifier(UUID.randomUUID(), Instant.now());
            // Tough test: user has authority
            final var user = createUser(Authority.ALL);
            final var response = performRequest(game, user, true);

            response.andExpect(status().isNotFound());
        }

        @Test
        public void noAuthentication() throws Exception {
            // Tough test: game exists and CSRF token provided
            final var id = createGame();

            final var response = performRequest(id, null, true);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void noCsrfToken() throws Exception {
            // Tough test: game exists and user has all authorities
            final var game = createGame();
            final var user = createUser(Authority.ALL);

            final var response = performRequest(game, user, false);

            response.andExpect(status().isForbidden());
            final Optional<GamePlayers> gamePlayersOptional = gamePlayersService
                    .getGamePlayersAsGameManager(game);
            assertThat("gamePlayers", gamePlayersOptional.isPresent());
            final var gamePlayers = gamePlayersOptional.get();
            assertThat("User not added to players of game",
                    gamePlayers.getUsers().values(), not(hasItem(user.getId())));
        }

        @Test
        public void notPermitted() throws Exception {
            /*
             * Tough test: game exists, user has all other authorities, and CSRF
             * token provided
             */
            final var game = createGame();
            final var authorities = EnumSet
                    .complementOf(EnumSet.of(Authority.ROLE_PLAYER));
            final var user = createUser(authorities);

            final var response = performRequest(game, user, true);

            response.andExpect(status().isForbidden());
            final Optional<GamePlayers> gamePlayersOptional = gamePlayersService
                    .getGamePlayersAsGameManager(game);
            assertThat("gamePlayers", gamePlayersOptional.isPresent());
            final var gamePlayers = gamePlayersOptional.get();
            assertThat("User not added to players of game",
                    gamePlayers.getUsers().values(), not(hasItem(user.getId())));
        }

        @Test
        public void notRecruiting() throws Exception {
            /*
             * Tough test: game exists, user has all authorities, and CSRF token
             * provided
             */
            final var game = createGame();
            final var user = createUser(Authority.ALL);
            gamePlayersService.endRecruitment(game);

            final var response = performRequest(game, user, true);

            response.andExpect(status().isConflict());
            Optional<GamePlayers> gamePlayersOptional = gamePlayersService
                    .getGamePlayersAsGameManager(game);
            assertThat("gamePlayers", gamePlayersOptional.isPresent());
            final var gamePlayers = gamePlayersOptional.get();
            assertThat("User not added to players of game",
                    gamePlayers.getUsers().values(), not(hasItem(user.getId())));
        }

        private ResultActions performRequest(final Game.Identifier game,
                                             final User user, final boolean hasCsrfToken) throws Exception {
            final var path = GamePlayersController.createPathForJoining(game);
            var request = post(path);
            if (user != null) {
                request = request.with(user(SpringUser.convertToSpring(user)));
            }
            if (hasCsrfToken) {
                request = request.with(csrf());
            }

            return mockMvc.perform(request);
        }

        @Test
        public void playingOtherGame() throws Exception {
            /*
             * Tough test: game exists, user has all authorities, and CSRF token
             * provided
             */
            final var gameA = createGame();
            Thread.sleep(10);// ensure can create a game with a new unique ID
            final var gameB = createGame();
            assert !gameA.equals(gameB);
            final var user = createUser(Authority.ALL);
            gamePlayersService.userJoinsGame(user.getId(), gameA);

            final var response = performRequest(gameB, user, true);

            final Optional<GamePlayers> gamePlayersOptional = gamePlayersService
                    .getGamePlayersAsGameManager(gameB);
            assertThat("gamePlayers", gamePlayersOptional.isPresent());
            final var gamePlayers = gamePlayersOptional.get();
            assertAll(() -> response.andExpect(status().isConflict()),
                    () -> assertThat("User not added to players of game",
                            gamePlayers.getUsers().values(),
                            not(hasItem(user.getId()))));
        }

        @Test
        public void valid() throws Exception {
            final var game = createGame();
            final var expectedRedirectionLocation = GamePlayersController
                    .createPathForGamePlayersOf(game);
            // Tough test: user has a minimum set of authorities
            final var authorities = EnumSet.of(Authority.ROLE_PLAYER);
            final var user = createUser(authorities);

            final var response = performRequest(game, user, true);

            final var location = response.andReturn().getResponse()
                    .getHeaderValue("Location");
            final Optional<GamePlayers> gamePlayersOptional = gamePlayersService
                    .getGamePlayersAsGameManager(game);
            assertThat("gamePlayers", gamePlayersOptional.isPresent());
            final var gamePlayers = gamePlayersOptional.get();
            final var currentGame = gamePlayersService
                    .getCurrentGameOfUser(user.getId());
            assertAll(() -> response.andExpect(status().isFound()),
                    () -> assertEquals(expectedRedirectionLocation, location,
                            "redirection location"),
                    () -> assertThat("User added to players of game",
                            gamePlayers.getUsers().values(),
                            hasItem(user.getId())),
                    () -> assertThat("User has a current game",
                            currentGame.isPresent(), is(true)));
        }

    }

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
                request = request.with(user(SpringUser.convertToSpring(user)));
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
            // Tough test: game exists and user has all the other authorities
            final var authorities = EnumSet
                    .complementOf(EnumSet.of(Authority.ROLE_PLAYER));
            final var user = createUser(authorities);
            final var id = createGame();

            final var response = test(id, user);

            response.andExpect(status().isForbidden());
        }

    }
}
