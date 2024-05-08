package uk.badamson.mc.rest;
/*
 * © Copyright Benedict Adamson 2020-24.
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.MongoDBContainer;
import uk.badamson.mc.*;
import uk.badamson.mc.presentation.GameController;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.*;

public class GameRestIT {
    private static final String MONGO_DB_PASSWORD = "LetMeIn1";
    private static final String ADMINISTRATOR_PASSWORD = ProcessFixtures.ADMINISTRATOR.getPassword();

    private static MongoDBContainer mongoDBContainer;
    private static McBackEndProcess mcBackEndProcess;
    private static McBackEndClient mcBackEndClient;

    @BeforeAll
    public static void setUp() throws TimeoutException {
        mongoDBContainer = new MongoDBContainer(ProcessFixtures.MONGO_DB_IMAGE);
        mongoDBContainer.start();
        final var mongoDBPath = mongoDBContainer.getReplicaSetUrl();
        mcBackEndProcess = new McBackEndProcess(mongoDBPath, MONGO_DB_PASSWORD, ADMINISTRATOR_PASSWORD);
        mcBackEndClient = new McBackEndClient(
                "localhost", mcBackEndProcess.getServerPort(), ADMINISTRATOR_PASSWORD
        );
    }

    @AfterAll
    public static void tearDown() {
        mcBackEndProcess.close();
        mongoDBContainer.close();
    }

    @Nonnull
    private UUID createGame() {
        return mcBackEndClient.createGame(getAScenarioId());
    }

    @Nonnull
    private UUID getAScenarioId() {
        return mcBackEndClient.getScenarios().findAny().orElseThrow().getId();
    }

    private void userJoinsGame(
            @Nonnull UUID gameId,
            @Nonnull BasicUserDetails user,
            @Nonnull MultiValueMap<String, HttpCookie> cookies
    ) {
        final var response = mcBackEndClient.joinGame(gameId, user, cookies, true, true, true);
        // Client will have followed the redirect.
        response.expectStatus().isFound();
    }

    private void endRecruitment(UUID gameId) {
        final var cookies = mcBackEndClient.login(ProcessFixtures.ADMINISTRATOR);
        try {
            mcBackEndClient.endRecruitment(gameId, ProcessFixtures.ADMINISTRATOR, cookies, true, true, true);
        } finally {
            mcBackEndClient.logout(ProcessFixtures.ADMINISTRATOR, cookies);
        }
    }

    /**
     * Tests {@link GameController#createGameForScenario(UUID)}
     */
    @Nested
    public class CreateGameForScenario {
        @Test
        public void noAuthentication() {
            final var scenarioId = getAScenarioId();

            final var response = test(scenarioId, ProcessFixtures.ADMINISTRATOR, false, false, true);

            response.expectStatus().isUnauthorized();
        }

        @Test
        public void noCsrfToken() {
            final var scenarioId = getAScenarioId();

            final var response = test(scenarioId, ProcessFixtures.ADMINISTRATOR, false, true, false);

            response.expectStatus().isForbidden();
        }

        @Test
        public void insufficientAuthority() {
            final var scenarioId = getAScenarioId();
            final var authorities = EnumSet.complementOf(EnumSet.of(Authority.ROLE_MANAGE_GAMES));
            final var user = ProcessFixtures.createBasicUserDetailsWithAuthorities(authorities);
            mcBackEndClient.addUser(user);

            final var response = test(scenarioId, user, true, true, true);

            response.expectStatus().isForbidden();
        }

        @Test
        public void unknownScenario() {
            final var scenarioId = UUID.randomUUID();

            final var response = test(scenarioId, ProcessFixtures.ADMINISTRATOR, true, true, true);

            response.expectStatus().isNotFound();
        }

        @Nonnull
        private WebTestClient.ResponseSpec test(
                @Nonnull final UUID scenarioId,
                @Nonnull final BasicUserDetails user,
                final boolean includeAuthentication,
                final boolean includeSessionCookie,
                final boolean includeXsrfToken) {
            final var cookies = mcBackEndClient.login(user);
            try {
                return mcBackEndClient.createGameForScenario(
                        scenarioId,
                        user,
                        cookies,
                        includeAuthentication, includeSessionCookie, includeXsrfToken
                );
            } finally {
                mcBackEndClient.logout(user, cookies);
            }
        }

        @Nested
        public class ValidRequest {
            @Test
            public void verboseByAdministrator() {
                test(ProcessFixtures.ADMINISTRATOR, true, true);
            }

            @Test
            public void verboseByNonAdministrator() {
                final var user = ProcessFixtures.createBasicUserDetailsWithManageGamesRole();
                mcBackEndClient.addUser(user);

                test(user, true, true);
            }

            @Test
            public void inSession() {
                test(ProcessFixtures.ADMINISTRATOR, false, true);
            }

            @Test
            public void withoutSession() {
                test(ProcessFixtures.ADMINISTRATOR, true, false);
            }

            private void test(
                    @Nonnull final BasicUserDetails user,
                    final boolean includeAuthentication,
                    final boolean includeSessionCookie
            ) {
                final var scenarioId = getAScenarioId();

                final var response = CreateGameForScenario.this.test(scenarioId, user, includeAuthentication, includeSessionCookie, true);

                response.expectStatus().isFound();
                response.expectHeader().exists("Location");
            }

        }

    }

    /**
     * Tests of {@link GameController#getGame(SpringUser, UUID)}
     */
    @Nested
    public class GetGame {

        @Test
        public void unknownGame() {
            final var id = UUID.randomUUID();
            /* Tough test: user is authorised. */
            final var response = test(id, ProcessFixtures.ADMINISTRATOR, true, true, true);

            response.expectStatus().isNotFound();
        }

        @Test
        public void noAuthentication() {
            /* Tough test: game exists. */
            final var id = createGame();
            final var response = test(id, ProcessFixtures.ADMINISTRATOR, false, false, false);

            response.expectStatus().isUnauthorized();
        }

        @Test
        public void insufficientAuthority() {
            /* Tough test: game exists */
            final var id = createGame();
            /* Tough test: user has all the other authorities */
            final Set<Authority> authorities = EnumSet.complementOf(
                    EnumSet.of(Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES)
            );
            final var user = ProcessFixtures.createBasicUserDetailsWithAuthorities(authorities);
            mcBackEndClient.addUser(user);

            final var response = test(id, user, true, true, true);

            response.expectStatus().isForbidden();
        }

        @Nonnull
        private WebTestClient.ResponseSpec test(
                @Nonnull final UUID gameId,
                @Nonnull final BasicUserDetails user,
                final boolean includeAuthentication,
                final boolean includeSessionCookie,
                final boolean includeXsrfToken) {
            final var cookies = mcBackEndClient.login(user);
            try {
                return mcBackEndClient.getGame(
                        gameId,
                        user, cookies,
                        includeAuthentication, includeSessionCookie, includeXsrfToken
                );
            } finally {
                mcBackEndClient.logout(user, cookies);
            }
        }

        @Nested
        public class ValidRequest {

            @Test
            public void asGamesManager() {
                test(Authority.ROLE_MANAGE_GAMES, true, true);
            }

            @Test
            public void asPlayer() {
                test(Authority.ROLE_PLAYER, true, true);
            }

            @Test
            public void withAuthentication() {
                test(Authority.ROLE_MANAGE_GAMES, true, false);
            }

            @Test
            public void inSession() {
                test(Authority.ROLE_MANAGE_GAMES, false, true);
            }

            private void test(
                    @Nonnull final Authority authority,
                    final boolean includeAuthentication,
                    final boolean includeSessionCookie
            ) {
                final var id = createGame();
                final var user = ProcessFixtures.createBasicUserDetailsWithAuthorities(EnumSet.of(authority));
                mcBackEndClient.addUser(user);

                final var response = GetGame.this.test(id, user, includeAuthentication, includeSessionCookie, true);

                response.expectStatus().isOk();
                response.expectBody(GameResponse.class)
                        .value(GameResponse::identifier, is(id));
            }

        }

    }

    /**
     * Tests {@link GameController#getGameIdentifiersOfScenario(UUID)}
     */
    @Nested
    public class GetGameIdentifiersOfScenario {

        @Test
        public void unknownScenario() {
            final var scenario = UUID.randomUUID();

            final var response = test(scenario, ProcessFixtures.ADMINISTRATOR, true, true, true);

            response.expectStatus().isNotFound();
        }

        @Test
        public void noAuthentication() {
            // Tough test: scenario exists
            final var scenario = getAScenarioId();

            final var response = test(scenario, ProcessFixtures.ADMINISTRATOR, false, false, false);

            response.expectStatus().isUnauthorized();
        }

        @Test
        public void insufficientAuthority() {
            // Tough test: game exists, user has all other authorities
            final var scenario = getAScenarioId();
            final var authorities = EnumSet.complementOf(EnumSet
                    .of(Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES));
            final var user = ProcessFixtures.createBasicUserDetailsWithAuthorities(authorities);
            mcBackEndClient.addUser(user);

            final var response = test(scenario, user, true, true, true);

            response.expectStatus().isForbidden();
        }

        @Nonnull
        private WebTestClient.ResponseSpec test(
                @Nonnull final UUID scenarioId,
                @Nonnull final BasicUserDetails user,
                final boolean includeAuthentication,
                final boolean includeSessionCookie,
                final boolean includeXsrfToken) {
            final var cookies = mcBackEndClient.login(user);
            try {
                return mcBackEndClient.getGamesOfScenario(
                        scenarioId,
                        user, cookies,
                        includeAuthentication, includeSessionCookie, includeXsrfToken
                );
            } finally {
                mcBackEndClient.logout(user, cookies);
            }
        }

        @Nested
        public class ValidRequest {

            @Test
            public void asAdministrator() {
                test(ProcessFixtures.ADMINISTRATOR, true, true, true);
            }

            @Test
            public void asPlayer() {
                final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
                mcBackEndClient.addUser(user);

                test(user, true, true, true);
            }

            @Test
            public void asManageGamesRole() {
                final var user = ProcessFixtures.createBasicUserDetailsWithManageGamesRole();
                mcBackEndClient.addUser(user);

                test(user, true, true, true);
            }

            @Test
            public void withAuthentication() {
                test(ProcessFixtures.ADMINISTRATOR, true, false, false);
            }

            @Test
            public void withSession() {
                test(ProcessFixtures.ADMINISTRATOR, false, true, true);
            }

            private void test(BasicUserDetails user,
                              final boolean includeAuthentication,
                              final boolean includeSessionCookie,
                              final boolean includeXsrfToken) {
                final var scenarioId = getAScenarioId();
                final var gameId = mcBackEndClient.createGame(scenarioId);
                createGame();

                final var response = GetGameIdentifiersOfScenario.this.test(scenarioId, user, includeAuthentication, includeSessionCookie, includeXsrfToken);

                response.expectStatus().isOk();
                response.expectBody(new ParameterizedTypeReference<List<NamedUUID>>() {
                        })
                        .value(notNullValue())
                        .value(not(empty()))
                        .value(l -> l.stream().filter(g -> gameId.equals(g.getId())).count(), is(1L));
            }
        }

    }

    /**
     * Tests {@link GameController#endRecruitment(UUID)}
     */
    @Nested
    public class EndRecruitment {

        @Test
        public void unknownGame() {
            final var gameId = UUID.randomUUID();
            // Tough test: user has authority

            final var response = test(gameId, ProcessFixtures.ADMINISTRATOR, true, true, true);

            response.expectStatus().isNotFound();
        }

        @Test
        public void noAuthentication() {
            // Tough test: game exists and CSRF token provided
            final var gameId = createGame();

            final var response = test(gameId, ProcessFixtures.ADMINISTRATOR, false, false, false);

            response.expectStatus().isForbidden();
        }

        @Test
        public void noCsrfToken() {
            final var gameId = createGame();

            final var response = test(gameId, ProcessFixtures.ADMINISTRATOR, true, true, false);

            response.expectStatus().isForbidden();
        }

        @Test
        public void insufficientAuthority() {
            // Tough test: game exists, user has all other authorities, and CSRF
            // token provided
            final var gameId = createGame();
            final var authorities = EnumSet.complementOf(EnumSet
                    .of(Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES));
            final var user = ProcessFixtures.createBasicUserDetailsWithAuthorities(authorities);
            mcBackEndClient.addUser(user);

            final var response = test(gameId, user, true, true, false);

            response.expectStatus().isForbidden();
        }

        @Nonnull
        private WebTestClient.ResponseSpec test(
                @Nonnull final UUID gameId,
                @Nonnull final BasicUserDetails user,
                final boolean includeAuthentication,
                final boolean includeSessionCookie,
                final boolean includeXsrfToken) {
            final var cookies = mcBackEndClient.login(user);
            try {
                return mcBackEndClient.endRecruitment(gameId, user, cookies, includeAuthentication, includeSessionCookie, includeXsrfToken);
            } finally {
                mcBackEndClient.logout(user, cookies);
            }
        }

        @Nested
        public class ValidRequest {

            @Test
            public void administrator() {
                test(ProcessFixtures.ADMINISTRATOR, true, true);
            }

            @Test
            public void manageGamesRole() {
                final var user = ProcessFixtures.createBasicUserDetailsWithManageGamesRole();
                mcBackEndClient.addUser(user);

                test(user, true, true);
            }

            @Test
            public void withAuthentication() {
                test(ProcessFixtures.ADMINISTRATOR, true, false);
            }

            @Test
            public void inSession() {
                test(ProcessFixtures.ADMINISTRATOR, false, true);
            }

            private void test(
                    final BasicUserDetails user,
                    final boolean includeAuthentication,
                    final boolean includeSessionCookie
            ) {
                final var gameId = createGame();

                final WebTestClient.ResponseSpec response;
                final var cookies = mcBackEndClient.login(user);
                try {
                    response = mcBackEndClient.endRecruitment(gameId, user, cookies, includeAuthentication, includeSessionCookie, true);
                } finally {
                    mcBackEndClient.logout(user, cookies);
                }

                response.expectStatus().isFound();
                response.expectHeader().location(Paths.createPathForGame(gameId));
            }
        }

    }

    /**
     * Tests {@link GameController#getCurrentGame(SpringUser)}.
     */
    @Nested
    public class GetCurrentGame {

        @Test
        public void hasCurrentGame() {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            mcBackEndClient.addUser(user);
            final var cookies = mcBackEndClient.login(user);

            final WebTestClient.ResponseSpec response;
            try {
                userJoinsGame(gameId, user, cookies);
                response = mcBackEndClient.getCurrentGame(
                        user, cookies,
                        true, true, true
                );
            } finally {
                mcBackEndClient.logout(user, cookies);
            }

            response.expectStatus().isTemporaryRedirect();
            response.expectHeader().location(Paths.createPathForGame(gameId));
        }

        @Test
        public void noAuthentication() {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            mcBackEndClient.addUser(user);
            final var cookies = mcBackEndClient.login(user);

            final WebTestClient.ResponseSpec response;
            try {
                userJoinsGame(gameId, user, cookies);
                response = mcBackEndClient.getCurrentGame(
                        user, cookies,
                        false, false, true
                );
            } finally {
                mcBackEndClient.logout(user, cookies);
            }

            /*
             * Must return Not Found rather than Unauthorized, because otherwise
             * web browsers will pop up an authentication dialogue
             */
            response.expectStatus().isNotFound();
        }

        @Test
        public void noCsrf() {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            mcBackEndClient.addUser(user);
            final var cookies = mcBackEndClient.login(user);

            final WebTestClient.ResponseSpec response;
            try {
                userJoinsGame(gameId, user, cookies);
                response = mcBackEndClient.getCurrentGame(
                        user, cookies,
                        false, false, false
                );
            } finally {
                mcBackEndClient.logout(user, cookies);
            }

            /*
             * Must return Not Found rather than Unauthorized, because otherwise
             * web browsers will pop up an authentication dialogue
             */
            response.expectStatus().isNotFound();
        }

        @Test
        public void noCurrentGame() {
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            mcBackEndClient.addUser(user);
            final var cookies = mcBackEndClient.login(user);
            final WebTestClient.ResponseSpec response;
            try {
                response = mcBackEndClient.getCurrentGame(
                        user, cookies,
                        true, true, false
                );
            } finally {
                mcBackEndClient.logout(user, cookies);
            }

            /*
             * Must return Not Found rather than Unauthorized, because otherwise
             * web browsers will pop up an authentication dialogue
             */
            response.expectStatus().isNotFound();
        }

    }

    /**
     * Tests {@link GameController#joinGame(SpringUser, UUID)}
     */
    @Nested
    public class JoinGame {

        @Test
        public void unknownGame() {
            final var gameId = UUID.randomUUID();

            final var response = test(gameId, ProcessFixtures.ADMINISTRATOR, true, true, true);

            response.expectStatus().isNotFound();
        }

        @Test
        public void noAuthentication() {
            // Tough test: game exists and CSRF token provided
            final var gameId = createGame();

            final var response = test(gameId, ProcessFixtures.ADMINISTRATOR, false, false, true);

            response.expectStatus().isUnauthorized();
        }

        @Test
        public void noCsrfToken() {
            // Tough test: game exists and user has all authorities
            final var gameId = createGame();

            final var response = test(gameId, ProcessFixtures.ADMINISTRATOR, false, false, false);

            response.expectStatus().isForbidden();
        }

        @Test
        public void insufficientAuthority() {
            /*
             * Tough test: game exists, user has all other authorities, and CSRF
             * token provided
             */
            final var gameId = createGame();
            final Set<Authority> authorities = EnumSet.complementOf(EnumSet.of(Authority.ROLE_PLAYER));
            final var user = ProcessFixtures.createBasicUserDetailsWithAuthorities(authorities);
            mcBackEndClient.addUser(user);

            final var response = test(gameId, user, true, true, true);

            response.expectStatus().isForbidden();
        }

        @Test
        public void notRecruiting() {
            /*
             * Tough test: game exists, user has all authorities, and CSRF token
             * provided
             */
            final var gameId = createGame();
            endRecruitment(gameId);

            final var response = test(gameId, ProcessFixtures.ADMINISTRATOR, true, true, true);

            response.expectStatus().isEqualTo(HttpStatus.CONFLICT);
        }

        @Nonnull
        private WebTestClient.ResponseSpec test(
                @Nonnull final UUID gameId,
                @Nonnull final BasicUserDetails user,
                final boolean includeAuthentication,
                final boolean includeSessionCookie,
                final boolean includeXsrfToken) {
            final var cookies = mcBackEndClient.login(user);
            try {
                return mcBackEndClient.joinGame(
                        gameId,
                        user, cookies,
                        includeAuthentication, includeSessionCookie, includeXsrfToken
                );
            } finally {
                mcBackEndClient.logout(user, cookies);
            }
        }

        @Test
        public void playingOtherGame() {
            /*
             * Tough test: game exists, user has all authorities, and CSRF token
             * provided
             */
            final var gameIdA = createGame();
            final var gameIdB = createGame();
            assert !gameIdA.equals(gameIdB);
            final var cookies = mcBackEndClient.login(ProcessFixtures.ADMINISTRATOR);
            final WebTestClient.ResponseSpec response;
            try {
                userJoinsGame(gameIdB, ProcessFixtures.ADMINISTRATOR, cookies);

                response = mcBackEndClient.joinGame(
                        gameIdA,
                        ProcessFixtures.ADMINISTRATOR, cookies,
                        true, true, true
                );
            } finally {
                mcBackEndClient.logout(ProcessFixtures.ADMINISTRATOR, cookies);
            }
            response.expectStatus().isEqualTo(HttpStatus.CONFLICT);
        }

        @Nested
        public class ValidRequest {
            @Test
            public void verbose() {
                test(true, true);
            }

            @Test
            public void withoutSession() {
                test(true, false);
            }

            @Test
            public void inSession() {
                test(false, true);
            }

            private void test(
                    final boolean includeAuthentication,
                    final boolean includeSessionCookie
            ) {
                assert includeAuthentication || includeSessionCookie;
                final var gameId = createGame();
                // Tough test: user has a minimum set of authorities
                final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
                mcBackEndClient.addUser(user);

                final var response = JoinGame.this.test(gameId, user, includeAuthentication, includeSessionCookie, true);

                response.expectStatus().isFound();
                response.expectHeader().location(Paths.createPathForGame(gameId));
            }

        }

    }

    /**
     * Tests {@link GameController#mayJoinGame(SpringUser, UUID)}.
     */
    @Nested
    public class MayJoinGame {

        @Test
        public void may() {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            mcBackEndClient.addUser(user);

            final var response = test(gameId, user, true, true, true);

            response.expectStatus().isOk();
            response.expectBody(Boolean.class).isEqualTo(true);
        }

        @Test
        public void noCsrfToken() {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            mcBackEndClient.addUser(user);

            final var response = test(gameId, user, true, true, false);

            response.expectStatus().isOk();
            response.expectBody(Boolean.class).isEqualTo(true);
        }

        @Test
        public void noAuthentication() {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            mcBackEndClient.addUser(user);

            final var response = test(gameId, user, false, false, true);

            response.expectStatus().isUnauthorized();
        }

        @Test
        public void recruitmentEnded() {
            final var gameId = createGame();
            endRecruitment(gameId);
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            mcBackEndClient.addUser(user);

            final var response = test(gameId, user, true, true, true);

            response.expectStatus().isOk();
            response.expectBody(Boolean.class).isEqualTo(false);
        }

        @Nonnull
        private WebTestClient.ResponseSpec test(
                @Nonnull final UUID gameId,
                @Nonnull final BasicUserDetails user,
                final boolean includeAuthentication,
                final boolean includeSessionCookie,
                final boolean includeXsrfToken) {
            final var cookies = mcBackEndClient.login(user);
            try {
                return mcBackEndClient.mayJoin(
                        gameId,
                        user, cookies,
                        includeAuthentication, includeSessionCookie, includeXsrfToken
                );
            } finally {
                mcBackEndClient.logout(user, cookies);
            }
        }


        @Test
        public void unknownGame() {
            final var gameId = UUID.randomUUID();

            final var response = test(gameId, ProcessFixtures.ADMINISTRATOR, true, true, true);

            response.expectStatus().isNotFound();
        }

        @Test
        public void insufficientAuthority() {
            final var gameId = UUID.randomUUID();
            final var authorities = EnumSet.complementOf(EnumSet.of(Authority.ROLE_PLAYER));
            final var user = ProcessFixtures.createBasicUserDetailsWithAuthorities(authorities);
            mcBackEndClient.addUser(user);

            final var response = test(gameId, user, true, true, true);

            response.expectStatus().isForbidden();
        }

    }

    /**
     * Tests {@link GameController#startGame(SpringUser, UUID)}
     */
    @Nested
    public class StartGame {
        @Test
        public void validRequest() {
            final var gameId = createGame();
            final var user = ProcessFixtures.createBasicUserDetailsWithManageGamesRole();
            mcBackEndClient.addUser(user);

            final var response = testAuthenticated(gameId, user);

            response.expectStatus().isFound();
            response.expectHeader().location(Paths.createPathForGame(gameId));
        }

        @Test
        public void noAuthentication() {
            final var gameId = createGame();
            final var cookies = mcBackEndClient.login(ProcessFixtures.ADMINISTRATOR);
            final WebTestClient.ResponseSpec response;
            try {
                response = mcBackEndClient.startGame(gameId, ProcessFixtures.ADMINISTRATOR, cookies, false, false, true);
            } finally {
                mcBackEndClient.logout(ProcessFixtures.ADMINISTRATOR, cookies);
            }
            response.expectStatus().isUnauthorized();
        }

        @Test
        public void noCsrfToken() {
            final var gameId = createGame();
            final var cookies = mcBackEndClient.login(ProcessFixtures.ADMINISTRATOR);
            final WebTestClient.ResponseSpec response;
            try {
                response = mcBackEndClient.startGame(gameId, ProcessFixtures.ADMINISTRATOR, cookies, true, true, false);
            } finally {
                mcBackEndClient.logout(ProcessFixtures.ADMINISTRATOR, cookies);
            }
            response.expectStatus().isForbidden();
        }

        @Test
        public void insufficientAuthority() {
            final var gameId = createGame();
            Set<Authority> authorities = EnumSet.complementOf(EnumSet.of(Authority.ROLE_MANAGE_GAMES));
            final var user = ProcessFixtures.createBasicUserDetailsWithAuthorities(authorities);
            mcBackEndClient.addUser(user);
            final var cookies = mcBackEndClient.login(user);
            final WebTestClient.ResponseSpec response;
            try {
                response = mcBackEndClient.startGame(gameId, user, cookies, true, true, true);
            } finally {
                mcBackEndClient.logout(user, cookies);
            }
            response.expectStatus().isForbidden();
        }

        private WebTestClient.ResponseSpec testAuthenticated(
                final UUID gameId,
                final BasicUserDetails user) {
            final var cookies = mcBackEndClient.login(user);
            try {
                return mcBackEndClient.startGame(gameId, user, cookies, true, true, true);
            } finally {
                mcBackEndClient.logout(user, cookies);
            }
        }

        @Test
        public void unknownGame() {
            final var gameId = UUID.randomUUID();

            final var response = testAuthenticated(gameId, ProcessFixtures.ADMINISTRATOR);

            response.expectStatus().isNotFound();
        }

    }

    /**
     * Tests {@link GameController#stopGame(SpringUser, UUID)}
     */
    @Nested
    public class StopGame {
        @Test
        public void validRequest() {
            final var gameId = createGame();

            final var response = test(EnumSet.of(Authority.ROLE_MANAGE_GAMES), gameId, true, true, true);

            response.expectStatus().isFound();
            response.expectHeader().location(Paths.createPathForGame(gameId));
        }

        @Test
        public void noAuthentication() {
            final var gameId = createGame();

            final var response = test(EnumSet.of(Authority.ROLE_MANAGE_GAMES), gameId, false, false, true);

            response.expectStatus().isUnauthorized();
        }

        @Test
        public void noCsrfToken() {
            final var gameId = createGame();

            final var response = test(EnumSet.of(Authority.ROLE_MANAGE_GAMES), gameId, true, true, false);

            response.expectStatus().isForbidden();
        }

        @Test
        public void insufficientAuthority() {
            final var gameId = createGame();

            final var response = test(EnumSet.complementOf(EnumSet.of(Authority.ROLE_MANAGE_GAMES)), gameId, true, true, true);

            response.expectStatus().isForbidden();
        }

        @Test
        public void unknownGame() {
            final var gameId = UUID.randomUUID();

            final var response = test(EnumSet.of(Authority.ROLE_MANAGE_GAMES), gameId, true, true, true);

            response.expectStatus().isNotFound();
        }

        private WebTestClient.ResponseSpec test(
                @Nonnull final Set<Authority> authorities,
                @Nonnull final UUID gameId,
                final boolean includeAuthentication,
                final boolean includeSessionCookie,
                final boolean includeXsrfToken
        ) {
            final var user = ProcessFixtures.createBasicUserDetailsWithAuthorities(authorities);
            mcBackEndClient.addUser(user);
            final var cookies = mcBackEndClient.login(user);
            try {
                mcBackEndClient.startGame(gameId, user, cookies, true, true, true);
                return mcBackEndClient.stopGame(gameId, user, cookies, includeAuthentication, includeSessionCookie, includeXsrfToken);
            } finally {
                mcBackEndClient.logout(user, cookies);
            }
        }
    }

}
