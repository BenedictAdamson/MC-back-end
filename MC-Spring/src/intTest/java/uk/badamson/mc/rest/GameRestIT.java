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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import uk.badamson.mc.*;
import uk.badamson.mc.presentation.GameController;
import uk.badamson.mc.presentation.SecurityConfiguration;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import java.util.*;

import static org.hamcrest.Matchers.*;

/**
 * Tests {@link SecurityConfiguration}.
 * Tests Spring annotations on {@link GameController}.
 */
public class GameRestIT extends RestIT {

    private static BasicUserDetails userWithManageGamesRole;
    private static BasicUserDetails userWithoutManageGamesRole;

    @BeforeAll
    public static void setupUsers() {
        userWithManageGamesRole = ProcessFixtures.createBasicUserDetailsWithManageGamesRole();
        userWithoutManageGamesRole = ProcessFixtures.createBasicUserDetailsWithAuthorities(
                EnumSet.complementOf(EnumSet.of(Authority.ROLE_MANAGE_GAMES))
        );
        addUser(userWithManageGamesRole);
        addUser(userWithoutManageGamesRole);
    }

    /**
     * Tests Spring annotations on {@link GameController#createGameForScenario(UUID)}
     */
    @Nested
    @SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "SpotBugs bug")
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

            final var response = test(scenarioId, userWithoutManageGamesRole, true, true, true);

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
            final var cookies = login(user);
            try {
                return getMcBackEndClient().createGameForScenario(
                        scenarioId,
                        includeAuthentication? user: null,
                        cookies,
                        includeSessionCookie, includeXsrfToken
                );
            } finally {
                logout(user, cookies);
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
                test(userWithManageGamesRole, true, true);
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
     * Tests Spring annotations on {@link GameController#getGame(SpringUser, UUID)}
     */
    @Nested
    @SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "SpotBugs bug")
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
            addUser(user);

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
            final var cookies = login(user);
            try {
                return getMcBackEndClient().getGame(
                        gameId,
                        includeAuthentication? user: null, cookies,
                        includeSessionCookie, includeXsrfToken
                );
            } finally {
                logout(user, cookies);
            }
        }

        @Nested
        public class ValidRequest {

            @Test
            public void asGamesManager() {
                test(userWithManageGamesRole, true, true);
            }

            @Test
            public void asPlayer() {
                final var user = ProcessFixtures.createBasicUserDetailsWithAuthorities(EnumSet.of(Authority.ROLE_PLAYER));
                addUser(user);
                test(user, true, true);
            }

            @Test
            public void withAuthentication() {
                test(userWithManageGamesRole, true, false);
            }

            @Test
            public void inSession() {
                test(userWithManageGamesRole, false, true);
            }

            private void test(
                    @Nonnull final BasicUserDetails user,
                    final boolean includeAuthentication,
                    final boolean includeSessionCookie
            ) {
                final var id = createGame();

                final var response = GetGame.this.test(id, user, includeAuthentication, includeSessionCookie, true);

                response.expectStatus().isOk();
                response.expectBody(GameResponse.class)
                        .value(GameResponse::identifier, is(id));
            }

        }

    }

    /**
     * Tests Spring annotations on {@link GameController#getGameIdentifiersOfScenario(UUID)}
     */
    @Nested
    @SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "SpotBugs bug")
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
            addUser(user);

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
            final var cookies = login(user);
            try {
                return getMcBackEndClient().getGamesOfScenario(
                        scenarioId,
                        includeAuthentication? user: null, cookies,
                        includeSessionCookie, includeXsrfToken
                );
            } finally {
                logout(user, cookies);
            }
        }

        @Nested
        @SuppressFBWarnings(value="SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "Required for JUnit 5")
        public class ValidRequest {

            @Test
            public void asAdministrator() {
                test(ProcessFixtures.ADMINISTRATOR, true, true, true);
            }

            @Test
            public void asPlayer() {
                final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
                addUser(user);

                test(user, true, true, true);
            }

            @Test
            public void asManageGamesRole() {
                test(userWithManageGamesRole, true, true, true);
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
                final var gameId = createGame(scenarioId);
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
     * Tests Spring annotations on {@link GameController#endRecruitment(UUID)}
     */
    @Nested
    @SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "SpotBugs bug")
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
            // Tough test: game exists, user has all other authorities, and session
            // token provided
            final var gameId = createGame();

            final var response = test(gameId, userWithoutManageGamesRole, true, true, false);

            response.expectStatus().isForbidden();
        }

        @Nonnull
        private WebTestClient.ResponseSpec test(
                @Nonnull final UUID gameId,
                @Nonnull final BasicUserDetails user,
                final boolean includeAuthentication,
                final boolean includeSessionCookie,
                final boolean includeXsrfToken) {
            final var cookies = login(user);
            try {
                return getMcBackEndClient().endRecruitment(gameId, includeAuthentication?user: null, cookies, includeSessionCookie, includeXsrfToken);
            } finally {
                logout(user, cookies);
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
                test(userWithManageGamesRole, true, true);
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
                final var cookies = login(user);
                try {
                    response = getMcBackEndClient().endRecruitment(gameId, includeAuthentication? user: null, cookies, includeSessionCookie, true);
                } finally {
                    logout(user, cookies);
                }

                response.expectStatus().isFound();
                response.expectHeader().location(Paths.createPathForGame(gameId));
            }
        }

    }

    /**
     * Tests Spring annotations on {@link GameController#getCurrentGame(SpringUser)}.
     */
    @Nested
    @SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "SpotBugs bug")
    public class GetCurrentGame {

        @Test
        public void hasCurrentGame() {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            addUser(user);
            final var cookies = login(user);

            final WebTestClient.ResponseSpec response;
            try {
                userJoinsGame(gameId, user, cookies);
                response = getMcBackEndClient().getCurrentGame(
                        user, cookies,
                        true, true
                );
            } finally {
                logout(user, cookies);
            }

            response.expectStatus().isTemporaryRedirect();
            response.expectHeader().location(Paths.createPathForGame(gameId));
        }

        @Test
        public void noAuthentication() {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            addUser(user);
            final var cookies = login(user);

            final WebTestClient.ResponseSpec response;
            try {
                userJoinsGame(gameId, user, cookies);
                response = getMcBackEndClient().getCurrentGame(
                        null, cookies,
                        false, true
                );
            } finally {
                logout(user, cookies);
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
            addUser(user);
            final var cookies = login(user);

            final WebTestClient.ResponseSpec response;
            try {
                userJoinsGame(gameId, user, cookies);
                response = getMcBackEndClient().getCurrentGame(
                        null, cookies,
                        false, false
                );
            } finally {
                logout(user, cookies);
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
            addUser(user);
            final var cookies = login(user);
            final WebTestClient.ResponseSpec response;
            try {
                response = getMcBackEndClient().getCurrentGame(
                        user, cookies,
                        true, false
                );
            } finally {
                logout(user, cookies);
            }

            /*
             * Must return Not Found rather than Unauthorized, because otherwise
             * web browsers will pop up an authentication dialogue
             */
            response.expectStatus().isNotFound();
        }

    }

    /**
     * Tests Spring annotations on {@link GameController#joinGame(SpringUser, UUID)}
     */
    @Nested
    @SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "SpotBugs bug")
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
            addUser(user);

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
            final var cookies = login(user);
            try {
                return getMcBackEndClient().joinGame(
                        gameId,
                        includeAuthentication? user: null, cookies,
                        includeSessionCookie, includeXsrfToken
                );
            } finally {
                logout(user, cookies);
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
            final var cookies = login(ProcessFixtures.ADMINISTRATOR);
            final WebTestClient.ResponseSpec response;
            try {
                userJoinsGame(gameIdB, ProcessFixtures.ADMINISTRATOR, cookies);

                response = getMcBackEndClient().joinGame(
                        gameIdA,
                        ProcessFixtures.ADMINISTRATOR, cookies,
                        true, true
                );
            } finally {
                logout(ProcessFixtures.ADMINISTRATOR, cookies);
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
                addUser(user);

                final var response = JoinGame.this.test(gameId, user, includeAuthentication, includeSessionCookie, true);

                response.expectStatus().isFound();
                response.expectHeader().location(Paths.createPathForGame(gameId));
            }

        }

    }

    /**
     * Tests Spring annotations on {@link GameController#mayJoinGame(SpringUser, UUID)}.
     */
    @Nested
    @SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "SpotBugs bug")
    public class MayJoinGame {

        @Test
        public void may() {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            addUser(user);

            final var response = test(gameId, user, true, true, true);

            response.expectStatus().isOk();
            response.expectBody(Boolean.class).isEqualTo(true);
        }

        @Test
        public void noCsrfToken() {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            addUser(user);

            final var response = test(gameId, user, true, true, false);

            response.expectStatus().isOk();
            response.expectBody(Boolean.class).isEqualTo(true);
        }

        @Test
        public void noAuthentication() {
            final var gameId = createGame();
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            addUser(user);

            final var response = test(gameId, user, false, false, true);

            response.expectStatus().isUnauthorized();
        }

        @Test
        public void recruitmentEnded() {
            final var gameId = createGame();
            endRecruitment(gameId);
            // Tough test: user has a minimum set of authorities
            final var user = ProcessFixtures.createBasicUserDetailsWithPlayerRole();
            addUser(user);

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
            final var cookies = login(user);
            try {
                return getMcBackEndClient().mayJoin(
                        gameId,
                        includeAuthentication? user: null, cookies,
                        includeSessionCookie, includeXsrfToken
                );
            } finally {
                logout(user, cookies);
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
            addUser(user);

            final var response = test(gameId, user, true, true, true);

            response.expectStatus().isForbidden();
        }

    }

    /**
     * Tests Spring annotations on {@link GameController#startGame(SpringUser, UUID)}
     */
    @Nested
    @SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "SpotBugs bug")
    public class StartGame {
        @Test
        public void validRequest() {
            final var gameId = createGame();

            final var response = testAuthenticated(gameId, userWithManageGamesRole);

            response.expectStatus().isFound();
            response.expectHeader().location(Paths.createPathForGame(gameId));
        }

        @Test
        public void noAuthentication() {
            final var gameId = createGame();
            final var cookies = login(ProcessFixtures.ADMINISTRATOR);
            final WebTestClient.ResponseSpec response;
            try {
                response = getMcBackEndClient().startGame(gameId, null, cookies, false, true);
            } finally {
                logout(ProcessFixtures.ADMINISTRATOR, cookies);
            }
            response.expectStatus().isUnauthorized();
        }

        @Test
        public void noCsrfToken() {
            final var gameId = createGame();
            final var cookies = login(ProcessFixtures.ADMINISTRATOR);
            final WebTestClient.ResponseSpec response;
            try {
                response = getMcBackEndClient().startGame(gameId, ProcessFixtures.ADMINISTRATOR, cookies, true, false);
            } finally {
                logout(ProcessFixtures.ADMINISTRATOR, cookies);
            }
            response.expectStatus().isForbidden();
        }

        @Test
        public void insufficientAuthority() {
            final var gameId = createGame();
            final var user = userWithoutManageGamesRole;
            final var cookies = login(user);
            final WebTestClient.ResponseSpec response;
            try {
                response = getMcBackEndClient().startGame(gameId, user, cookies, true, true);
            } finally {
                logout(user, cookies);
            }
            response.expectStatus().isForbidden();
        }

        private WebTestClient.ResponseSpec testAuthenticated(
                final UUID gameId,
                final BasicUserDetails user) {
            final var cookies = login(user);
            try {
                return getMcBackEndClient().startGame(gameId, user, cookies, true, true);
            } finally {
                logout(user, cookies);
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
     * Tests Spring annotations on {@link GameController#stopGame(SpringUser, UUID)}
     */
    @Nested
    @SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "SpotBugs bug")
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
            addUser(user);
            final var cookies = login(user);
            try {
                getMcBackEndClient().startGame(gameId, user, cookies, true, true);
                return getMcBackEndClient().stopGame(gameId, includeAuthentication? user: null, cookies, includeSessionCookie, includeXsrfToken);
            } finally {
                logout(user, cookies);
            }
        }
    }

}
