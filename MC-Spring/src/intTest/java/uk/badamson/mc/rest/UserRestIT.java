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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import uk.badamson.mc.*;
import uk.badamson.mc.presentation.UserController;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class UserRestIT {
    private static final String MONGO_DB_PASSWORD = "LetMeIn1";
    private static final String ADMINISTRATOR_PASSWORD = ProcessFixtures.ADMINISTRATOR.getPassword();

    private static MongoDBContainer MONGO_DB_CONTAINER;
    private static McBackEndProcess MC_BACK_END_PROCESS;
    private static McBackEndClient MC_BACK_END_CLIENT;

    @BeforeAll
    public static void setUp() throws TimeoutException {
        MONGO_DB_CONTAINER = new MongoDBContainer(ProcessFixtures.MONGO_DB_IMAGE);
        MONGO_DB_CONTAINER.start();
        final var mongoDBPath = MONGO_DB_CONTAINER.getReplicaSetUrl();
        MC_BACK_END_PROCESS = new McBackEndProcess(mongoDBPath, MONGO_DB_PASSWORD, ADMINISTRATOR_PASSWORD);
        MC_BACK_END_CLIENT = new McBackEndClient(
                "localhost", MC_BACK_END_PROCESS.getServerPort(), ADMINISTRATOR_PASSWORD
        );
    }

    @AfterAll
    public static void tearDown() {
        MC_BACK_END_PROCESS.close();
        MONGO_DB_CONTAINER.close();
    }

    private WebTestClient.ResponseSpec addUser(
            @Nonnull final BasicUserDetails loggedInUser,
            @Nonnull final BasicUserDetails addingUserDetails,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken
    ) {
        final var cookies = MC_BACK_END_CLIENT.login(loggedInUser);
        final var headers = MC_BACK_END_CLIENT.connectWebTestClient("/api/user").post()
                .contentType(MediaType.APPLICATION_JSON);
        McBackEndClient.secure(headers, loggedInUser, cookies, includeSessionCookie, includeXsrfToken);
        final var request = headers.bodyValue(McBackEndClient.encodeAsJson(addingUserDetails));

        try {
            return request.exchange();
        } finally {
            MC_BACK_END_CLIENT.logout(loggedInUser, cookies);
        }
    }

    private WebTestClient.ResponseSpec getUser(
            @Nonnull final UUID id,
            @Nonnull final BasicUserDetails loggedInUser,
            final boolean includeAuthentication,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var path = Paths.createPathForUser(id);
        BasicUserDetails authenticatingUser = includeAuthentication ? loggedInUser : null;
        final var cookies = MC_BACK_END_CLIENT.login(loggedInUser);
        final var request = MC_BACK_END_CLIENT.connectWebTestClient(path)
                .get()
                .accept(MediaType.APPLICATION_JSON);
        McBackEndClient.secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        try {
            return request.exchange();
        } finally {
            MC_BACK_END_CLIENT.logout(loggedInUser, cookies);
        }
    }


    /**
     * Tests {@link UserController#addUser(UserDetailsRequest)}
     */
    @Nested
    public class AddUser {

        @Test
        public void administrator() {
            final var performingUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            MC_BACK_END_CLIENT.addUser(performingUser);

            final var response = addUser(performingUser, ProcessFixtures.ADMINISTRATOR, true, true);

            response.expectStatus().isBadRequest();
        }

        @Test
        public void duplicate() {
            final var performingUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            MC_BACK_END_CLIENT.addUser(performingUser);
            final var addingUserDetails = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            addUser(performingUser, addingUserDetails, true, true);

            final var response = addUser(performingUser, addingUserDetails, true, true);

            response.expectStatus().isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        public void noAuthentication() {
            final var addingUserDetails = ProcessFixtures.createBasicUserDetailsWithAllRoles();

            final var response = addUser(ProcessFixtures.ADMINISTRATOR, addingUserDetails, false, false);

            response.expectStatus().is4xxClientError();
        }

        @Test
        public void noCsrfToken() {
            final var addingUserDetails = ProcessFixtures.createBasicUserDetailsWithAllRoles();

            final var response = addUser(ProcessFixtures.ADMINISTRATOR, addingUserDetails, true, false);

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
                final var response = addUser(ProcessFixtures.ADMINISTRATOR, addingUserDetails, true, true);

                response.expectStatus().isFound();
                final var location = response.returnResult(String.class).getResponseHeaders().getLocation();
                assertThat("localtion", location, notNullValue());
            }
        }

    }

    /**
     * Tests {@link UserController#getSelf(SpringUser)}
     */
    @Nested
    public class GetSelf {

        @Test
        public void twice() {
            final var requestingUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            final var userId = MC_BACK_END_CLIENT.addUser(requestingUser);

            MC_BACK_END_CLIENT.getSelf(requestingUser);

            final var response2 = MC_BACK_END_CLIENT.getSelf(requestingUser);

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

            final var response = MC_BACK_END_CLIENT.getSelf(detailsOfRequestingUser);

            response.expectStatus().isUnauthorized();
        }

        @Test
        public void wrongPassword() {
            // Tough test: user-name is valid
            final var detailsOfRealUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            MC_BACK_END_CLIENT.addUser(detailsOfRealUser);
            final var userDetailsWithWrongPassword = new BasicUserDetails(detailsOfRealUser.getUsername(),
                    "wrong-password",
                    detailsOfRealUser.getAuthorities(),
                    true, true, true, true);


            final var response = MC_BACK_END_CLIENT.getSelf(userDetailsWithWrongPassword);

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
                final var userId =MC_BACK_END_CLIENT.addUser(requestingUser);

                final var response = MC_BACK_END_CLIENT.getSelf(requestingUser);

                response.expectStatus().isOk();
                response.expectBody(UserResponse.class)
                        .value(UserResponse::id, is(userId))
                        .value(UserResponse::username, is(requestingUser.getUsername()))
                        .value(
                                UserResponse::authorities,
                                is(AuthorityValue.convertToValue(requestingUser.getAuthorities()))
                        );
            }

        }

    }

    /**
     * Tests {@link UserController#getUser(UUID)}
     */
    @Nested
    public class GetUser {
        @Test
        public void forbidden() {
            // Tough test: user exists
            // Tough test: requesting user has maximum authority
            final var authorities = EnumSet.allOf(Authority.class);
            authorities.remove(Authority.ROLE_MANAGE_USERS);
            final var requestingUser = new BasicUserDetails(ProcessFixtures.createUserName(), "password1",
                    authorities, true, true, true, true);
            MC_BACK_END_CLIENT.addUser(requestingUser);
            final var requestedUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            final var requestedUserId = MC_BACK_END_CLIENT.addUser(requestedUser);

            final var response = getUser(requestedUserId, requestingUser, true, true, true);

            response.expectStatus().isForbidden();
        }

        @Test
        public void noAuthentication() {
            // Tough test: user exists
            final var requestedUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            final var requestedUserId = MC_BACK_END_CLIENT.addUser(requestedUser);

            final var response = getUser(requestedUserId, ProcessFixtures.ADMINISTRATOR, false, false, false);

            response.expectStatus().isUnauthorized();
        }

        @Test
        public void unknownUser() {
            // Tough test: has permission
            final var requestedUserId = UUID.randomUUID();

            final var response = getUser(requestedUserId, ProcessFixtures.ADMINISTRATOR, true, true, true);

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
                final var requestedUserId = MC_BACK_END_CLIENT.addUser(requestedUser);

                final var response = getUser(requestedUserId, ProcessFixtures.ADMINISTRATOR, true, true, true);

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
                MC_BACK_END_CLIENT.addUser(requestingUser);
                final var requestedUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
                final var requestedUserId = MC_BACK_END_CLIENT.addUser(requestedUser);

                final var response = getUser(
                        requestedUserId, requestingUser,
                        includeAuthentication, includeSessionCookie, includeXsrfToken
                );

                response.expectStatus().isOk();
                response.expectBody(UserResponse.class)
                        .value(UserResponse::id, is(requestedUserId));
            }
        }
    }
}
