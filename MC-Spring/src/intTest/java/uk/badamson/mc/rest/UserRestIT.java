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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import uk.badamson.mc.*;
import uk.badamson.mc.presentation.SecurityConfiguration;
import uk.badamson.mc.presentation.UserController;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Tests {@link SecurityConfiguration}.
 * Tests Spring annotations on {@link UserController}.
 */
public class UserRestIT extends RestIT {

    /**
     * Tests Spring annotations on {@link UserController#addUser(UserDetailsRequest)}
     */
    @Nested
    @SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "SpotBugs bug")
    public class AddUser {

        @Test
        public void administrator() {
            final var performingUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            addUser(performingUser);

            final var response = test(performingUser, ProcessFixtures.ADMINISTRATOR, true, true);

            response.expectStatus().isBadRequest();
        }

        @Test
        public void duplicate() {
            final var performingUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            addUser(performingUser);
            final var addingUserDetails = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            test(performingUser, addingUserDetails, true, true);

            final var response = test(performingUser, addingUserDetails, true, true);

            response.expectStatus().isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        public void noAuthentication() {
            final var addingUserDetails = ProcessFixtures.createBasicUserDetailsWithAllRoles();

            final var response = test(ProcessFixtures.ADMINISTRATOR, addingUserDetails, false, false);

            response.expectStatus().is4xxClientError();
        }

        @Test
        public void noCsrfToken() {
            final var addingUserDetails = ProcessFixtures.createBasicUserDetailsWithAllRoles();

            final var response = test(ProcessFixtures.ADMINISTRATOR, addingUserDetails, true, false);

            response.expectStatus().isForbidden();
        }

        @Nested
        public class Valid {

            @Test
            public void addPlayer() {
                test(ProcessFixtures.createBasicUserDetailsWithPlayerRole());
            }

            @Test
            public void addGamesManager() {
                test(ProcessFixtures.createBasicUserDetailsWithManageGamesRole());
            }

            private void test(@Nonnull final BasicUserDetails addingUserDetails) {
                final var response = AddUser.this.test(ProcessFixtures.ADMINISTRATOR, addingUserDetails, true, true);

                response.expectStatus().isFound();
                final var location = response.returnResult(String.class).getResponseHeaders().getLocation();
                assertThat("localtion", location, notNullValue());
            }
        }

        private WebTestClient.ResponseSpec test(
                @Nonnull final BasicUserDetails loggedInUser,
                @Nonnull final BasicUserDetails addingUserDetails,
                final boolean includeSessionCookie,
                final boolean includeXsrfToken
        ) {
            final var cookies = login(loggedInUser);
            try {
                return getMcBackEndClient().addUser(
                        loggedInUser,
                        addingUserDetails,
                        cookies,
                        includeSessionCookie, includeXsrfToken
                );
            } finally {
                logout(loggedInUser, cookies);
            }
        }

    }

    /**
     * Tests Spring annotations on {@link UserController#getSelf(SpringUser)}
     */
    @Nested
    @SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "SpotBugs bug")
    public class GetSelf {

        @Test
        public void twice() {
            final var requestingUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            final var userId = addUser(requestingUser);

            getMcBackEndClient().getSelf(requestingUser);

            final var response2 = getMcBackEndClient().getSelf(requestingUser);

            /*
             * We can not check the response body for equivalence to a JSON
             * encoding of the user object, because the returned object has an
             * encoded password with a random salt. Checking the decoded response
             * body for equivalence to the user object is a weak test because User
             * objects have only entity semantics.
             */
            response2.expectStatus().isOk();
            response2.expectBody(UserResponse.class)
                    .value(UserResponse::id, is(userId))
                    .value(UserResponse::username, is(requestingUser.getUsername()))
                    .value(
                            UserResponse::authorities,
                            is(AuthorityValue.convertToValue(requestingUser.getAuthorities()))
                    );
        }

        @Test
        public void unknownUser() {
            final var detailsOfRequestingUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();

            final var response = getMcBackEndClient().getSelf(detailsOfRequestingUser);

            response.expectStatus().isUnauthorized();
        }

        @Test
        public void wrongPassword() {
            // Tough test: user-name is valid
            final var detailsOfRealUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            addUser(detailsOfRealUser);
            final var userDetailsWithWrongPassword = new BasicUserDetails(detailsOfRealUser.getUsername(),
                    "wrong-password",
                    detailsOfRealUser.getAuthorities(),
                    true, true, true, true);


            final var response = getMcBackEndClient().getSelf(userDetailsWithWrongPassword);

            response.expectStatus().isUnauthorized();
        }

        @Nested
        public class Valid {
            @Test
            public void a() {
                test(ProcessFixtures.createBasicUserDetailsWithAllRoles());
            }

            @Test
            public void b() {
                test(ProcessFixtures.createBasicUserDetailsWithPlayerRole());
            }

            private void test(final BasicUserDetails requestingUser) {
                final var userId = addUser(requestingUser);

                final var response = getMcBackEndClient().getSelf(requestingUser);

                response.expectStatus().isOk();
                response.expectBody(UserResponse.class)
                        .value(UserResponse::id, is(userId))
                        .value(UserResponse::username, is(requestingUser.getUsername()))
                        .value(
                                UserResponse::authorities,
                                is(AuthorityValue.convertToValue(requestingUser.getAuthorities()))
                        );
                final var cookies = response.returnResult(UserResponse.class).getResponseCookies();
                assertAll(
                        () -> assertThat(cookies, hasKey(McBackEndClient.SESSION_COOKIE_NAME)),
                        () -> assertThat(cookies, hasKey(McBackEndClient.XSRF_TOKEN_COOKIE_NAME))
                );
            }

        }

    }

    /**
     * Tests Spring annotations on {@link UserController#getUser(UUID)}
     */
    @Nested
    @SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "SpotBugs bug")
    public class GetUser {
        @Test
        public void forbidden() {
            // Tough test: user exists
            // Tough test: requesting user has maximum authority
            final var authorities = EnumSet.allOf(Authority.class);
            authorities.remove(Authority.ROLE_MANAGE_USERS);
            final var requestingUser = new BasicUserDetails(ProcessFixtures.createUserName(), "password1",
                    authorities, true, true, true, true);
            addUser(requestingUser);
            final var requestedUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            final var requestedUserId = addUser(requestedUser);

            final var response = test(requestedUserId, requestingUser, true, true, true);

            response.expectStatus().isForbidden();
        }

        @Test
        public void noAuthentication() {
            // Tough test: user exists
            final var requestedUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            final var requestedUserId = addUser(requestedUser);

            final var response = test(requestedUserId, ProcessFixtures.ADMINISTRATOR, false, false, false);

            response.expectStatus().isUnauthorized();
        }

        @Test
        public void unknownUser() {
            // Tough test: has permission
            final var requestedUserId = UUID.randomUUID();

            final var response = test(requestedUserId, ProcessFixtures.ADMINISTRATOR, true, true, true);

            response.expectStatus().isNotFound();
        }

        @Nested
        public class Valid {

            @Test
            public void requesterHasManageUsersRole() {
                testNonAdministrator(EnumSet.of(Authority.ROLE_MANAGE_USERS), true, true, true);
            }

            @Test
            public void requesterIsAdministrator() {
                final var requestedUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
                final var requestedUserId = addUser(requestedUser);

                final var response = test(requestedUserId, ProcessFixtures.ADMINISTRATOR, true, true, true);

                response.expectStatus().isOk();
                response.expectBody(UserResponse.class)
                        .value(UserResponse::id, is(requestedUserId));
            }

            @Test
            public void requesterHasAllRoles() {
                testNonAdministrator(Authority.ALL, true, true, true);
            }

            @Test
            public void inSession() {
                testNonAdministrator(Authority.ALL, false, true, true);
            }

            @Test
            public void withoutSession() {
                testNonAdministrator(Authority.ALL, true, false, true);
            }

            @Test
            public void withoutCsrfToken() {
                testNonAdministrator(Authority.ALL, true, true, false);
            }

            private void testNonAdministrator(
                    Set<Authority> authorities,
                    boolean includeAuthentication,
                    boolean includeSessionCookie,
                    boolean includeXsrfToken
            ) {
                final var requestingUserName = ProcessFixtures.createUserName();
                final var requestingUser = new BasicUserDetails(requestingUserName, "password1",
                        authorities, true, true, true, true);
                addUser(requestingUser);
                final var requestedUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
                final var requestedUserId = addUser(requestedUser);

                final var response = test(
                        requestedUserId, requestingUser,
                        includeAuthentication, includeSessionCookie, includeXsrfToken
                );

                response.expectStatus().isOk();
                response.expectBody(UserResponse.class)
                        .value(UserResponse::id, is(requestedUserId));
            }
        }

        private WebTestClient.ResponseSpec test(
                @Nonnull final UUID id,
                @Nonnull final BasicUserDetails loggedInUser,
                final boolean includeAuthentication,
                final boolean includeSessionCookie,
                final boolean includeXsrfToken) {
            BasicUserDetails authenticatingUser = includeAuthentication ? loggedInUser : null;
            final var cookies = login(loggedInUser);
            try {
                return getMcBackEndClient().getUser(
                        id,
                        authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
            } finally {
                logout(loggedInUser, cookies);
            }
        }

    }
}
