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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.badamson.mc.Authority;
import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.TestConfiguration;
import uk.badamson.mc.User;
import uk.badamson.mc.rest.AuthorityValue;
import uk.badamson.mc.rest.Paths;
import uk.badamson.mc.rest.UserDetailsRequest;
import uk.badamson.mc.rest.UserResponse;
import uk.badamson.mc.service.UserSpringService;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class UserControllerTest {

    @Autowired
    private UserSpringService service;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static void assertEquivalentUserAttributes(
            final String message,
            final BasicUserDetails details,
            final UserResponse response)
            throws MultipleFailuresError {
        assertAll(message,
                () -> assertThat("username", response.username(),
                        is(details.getUsername())),
                () -> assertThat("authorities", response.authorities(),
                        is(AuthorityValue.convertToValue(details.getAuthorities()))),
                () -> assertThat("accountNonExpired",
                        response.accountNonExpired(),
                        is(details.isAccountNonExpired())),
                () -> assertThat("accountNonLocked", response.accountNonLocked(),
                        is(details.isAccountNonLocked())),
                () -> assertThat("credentialsNonExpired",
                        response.credentialsNonExpired(),
                        is(details.isCredentialsNonExpired())),
                () -> assertThat("enabled", response.enabled(),
                        is(details.isEnabled())));
    }

    private boolean isKnownUsername(@Nonnull String username) {
        return service.getUsers().anyMatch(u -> u.username().equals(username));
    }

    private ResultActions addUser(
            @Nullable final User performingUser,
            @Nonnull final BasicUserDetails addingUserDetails,
            final boolean withCsrfToken
    ) throws Exception {
        final var encoded = objectMapper.writeValueAsString(
                UserDetailsRequest.convertToRequest(addingUserDetails)
        );
        var request = post(Paths.USERS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(encoded);
        if (performingUser != null) {
            request = request.with(user(SpringUser.convertToSpring(performingUser)));
        }
        if (withCsrfToken) {
            request = request.with(csrf());
        }

        return mockMvc.perform(request);
    }

    private ResultActions getSelf(@Nullable final SpringUser requestingUser) throws Exception {
        var request = get(Paths.SELF_PATH).accept(MediaType.APPLICATION_JSON);
        if (requestingUser != null) {
            request = request.with(user(requestingUser));
        }

        return mockMvc.perform(request);
    }

    private ResultActions getUser(@Nonnull final UUID id, @Nullable final User requestingUser)
            throws Exception {
        final var path = Paths.createPathForUser(id);
        var request = get(path).accept(MediaType.APPLICATION_JSON);
        if (requestingUser != null) {
            request = request.with(user(SpringUser.convertToSpring(requestingUser)));
        }

        return mockMvc.perform(request);
    }


    /**
     * Tests {@link UserController#addUser(UserDetailsRequest)}
     */
    @Nested
    public class AddUser {

        @Test
        public void administrator() throws Exception {
            final var performingUser = service.add(Fixtures.createBasicUserDetailsWithAllRoles());

            final var response = addUser(performingUser, Fixtures.ADMINISTRATOR, true);

            response.andExpect(status().isBadRequest());
        }

        @Test
        public void duplicate() throws Exception {
            final var performingUser = service.add(Fixtures.createBasicUserDetailsWithAllRoles());
            final var addingUserDetails = Fixtures.createBasicUserDetailsWithAllRoles();
            service.add(addingUserDetails);

            final var response = addUser(performingUser, addingUserDetails, true);

            response.andExpect(status().isConflict());
        }

        @Test
        public void noAuthentication() throws Exception {
            final var addingUserDetails = Fixtures.createBasicUserDetailsWithAllRoles();

            final var response = addUser(null, addingUserDetails, true);

            assertAll(
                    () -> response.andExpect(status().is4xxClientError()),
                    () -> assertThat("User not added", isKnownUsername(addingUserDetails.getUsername()), is(false)));
        }

        @Test
        public void noCsrfToken() throws Exception {
            final var performingUser = service.add(Fixtures.createBasicUserDetailsWithAllRoles());
            final var addingUserDetails = Fixtures.createBasicUserDetailsWithAllRoles();

            final var response = addUser(performingUser, addingUserDetails, false);

            assertAll(() -> response.andExpect(status().isForbidden()),
                    () -> assertThat("User not added", isKnownUsername(addingUserDetails.getUsername()), is(false)));
        }

        @Nested
        public class Valid {

            @Test
            public void addPlayer() throws Exception {
                test(Fixtures.createBasicUserDetailsWithPlayerRole());
            }

            @Test
            public void addGamesManager() throws Exception {
                test(Fixtures.createBasicUserDetailsWithManageGamesRole());
            }

            private void test(@Nonnull final BasicUserDetails addingUserDetails) throws Exception {
                final var performingUser = service.add(Fixtures.createBasicUserDetailsWithAllRoles());
                assert performingUser.getAuthorities().contains(Authority.ROLE_MANAGE_USERS);

                final var response = addUser(performingUser, addingUserDetails, true);

                final var location = response.andReturn().getResponse()
                        .getHeaderValue("Location");
                final var newUserOptional = service.getUsers().filter(
                                u -> u.username().equals(addingUserDetails.getUsername()))
                        .findAny();
                assertThat("List of users includes the added user", newUserOptional.isPresent(),
                        is(true));// guard
                final var newUser = newUserOptional.get();
                assertAll(() -> response.andExpect(status().isFound()),
                        () -> assertThat("redirection location is the resource for the added user",
                                location,
                                is(Paths.createPathForUser(newUser.id()))
                        ),
                        () -> assertEquivalentUserAttributes(
                                "Added user has the given attributes", addingUserDetails,
                                newUser));
            }
        }

    }

    /**
     * Tests {@link UserController#getSelf(SpringUser)}
     */
    @Nested
    public class GetSelf {

        @Test
        public void twice() throws Exception {
            final var detailsOfRequestingUser = Fixtures.createBasicUserDetailsWithAllRoles();
            final var requestingUser = SpringUser.convertToSpring(service.add(detailsOfRequestingUser));
            getSelf(requestingUser);

            final var response2 = getSelf(requestingUser);
            /*
             * We can not test that the response has the same session and CSRF
             * cookies, because MockMvc does not set those cookies.
             */
            /*
             * We can not check the response body for equivalence to a JSON
             * encoding of the user object, because the returned object has an
             * encoded password with a random salt. Checking the decoded response
             * body for equivalence to the user object is a weak test because User
             * objects have only entity semantics.
             */
            final var jsonResponse = response2.andReturn().getResponse()
                    .getContentAsString();
            final var decodedResponse = objectMapper.readValue(jsonResponse, UserResponse.class);
            assertEquivalentUserAttributes("Response is the authenticated user",
                    detailsOfRequestingUser, decodedResponse);
        }

        @Test
        public void unknownUser() throws Exception {
            final var user = Fixtures.createUserWithAllRoles();
            final var headers = new HttpHeaders();
            headers.setBasicAuth(user.getUsername(), user.getPassword());
            final var request = get("/api/self").accept(MediaType.APPLICATION_JSON)
                    .headers(headers);

            final var response = mockMvc.perform(request);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void wrongPassword() throws Exception {
            final var basicUserDetails = Fixtures.createBasicUserDetailsWithAllRoles();
            final var wrongPassword = "****";
            final var headers = new HttpHeaders();
            headers.setBasicAuth(basicUserDetails.getUsername(), wrongPassword);
            service.add(basicUserDetails);
            final var request = get(Paths.SELF_PATH).accept(MediaType.APPLICATION_JSON)
                    .headers(headers);

            final var response = mockMvc.perform(request);

            response.andExpect(status().isUnauthorized());
        }

        @Nested
        public class Valid {
            @Test
            public void a() throws Exception {
                test(Fixtures.createBasicUserDetailsWithAllRoles());
            }

            @Test
            public void b() throws Exception {
                test(Fixtures.createBasicUserDetailsWithPlayerRole());
            }

            private void test(final BasicUserDetails detailsOfRequestingUser) throws Exception {
                final var requestingUser = SpringUser.convertToSpring(service.add(detailsOfRequestingUser));

                final var response = getSelf(requestingUser);

                response.andExpect(status().isOk());
                /*
                 * We can not test that the response has session and CSRF cookies,
                 * because MockMvc does not set those cookies.
                 */
                /*
                 * We can not check the response body for equivalence to a JSON
                 * encoding of the user object, because the returned object has an
                 * encoded password with a random salt. Checking the decoded response
                 * body for equivalence to the user object is a weak test because User
                 * objects have only entity semantics.
                 */
                final var jsonResponse = response.andReturn().getResponse()
                        .getContentAsString();
                final var decodedResponse = objectMapper.readValue(jsonResponse,
                        UserResponse.class);
                assertEquivalentUserAttributes("Response is the authenticated user",
                        detailsOfRequestingUser, decodedResponse);
            }

        }

    }

    /**
     * Tests {@link UserController#getUser(UUID)}
     */
    @Nested
    public class GetUser {
        @Test
        public void forbidden() throws Exception {
            // Tough test: user exists, requester has all other permissions
            // Tough test: requesting user has minimum authority
            final var requestingUserName = Fixtures.createUserName();
            final var authorities = EnumSet.allOf(Authority.class);
            authorities.remove(Authority.ROLE_MANAGE_USERS);
            final var requestingUserDetails = new BasicUserDetails(requestingUserName, "password1",
                    authorities, true, true, true, true);
            final var requestingUser = service.add(requestingUserDetails);
            final var requestedUser = service.add(Fixtures.createBasicUserDetailsWithAllRoles());

            final var response = getUser(requestedUser.getId(), requestingUser);

            response.andExpect(status().isForbidden());
        }

        @Test
        public void notLoggedIn() throws Exception {
            // Tough test: user exists
            final var basicUserDetails = Fixtures.createBasicUserDetailsWithAllRoles();
            final var requestedUser = service.add(basicUserDetails);

            final var response = getUser(requestedUser.getId(), null);

            response.andExpect(status().isUnauthorized());
        }

        @Test
        public void unknownUser() throws Exception {
            // Tough test: has permission
            final var requestingUserOptional = service.getUser(User.ADMINISTRATOR_ID);
            assert requestingUserOptional.isPresent();
            final var requestingUser = requestingUserOptional.get();
            final var response = getUser(Fixtures.createUserWithAllRoles().getId(), requestingUser);

            response.andExpect(status().isNotFound());
        }

        @Nested
        public class Valid {

            @Test
            public void requesterHasPlayerRole() throws Exception {
                testNonAdministrator(Fixtures.createUserName(), Fixtures.createBasicUserDetailsWithPlayerRole());
            }

            @Test
            public void requesterIsAdministrator() throws Exception {
                final Optional<User> userOptional = service.getUser(User.ADMINISTRATOR_ID);
                assert userOptional.isPresent();

                test(Fixtures.createUserName(), userOptional.get());
            }

            @Test
            public void requesterHasAllRoles() throws Exception {
                testNonAdministrator(Fixtures.createUserName(), Fixtures.createBasicUserDetailsWithAllRoles());
            }

            private void test(final String requestingUserName, final User requestedUser)
                    throws Exception {
                // Tough test: requesting user has minimum authority
                final var requestingUserDetails = new BasicUserDetails(
                        requestingUserName, "password1",
                        EnumSet.of(Authority.ROLE_MANAGE_USERS),
                        true, true, true,
                        true);
                final var requestingUser = service.add(requestingUserDetails);

                final var response = getUser(requestedUser.getId(), requestingUser);

                response.andExpect(status().isOk());
                final var jsonResponse = response.andReturn().getResponse()
                        .getContentAsString();
                final var decodedResponse = objectMapper.readValue(jsonResponse,
                        UserResponse.class);
                assertAll("Response is the identified user",
                        () -> assertEquivalentUserAttributes("user details", requestedUser,
                                decodedResponse),
                        () -> assertEquals(requestedUser.getId(), decodedResponse.id(),
                                "id"));
            }

            private void testNonAdministrator(
                    final String requestingUserName,
                    final BasicUserDetails requestedUserDetails) throws Exception {
                assert !requestingUserName.equals(requestedUserDetails.getUsername());
                final var requestedUser = service.add(requestedUserDetails);

                test(requestingUserName, requestedUser);
            }
        }
    }
}
