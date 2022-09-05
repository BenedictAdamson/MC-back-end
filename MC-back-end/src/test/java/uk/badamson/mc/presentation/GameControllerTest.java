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
import uk.badamson.mc.*;
import uk.badamson.mc.repository.GameSpringRepository;
import uk.badamson.mc.rest.GameResponse;
import uk.badamson.mc.rest.NamedUUID;
import uk.badamson.mc.rest.Paths;
import uk.badamson.mc.service.GameSpringService;
import uk.badamson.mc.service.ScenarioSpringService;
import uk.badamson.mc.service.UserSpringService;
import uk.badamson.mc.spring.SpringUser;

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
public class GameControllerTest {

    private static final UUID ID_A = UUID.randomUUID();
    private static final TypeReference<List<NamedUUID>> GAME_ID_LIST = new TypeReference<>() {
    };
    private static final User USER_WITH_ALL_AUTHORITIES = new User(
            UUID.randomUUID(), "jeff", "password", Authority.ALL,
            true, true, true, true);

    @Autowired
    GameSpringRepository gameRepository;
    @Autowired
    ScenarioSpringService scenarioService;
    @Autowired
    GameSpringService gameService;
    @Autowired
    UserSpringService userService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    private UUID createGame() {
        final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
        assertThat("scenario", scenarioOptional.isPresent());
        final var scenario = scenarioOptional.get();
        return gameService.create(scenario).getIdentifier();
    }

    private User createUser(final Set<Authority> authorities) {
        return userService.add(new BasicUserDetails(Fixtures.createUserName(), "password",
                authorities, true, true, true, true));
    }

    @Nested
    public class Create {
        @Test
        public void knownScenario() throws Exception {
            final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
            assertThat("scenario", scenarioOptional.isPresent());
            final var scenario = scenarioOptional.get();
            final Set<UUID> gamesForScenario0 = new HashSet<>();
            for (var id0 : gameService.getGameIdentifiers()) {
                gamesForScenario0.add(id0);
            }

            final var response = testAuthenticated(scenario,
                    USER_WITH_ALL_AUTHORITIES);

            final Set<UUID> gamesForScenario = new HashSet<>();
            for (var id : gameService.getGameIdentifiers()) {
                gamesForScenario.add(id);
            }
            final var idAddedOptional = gamesForScenario.stream()
                    .filter(id -> !gamesForScenario0.contains(id))
                    .findAny();
            assertThat(idAddedOptional.isPresent(), is(true));
            final var id = idAddedOptional.get();
            final var location = response.andReturn().getResponse()
                    .getHeaderValue("Location");
            response.andExpect(status().isFound());
            assertEquals(Paths.createPathForGame(id),
                    location, "redirection location");
        }

        @Test
        public void noAuthentication() throws Exception {
            final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
            assertThat("scenario", scenarioOptional.isPresent());
            final var scenario = scenarioOptional
                    .get();
            final var request = post(Paths.createPathForGamesOfScenario(scenario))
                    .with(csrf());

            final var response = mockMvc.perform(request);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void noCsrfToken() throws Exception {
            final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
            assertThat("scenario", scenarioOptional.isPresent());
            final var scenario = scenarioOptional.get();
            final var request = post(Paths.createPathForGamesOfScenario(scenario))
                    .with(user(SpringUser.convertToSpring(USER_WITH_ALL_AUTHORITIES)));

            final var response = mockMvc.perform(request);

            response.andExpect(status().isForbidden());
        }

        @Test
        public void notPermitted() throws Exception {
            final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
            assertThat("scenario", scenarioOptional.isPresent());
            final var scenario = scenarioOptional.get();
            final var authorities = EnumSet
                    .complementOf(EnumSet.of(Authority.ROLE_MANAGE_GAMES));
            final var user = new User(ID_A, "allan", "password", authorities, true,
                    true, true, true);

            final var response = testAuthenticated(scenario, user);

            response.andExpect(status().is4xxClientError());
        }

        private ResultActions testAuthenticated(final UUID scenario,
                                                final User user) throws Exception {
            final var request = post(Paths.createPathForGamesOfScenario(scenario))
                    .with(user(SpringUser.convertToSpring(user))).with(csrf());

            return mockMvc.perform(request);
        }

        @Test
        public void unknownScenario() throws Exception {
            final var scenario = UUID.randomUUID();

            final var response = testAuthenticated(scenario,
                    USER_WITH_ALL_AUTHORITIES);

            response.andExpect(status().is4xxClientError());
        }

    }

    @Nested
    public class GetGame {

        @Test
        public void absent() throws Exception {
            final var id = UUID.randomUUID();
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
            /* Tough test: game exists and user has all the other authorities */
            final Set<Authority> authorities = EnumSet.complementOf(EnumSet
                    .of(Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES));
            final var user = createUser(authorities);
            final var id = createGame();

            final var response = perform(id, user);

            response.andExpect(status().isForbidden());
        }

        private ResultActions perform(final UUID id, final User user)
                throws Exception {
            final var request = get(Paths.createPathForGame(id))
                    .accept(MediaType.APPLICATION_JSON);
            if (user != null) {
                request.with(user(SpringUser.convertToSpring(user)));
            }

            return mockMvc.perform(request);
        }

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
                final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
                assertThat("scenario", scenarioOptional.isPresent());
                final var scenarioId = scenarioOptional.get();
                final var identifiedGame = gameService.create(scenarioId);
                final var gameId = identifiedGame.getIdentifier();
                final var created = identifiedGame.getValue().getCreated();
                final var user = createUser(EnumSet.of(authority));

                final var response = perform(gameId, user);

                response.andExpect(status().isOk());
                final var jsonResponse = response.andReturn().getResponse().getContentAsString();
                final var gameResponse = objectMapper.readValue(jsonResponse, GameResponse.class);
                assertThat("scenario", gameResponse.scenario(), is(scenarioId));
                assertThat("created", gameResponse.created(), is(created));
            }

        }

    }

    @Nested
    public class GetGames {

        @Test
        public void absent() throws Exception {
            final var scenario = UUID.randomUUID();
            // Tough test: user has authority
            final var user = createUser(Authority.ALL);

            final var response = perform(scenario, user);

            response.andExpect(status().isNotFound());
        }
        @Test
        public void noAuthentication() throws Exception {
            // Tough test: scenario exists
            final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
            assertThat("scenario", scenarioOptional.isPresent());
            final var scenario = scenarioOptional.get();

            final var response = perform(scenario, null);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void notPermitted() throws Exception {
            // Tough test: game exists, user has all other authorities
            final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
            assert scenarioOptional.isPresent();
            final var scenario = scenarioOptional.get();
            final var authorities = EnumSet.complementOf(EnumSet
                    .of(Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES));
            final var user = createUser(authorities);

            final var response = perform(scenario, user);

            response.andExpect(status().isForbidden());
        }

        @Test
        public void permitted() throws Exception {
            // Tough test: user has minimum authority
            final var user = createUser(EnumSet.of(Authority.ROLE_PLAYER));
            final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers().findAny();
            assert scenarioOptional.isPresent();
            final var scenario = scenarioOptional.get();
            final var identifiedGame = gameService.create(scenario);
            final var gameId = identifiedGame.getIdentifier();
            final var created = identifiedGame.getValue().getCreated();
            final var expectedNamedId = new NamedUUID(gameId, created.toString());

            final var response = perform(scenario, user);

            response.andExpect(status().isOk());
            final var jsonResponse = response.andReturn().getResponse()
                    .getContentAsString();
            final var gameIds = objectMapper.readValue(jsonResponse, GAME_ID_LIST);
            assertThat(gameIds, hasItem(expectedNamedId));
        }

        private ResultActions perform(final UUID scenario,
                                      final User user) throws Exception {
            final var path = Paths.createPathForGamesOfScenario(scenario);
            var request = get(path).accept(MediaType.APPLICATION_JSON);
            if (user != null) {
                request = request.with(user(SpringUser.convertToSpring(user)));
            }

            return mockMvc.perform(request);
        }

    }

    @Nested
    public class EndRecruitment {

        @Test
        public void absent() throws Exception {
            final var game = UUID.randomUUID();
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
            final var expectedRedirectionLocation = Paths.createPathForGame(game);
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

        private ResultActions test(final UUID id, final User user,
                                   final boolean hasCsrfToken) throws Exception {
            final var path = GameController.createPathForEndRecruitmentOf(id);
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
            gameService.userJoinsGame(user.getId(), game);

            final var response = test(user);

            response.andExpect(status().isTemporaryRedirect()).andExpect(header()
                    .string("Location", Paths.createPathForGame(game)));
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
            var request = get(Paths.CURRENT_GAME_PATH)
                    .with(csrf());
            if (user != null) {
                request = request.with(user(SpringUser.convertToSpring(user)));
            }

            return mockMvc.perform(request);
        }

    }

    @Nested
    public class JoinGame {

        @Test
        public void absent() throws Exception {
            final var game = UUID.randomUUID();
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
            final Optional<Game> gameOptional = gameService.getGameAsGameManager(game).map(FindGameResult::game);
            assertThat("present", gameOptional.isPresent());
            final var gamePlayers = gameOptional.get();
            assertThat("User not added to players of game",
                    gamePlayers.getUsers().values(), not(hasItem(user.getId())));
        }

        @Test
        public void notPermitted() throws Exception {
            /*
             * Tough test: game exists, user has all other authorities, and CSRF
             * token provided
             */
            final var gameId = createGame();
            final var authorities = EnumSet
                    .complementOf(EnumSet.of(Authority.ROLE_PLAYER));
            final var user = createUser(authorities);

            final var response = performRequest(gameId, user, true);

            response.andExpect(status().isForbidden());
            final Optional<Game> gameOptional = gameService.getGameAsGameManager(gameId).map(FindGameResult::game);
            assertThat("present", gameOptional.isPresent());
            final var game = gameOptional.get();
            assertThat("User not added to players of game",
                    game.getUsers().values(), not(hasItem(user.getId())));
        }

        @Test
        public void notRecruiting() throws Exception {
            /*
             * Tough test: game exists, user has all authorities, and CSRF token
             * provided
             */
            final var gameId = createGame();
            final var user = createUser(Authority.ALL);
            gameService.endRecruitment(gameId);

            final var response = performRequest(gameId, user, true);

            response.andExpect(status().isConflict());
            Optional<Game> gameOptional = gameService.getGameAsGameManager(gameId).map(FindGameResult::game);
            assertThat("present", gameOptional.isPresent());
            final var game = gameOptional.get();
            assertThat("User not added to players of game",
                    game.getUsers().values(), not(hasItem(user.getId())));
        }

        private ResultActions performRequest(final UUID game,
                                             final User user, final boolean hasCsrfToken) throws Exception {
            final var path = GameController.createPathForJoining(game);
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
            final var gameIdA = createGame();
            Thread.sleep(10);// ensure can create a game with a new unique ID
            final var gameIdB = createGame();
            assert !gameIdA.equals(gameIdB);
            final var user = createUser(Authority.ALL);
            gameService.userJoinsGame(user.getId(), gameIdA);

            final var response = performRequest(gameIdB, user, true);

            final Optional<Game> gameOptional = gameService.getGameAsGameManager(gameIdB).map(FindGameResult::game);
            assertThat("present", gameOptional.isPresent());
            final var game = gameOptional.get();
            assertAll(() -> response.andExpect(status().isConflict()),
                    () -> assertThat("User not added to players of game",
                            game.getUsers().values(),
                            not(hasItem(user.getId()))));
        }

        @Test
        public void valid() throws Exception {
            final var gameId = createGame();
            final var expectedRedirectionLocation = Paths.createPathForGame(gameId);
            // Tough test: user has a minimum set of authorities
            final var authorities = EnumSet.of(Authority.ROLE_PLAYER);
            final var user = createUser(authorities);

            final var response = performRequest(gameId, user, true);

            final var location = response.andReturn().getResponse().getHeaderValue("Location");
            final Optional<Game> gameOptional = gameService.getGameAsGameManager(gameId).map(FindGameResult::game);
            assertThat("present", gameOptional.isPresent());
            final var game = gameOptional.get();
            final var currentGame = gameService.getCurrentGameOfUser(user.getId());
            assertAll(() -> response.andExpect(status().isFound()),
                    () -> assertEquals(expectedRedirectionLocation, location,
                            "redirection location"),
                    () -> assertThat("User added to players of game",
                            game.getUsers().values(), hasItem(user.getId())),
                    () -> assertThat("User has a current game",
                            currentGame.isPresent(), is(true)));
        }

    }

    @Nested
    public class MayJoinGame {

        private Boolean expectValidResponseBody(final ResultActions response)
                throws Exception {
            response.andExpect(status().isOk());
            final var jsonResponse = response.andReturn().getResponse().getContentAsString();
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
            gameService.endRecruitment(game);
            // Tough test: user has full authority
            final var user = createUser(Authority.ALL);

            final var response = test(game, user);

            final var may = expectValidResponseBody(response);
            assertFalse(may);
        }

        private ResultActions test(final UUID game, final User user)
                throws Exception {
            final var path = GameController.createPathForMayJoinQueryOf(game);
            var request = get(path).accept(MediaType.APPLICATION_JSON)
                    .with(csrf());
            if (user != null) {
                request = request.with(user(SpringUser.convertToSpring(user)));
            }

            return mockMvc.perform(request);
        }

        @Test
        public void unknownGame() throws Exception {
            final var game = UUID.randomUUID();
            // Tough test: user has all authorities
            final var user = createUser(Authority.ALL);
            final var response = test(game, user);

            response.andExpect(status().isNotFound());
        }

        @Test
        public void userNotPermitted() throws Exception {
            // Tough test: game exists and user has all the other authorities
            final var authorities = EnumSet.complementOf(EnumSet.of(Authority.ROLE_PLAYER));
            final var user = createUser(authorities);
            final var id = createGame();

            final var response = test(id, user);

            response.andExpect(status().isForbidden());
        }

    }
}
