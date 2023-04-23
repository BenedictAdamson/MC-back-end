package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2020-23.
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.badamson.mc.*;
import uk.badamson.mc.rest.GameResponse;
import uk.badamson.mc.rest.NamedUUID;
import uk.badamson.mc.rest.Paths;
import uk.badamson.mc.service.GameSpringService;
import uk.badamson.mc.service.ScenarioSpringService;
import uk.badamson.mc.service.UserSpringService;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
public class GameRESTTest {

    private static final TypeReference<List<NamedUUID>> GAME_ID_LIST = new TypeReference<>() {
    };

    private static final User USER_WITH_ALL_AUTHORITIES = new User(
            UUID.randomUUID(), "jeff", "password", Authority.ALL,
            true, true, true, true);

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(Fixtures.MONGO_DB_IMAGE);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

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

    @Nonnull
    private static <T> Set<T> asSet(@Nonnull Iterable<T> values) {
        final Set<T> result = new HashSet<>();
        for (final var value : values) {
            result.add(value);
        }
        return result;
    }

    @Nonnull
    private static <T> Set<T> difference(@Nonnull Set<T> u, @Nonnull Set<T> a) {
        final Set<T> result = new HashSet<>(u);
        result.removeAll(a);
        return result;
    }

    @Nullable
    private static String getLocationHeaderValue(@Nonnull ResultActions response) {
        return (String) response.andReturn().getResponse().getHeaderValue("Location");
    }

    private static void assertRedirectsToTheLocationOfTheGame(ResultActions response, UUID gameId) {
        assertThat("redirects to the location of the game",
                getLocationHeaderValue(response),
                is(Paths.createPathForGame(gameId)));
    }

    @Nonnull
    private UUID createGame() {
        return gameService.create(getAScenarioId()).getIdentifier();
    }

    private User createUser(final Set<Authority> authorities) {
        return userService.add(new BasicUserDetails(Fixtures.createUserName(), "password",
                authorities, true, true, true, true));
    }

    private UUID getAScenarioId() {
        return scenarioService.getScenarioIdentifiers().findAny().orElseThrow();
    }

    /**
     * Tests {@link GameController#createGameForScenario(java.util.UUID)}
     */
    @Nested
    public class CreateGameForScenario {
        @Test
        public void validRequest() throws Exception {
            final var scenarioId = getAScenarioId();
            final Set<UUID> gameIds0 = asSet(gameService.getGameIdentifiers());

            final var response = createGameForScenario(scenarioId, USER_WITH_ALL_AUTHORITIES, true);

            final Set<UUID> gameIds = asSet(gameService.getGameIdentifiers());
            final var gameIdsAdded = difference(gameIds, gameIds0);
            assertThat("created one game", gameIdsAdded, hasSize(1));
            final var gameIdAdded = gameIdsAdded.iterator().next();
            assertAll(
                    () -> response.andExpect(status().isFound()),
                    () -> assertRedirectsToTheLocationOfTheGame(response, gameIdAdded));
        }

        @Test
        public void noAuthentication() throws Exception {
            final var scenarioId = getAScenarioId();

            final var response = createGameForScenario(scenarioId, null, true);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void noCsrfToken() throws Exception {
            final var scenarioId = getAScenarioId();

            final var response = createGameForScenario(scenarioId, USER_WITH_ALL_AUTHORITIES, false);

            response.andExpect(status().isForbidden());
        }

        @Test
        public void insufficientAuthority() throws Exception {
            final var scenarioId = getAScenarioId();
            final var authorities = EnumSet.complementOf(EnumSet.of(Authority.ROLE_MANAGE_GAMES));
            final var user = createUser(authorities);

            final var response = createGameForScenario(scenarioId, user, true);

            response.andExpect(status().isForbidden());
        }

        @Test
        public void unknownScenario() throws Exception {
            final var scenarioId = UUID.randomUUID();

            final var response = createGameForScenario(scenarioId, USER_WITH_ALL_AUTHORITIES, true);

            response.andExpect(status().isNotFound());
        }

        @Nonnull
        private ResultActions createGameForScenario(
                @Nonnull final UUID scenarioId,
                @Nullable final User user,
                final boolean hasCsrf) throws Exception {
            var request = post(Paths.createPathForGamesOfScenario(scenarioId));
            if (user != null) {
                request = request.with(user(SpringUser.convertToSpring(user)));
            }
            if (hasCsrf) {
                request = request.with(csrf());
            }

            return mockMvc.perform(request);
        }

    }

    /**
     * Tests of {@link GameController#getGame(SpringUser, UUID)}
     */
    @Nested
    public class GetGame {

        @Test
        public void unknownGame() throws Exception {
            final var id = UUID.randomUUID();
            /* Tough test: user is authorised. */
            final var response = getGame(id, USER_WITH_ALL_AUTHORITIES);

            response.andExpect(status().isNotFound());
        }

        @Test
        public void noAuthentication() throws Exception {
            /* Tough test: game exists. */
            final var id = createGame();

            final var response = getGame(id, null);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void insufficientAuthority() throws Exception {
            /* Tough test: game exists and user has all the other authorities */
            final Set<Authority> authorities = EnumSet.complementOf(EnumSet
                    .of(Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES));
            final var user = createUser(authorities);
            final var id = createGame();

            final var response = getGame(id, user);

            response.andExpect(status().isForbidden());
        }

        @Nonnull
        private ResultActions getGame(
                @Nonnull final UUID id,
                @Nullable final User user)
                throws Exception {
            final var request = get(Paths.createPathForGame(id))
                    .accept(MediaType.APPLICATION_JSON);
            if (user != null) {
                request.with(user(SpringUser.convertToSpring(user)));
            }

            return mockMvc.perform(request);
        }

        @Nested
        public class ValidRequest {

            @Test
            public void asGamesManager() throws Exception {
                testWithAuthority(Authority.ROLE_MANAGE_GAMES);
            }

            @Test
            public void asPlayer() throws Exception {
                testWithAuthority(Authority.ROLE_PLAYER);
            }

            private void testWithAuthority(
                    @Nonnull final Authority authority)
                    throws Exception {
                final var scenarioId = getAScenarioId();
                final var identifiedGame = gameService.create(scenarioId);
                final var gameId = identifiedGame.getIdentifier();
                final var created = identifiedGame.getValue().getCreated();
                final var user = createUser(EnumSet.of(authority));

                final var response = getGame(gameId, user);

                final var responseBodyText = response.andReturn().getResponse().getContentAsString();
                final GameResponse gameResponse = objectMapper.readValue(responseBodyText, GameResponse.class);
                assertAll(
                        () -> response.andExpect(status().isOk()),
                        () -> assertThat("scenario", gameResponse.scenario(), is(scenarioId)),
                        () -> assertThat("created", gameResponse.created(), is(created)));
            }

        }

    }

    /**
     * Tests {@link GameController#getGameIdentifiersOfScenario(UUID)}
     */
    @Nested
    public class GetGameIdentifiersOfScenario {

        @Test
        public void unknownScenario() throws Exception {
            final var scenario = UUID.randomUUID();
            // Tough test: user has authority
            final var user = createUser(Authority.ALL);

            final var response = getGamesOfScenario(scenario, user);

            response.andExpect(status().isNotFound());
        }

        @Test
        public void noAuthentication() throws Exception {
            // Tough test: scenario exists
            final var scenario = getAScenarioId();

            final var response = getGamesOfScenario(scenario, null);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void insufficientAuthority() throws Exception {
            // Tough test: game exists, user has all other authorities
            final var scenario = getAScenarioId();
            final var authorities = EnumSet.complementOf(EnumSet
                    .of(Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES));
            final var user = createUser(authorities);

            final var response = getGamesOfScenario(scenario, user);

            response.andExpect(status().isForbidden());
        }

        @Test
        public void validRequest() throws Exception {
            // Tough test: user has minimum authority
            final var user = createUser(EnumSet.of(Authority.ROLE_PLAYER));
            final var scenario = getAScenarioId();
            final var existingIdentifiedGame = gameService.create(scenario);
            final var idOfExistingGame = existingIdentifiedGame.getIdentifier();
            final Game existingGame = existingIdentifiedGame.getValue();
            final var labelOfExistingGame = existingGame.getCreated().toString();
            final var expectedNamedId = new NamedUUID(idOfExistingGame, labelOfExistingGame);

            final var response = getGamesOfScenario(scenario, user);

            response.andExpect(status().isOk());
            final var responseBodyText = response.andReturn().getResponse().getContentAsString();
            final List<NamedUUID> gameIds = objectMapper.readValue(responseBodyText, GAME_ID_LIST);
            assertThat(gameIds, hasItem(expectedNamedId));
        }

        @Nonnull
        private ResultActions getGamesOfScenario(
                @Nonnull final UUID scenario,
                @Nullable final User user) throws Exception {
            final var path = Paths.createPathForGamesOfScenario(scenario);
            var request = get(path).accept(MediaType.APPLICATION_JSON);
            if (user != null) {
                request = request.with(user(SpringUser.convertToSpring(user)));
            }

            return mockMvc.perform(request);
        }

    }

    /**
     * Tests {@link GameController#endRecruitment(UUID)}
     */
    @Nested
    public class EndRecruitment {

        @Test
        public void unknownGame() throws Exception {
            final var gameId = UUID.randomUUID();
            // Tough test: user has authority
            final var user = createUser(Authority.ALL);

            final var response = endRecruitment(gameId, user, true);

            response.andExpect(status().isNotFound());
        }

        @Test
        public void noAuthentication() throws Exception {
            // Tough test: game exists and CSRF token provided
            final var gameId = createGame();

            final var response = endRecruitment(gameId, null, true);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void noCsrfToken() throws Exception {
            // Tough test: game exists and user has all authorities
            final var gameId = createGame();
            final var user = createUser(Authority.ALL);

            final var response = endRecruitment(gameId, user, false);

            response.andExpect(status().isForbidden());
        }

        @Test
        public void insufficientAuthority() throws Exception {
            // Tough test: game exists, user has all other authorities, and CSRF
            // token provided
            final var gameId = createGame();
            final var authorities = EnumSet.complementOf(EnumSet
                    .of(Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES));
            final var user = createUser(authorities);

            final var response = endRecruitment(gameId, user, true);

            response.andExpect(status().isForbidden());
        }

        @Test
        public void validRequest() throws Exception {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var authorities = EnumSet.of(Authority.ROLE_MANAGE_GAMES);
            final var user = createUser(authorities);

            final var response = endRecruitment(gameId, user, true);

            assertAll(
                    () -> response.andExpect(status().isFound()),
                    () -> assertRedirectsToTheLocationOfTheGame(response, gameId));
        }

        private ResultActions endRecruitment(
                final UUID gameId,
                final User user,
                final boolean hasCsrfToken) throws Exception {
            final var path = Paths.createPathForEndRecruitmentOfGame(gameId);
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

    /**
     * Tests {@link GameController#getCurrentGame(SpringUser)}.
     */
    @Nested
    public class GetCurrentGame {

        @Test
        public void hasCurrentGame() throws Exception {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = createUser(EnumSet.of(Authority.ROLE_PLAYER));
            gameService.userJoinsGame(user.getId(), gameId);

            final var response = getCurrentGame(user, true);

            assertAll(
                    () -> response.andExpect(status().isTemporaryRedirect()),
                    () -> assertRedirectsToTheLocationOfTheGame(response, gameId));
        }

        @Test
        public void noAuthentication() throws Exception {
            // Tough test: a game exists
            createGame();

            final var response = getCurrentGame(null, true);

            /*
             * Must return Not Found rather than Unauthorized, because otherwise
             * web browsers will pop up an authentication dialogue
             */
            response.andExpect(status().isNotFound());
        }

        @Test
        public void noCsrf() throws Exception {
            // Tough test: user has all authorities
            final var user = createUser(Authority.ALL);
            // Tough test: a game exists
            createGame();

            final var response = getCurrentGame(user, false);

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
            final var response = getCurrentGame(user, true);

            response.andExpect(status().isNotFound());
        }

        @Nonnull
        private ResultActions getCurrentGame(
                @Nullable final User user,
                boolean hasCsrf) throws Exception {
            var request = get(Paths.CURRENT_GAME_PATH);
            if (user != null) {
                request = request.with(user(SpringUser.convertToSpring(user)));
            }
            if (hasCsrf) {
                request = request.with(csrf());
            }

            return mockMvc.perform(request);
        }

    }

    /**
     * Tests {@link GameController#joinGame(SpringUser, UUID)}
     */
    @Nested
    public class JoinGame {

        @Test
        public void unknownGame() throws Exception {
            final var game = UUID.randomUUID();
            // Tough test: user has authority
            final var user = createUser(Authority.ALL);

            final var response = joinGame(game, user, true);

            response.andExpect(status().isNotFound());
        }

        @Test
        public void noAuthentication() throws Exception {
            // Tough test: game exists and CSRF token provided
            final var id = createGame();

            final var response = joinGame(id, null, true);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void noCsrfToken() throws Exception {
            // Tough test: game exists and user has all authorities
            final var gameId = createGame();
            final var user = createUser(Authority.ALL);

            final var response = joinGame(gameId, user, false);

            assertAll(
                    () -> response.andExpect(status().isForbidden()),
                    () -> assertGameStillExistAndUserIsNotPlayingIt(gameId, user));
        }

        private void assertGameStillExistAndUserIsNotPlayingIt(
                @Nonnull UUID gameId,
                @Nonnull User user) {
            final Optional<Game> gameOptional = gameService.getGameAsGameManager(gameId).map(FindGameResult::game);
            assertThat("game (still) exists", gameOptional.isPresent());
            final var gamePlayers = gameOptional.get();
            assertThat("User is not a player of the game",
                    gamePlayers.getUsers().values(), not(hasItem(user.getId())));
        }

        private void assertGameStillExistsAndUserIsPlayingIt(
                @Nonnull UUID gameId,
                @Nonnull User user) {
            final Optional<Game> gameOptional = gameService.getGameAsGameManager(gameId).map(FindGameResult::game);
            assertThat("game (still) exists", gameOptional.isPresent());
            final var gamePlayers = gameOptional.get();
            assertThat("User is a player of the game",
                    gamePlayers.getUsers().values(), hasItem(user.getId()));
        }

        @Test
        public void insufficientAuthority() throws Exception {
            /*
             * Tough test: game exists, user has all other authorities, and CSRF
             * token provided
             */
            final var gameId = createGame();
            final var authorities = EnumSet
                    .complementOf(EnumSet.of(Authority.ROLE_PLAYER));
            final var user = createUser(authorities);

            final var response = joinGame(gameId, user, true);

            assertAll(
                    () -> response.andExpect(status().isForbidden()),
                    () -> assertGameStillExistAndUserIsNotPlayingIt(gameId, user));
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

            final var response = joinGame(gameId, user, true);

            assertAll(
                    () -> response.andExpect(status().isConflict()),
                    () -> assertGameStillExistAndUserIsNotPlayingIt(gameId, user));
        }

        private ResultActions joinGame(final UUID game,
                                       final User user, final boolean hasCsrfToken) throws Exception {
            final var path = Paths.createPathForJoiningGame(game);
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
            final var gameIdB = createGame();
            assert !gameIdA.equals(gameIdB);
            final var user = createUser(Authority.ALL);
            gameService.userJoinsGame(user.getId(), gameIdA);

            final var response = joinGame(gameIdB, user, true);

            assertAll(
                    () -> response.andExpect(status().isConflict()),
                    () -> assertGameStillExistsAndUserIsPlayingIt(gameIdA, user),
                    () -> assertGameStillExistAndUserIsNotPlayingIt(gameIdB, user));
        }

        @Test
        public void validRequest() throws Exception {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = createUser(EnumSet.of(Authority.ROLE_PLAYER));

            final var response = joinGame(gameId, user, true);

            assertAll(
                    () -> response.andExpect(status().isFound()),
                    () -> assertGameStillExistsAndUserIsPlayingIt(gameId, user),
                    () -> assertRedirectsToTheLocationOfTheGame(response, gameId));
        }

    }

    /**
     * Tests {@link GameController#mayJoinGame(SpringUser, UUID)}.
     */
    @Nested
    public class MayJoinGame {

        private boolean expectValidResponseBody(@Nonnull final ResultActions response)
                throws Exception {
            response.andExpect(status().isOk());
            final var jsonResponse = response.andReturn().getResponse().getContentAsString();
            Boolean value = objectMapper.readValue(jsonResponse, Boolean.class);
            assertThat(value, notNullValue());
            return value;
        }

        @Test
        public void may() throws Exception {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = createUser(EnumSet.of(Authority.ROLE_PLAYER));

            final var response = mayJoin(gameId, user);

            final var may = expectValidResponseBody(response);
            assertThat(may, is(true));
        }

        @Test
        public void noAuthentication() throws Exception {
            // Tough test: game exists
            final var gameId = createGame();

            final var response = mayJoin(gameId, null);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void recruitmentEnded() throws Exception {
            final var gameId = createGame();
            gameService.endRecruitment(gameId);
            // Tough test: user has full authority
            final var user = createUser(Authority.ALL);

            final var response = mayJoin(gameId, user);

            final var may = expectValidResponseBody(response);
            assertThat(may, is(false));
        }

        @Nonnull
        private ResultActions mayJoin(
                @Nonnull final UUID gameId,
                @Nullable final User user)
                throws Exception {
            final var path = Paths.createPathForMayJoinQueryOfGame(gameId);
            var request = get(path).accept(MediaType.APPLICATION_JSON);
            if (user != null) {
                request = request.with(user(SpringUser.convertToSpring(user)));
            }

            return mockMvc.perform(request);
        }

        @Test
        public void unknownGame() throws Exception {
            final var gameId = UUID.randomUUID();
            // Tough test: user has all authorities
            final var user = createUser(Authority.ALL);

            final var response = mayJoin(gameId, user);

            response.andExpect(status().isNotFound());
        }

        @Test
        public void insufficientAuthority() throws Exception {
            // Tough test: game exists and user has all the other authorities
            final var gameId = createGame();
            final var authorities = EnumSet.complementOf(EnumSet.of(Authority.ROLE_PLAYER));
            final var user = createUser(authorities);

            final var response = mayJoin(gameId, user);

            response.andExpect(status().isForbidden());
        }

    }

    /**
     * Tests {@link GameController#startGame(SpringUser, UUID)}
     */
    @Nested
    public class StartGame {
        @Test
        public void validRequest() throws Exception {
            final var gameId = createGame();
            final var user = createUser(EnumSet.of(Authority.ROLE_MANAGE_GAMES));

            final var response = testAuthenticated(gameId, user);

            assertAll(
                    () -> response.andExpect(status().isFound()),
                    () -> assertRedirectsToTheLocationOfTheGame(response, gameId));
        }

        @Test
        public void noAuthentication() throws Exception {
            final var gameId = createGame();
            final var request = post(Paths.createPathForStartingGame(gameId)).with(csrf());

            final var response = mockMvc.perform(request);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void noCsrfToken() throws Exception {
            final var gameId = createGame();
            final var request = post(Paths.createPathForStartingGame(gameId))
                    .with(user(SpringUser.convertToSpring(USER_WITH_ALL_AUTHORITIES)));

            final var response = mockMvc.perform(request);

            response.andExpect(status().isForbidden());
        }

        @Test
        public void insufficientAuthority() throws Exception {
            final var gameId = createGame();
            final var user = createUser(EnumSet.complementOf(EnumSet.of(Authority.ROLE_MANAGE_GAMES)));

            final var response = testAuthenticated(gameId, user);

            response.andExpect(status().isForbidden());
        }

        private ResultActions testAuthenticated(
                final UUID game,
                final User user) throws Exception {
            final var request = post(Paths.createPathForStartingGame(game))
                    .with(user(SpringUser.convertToSpring(user))).with(csrf());

            return mockMvc.perform(request);
        }

        @Test
        public void unknownGame() throws Exception {
            final var gameId = UUID.randomUUID();

            final var response = testAuthenticated(gameId, USER_WITH_ALL_AUTHORITIES);

            response.andExpect(status().isNotFound());
        }

    }

    /**
     * Tests {@link GameController#stopGame(SpringUser, UUID)}
     */
    @Nested
    public class StopGame {
        @Test
        public void validRequest() throws Exception {
            final var gameId = createGame();
            gameService.startGame(gameId);
            final var user = createUser(EnumSet.of(Authority.ROLE_MANAGE_GAMES));

            final var response = stopGame(gameId, user);

            assertAll(
                    () -> response.andExpect(status().isFound()),
                    () -> assertRedirectsToTheLocationOfTheGame(response, gameId));
        }

        @Test
        public void noAuthentication() throws Exception {
            final var gameId = createGame();
            gameService.startGame(gameId);// Tough test: game is running
            final var request = post(Paths.createPathForStoppingGame(gameId)).with(csrf());

            final var response = mockMvc.perform(request);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void noCsrfToken() throws Exception {
            final var gameId = createGame();
            gameService.startGame(gameId);// Tough test: game is running
            final var request = post(Paths.createPathForStoppingGame(gameId))
                    .with(user(SpringUser.convertToSpring(USER_WITH_ALL_AUTHORITIES)));

            final var response = mockMvc.perform(request);

            response.andExpect(status().isForbidden());
        }

        @Test
        public void insufficientAuthority() throws Exception {
            final var gameId = createGame();
            final var user = createUser(EnumSet.complementOf(EnumSet.of(Authority.ROLE_MANAGE_GAMES)));

            final var response = stopGame(gameId, user);

            response.andExpect(status().isForbidden());
        }

        private ResultActions stopGame(
                final UUID game,
                final User user) throws Exception {
            final var request = post(Paths.createPathForStoppingGame(game))
                    .with(user(SpringUser.convertToSpring(user))).with(csrf());

            return mockMvc.perform(request);
        }

        @Test
        public void unknownGame() throws Exception {
            final var gameId = UUID.randomUUID();

            final var response = stopGame(gameId, USER_WITH_ALL_AUTHORITIES);

            response.andExpect(status().isNotFound());
        }

    }

}
