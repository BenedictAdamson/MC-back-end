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
import uk.badamson.mc.rest.UserDetailsRequest;
import uk.badamson.mc.rest.UserResponse;
import uk.badamson.mc.service.UserSpringService;
import uk.badamson.mc.spring.SpringUser;

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

    private void assertEquivalentUserAttributes(final String message,
                                                final BasicUserDetails details, final UserResponse response)
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

    @Nested
    public class Add {

        @Test
        public void administrator() throws Exception {
            final var performingUser = Fixtures.createBasicUserDetailsWithAllRoles();

            final var response = test(performingUser, Fixtures.ADMINISTRATOR);

            response.andExpect(status().isBadRequest());
        }

        @Test
        public void duplicate() throws Exception {
            final var performingUser = Fixtures.createBasicUserDetailsWithAllRoles();
            final var addedUser = Fixtures.createBasicUserDetailsWithAllRoles();

            service.add(addedUser);
            final var response = test(performingUser, addedUser);

            response.andExpect(status().isConflict());
        }

        @Test
        public void noAuthentication() throws Exception {
            final var performingUser = Fixtures.createBasicUserDetailsWithAllRoles();
            final var addedUser =
                    UserDetailsRequest.convertToRequest(Fixtures.createBasicUserDetailsWithAllRoles());
            service.add(performingUser);
            final var encoded = objectMapper.writeValueAsString(addedUser);
            final var request = post("/api/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON).with(csrf())
                    .content(encoded);

            final var response = mockMvc.perform(request);

            response.andExpect(status().is4xxClientError());
            assertThat("User not added", service.getUsers().noneMatch(
                    u -> u.username().equals(addedUser.username())));
        }

        @Test
        public void noCsrfToken() throws Exception {
            final var addedUser =
                    UserDetailsRequest.convertToRequest(Fixtures.createBasicUserDetailsWithAllRoles());
            final var performingUser = service.add(Fixtures.createBasicUserDetailsWithAllRoles());
            final var encoded = objectMapper.writeValueAsString(addedUser);
            final var request = post("/api/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON).with(user(SpringUser.convertToSpring(performingUser)))
                    .content(encoded);

            final var response = mockMvc.perform(request);

            assertAll(() -> response.andExpect(status().isForbidden()),
                    () -> assertThat("User not added",
                            service.getUsers().noneMatch(u -> u.username()
                                    .equals(addedUser.username()))));
        }

        private ResultActions test(final BasicUserDetails performingUserDetails,
                                   final BasicUserDetails addedUserDetails) throws Exception {
            final var performingUser = service.add(performingUserDetails);
            final var encoded = objectMapper.writeValueAsString(
                    UserDetailsRequest.convertToRequest(addedUserDetails)
            );
            final var request = post("/api/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON).with(user(SpringUser.convertToSpring(performingUser)))
                    .with(csrf()).content(encoded);

            return mockMvc.perform(request);
        }

        @Nested
        public class Valid {

            @Test
            public void a() throws Exception {
                test(Fixtures.createBasicUserDetailsWithPlayerRole());
            }

            @Test
            public void b() throws Exception {
                test(Fixtures.createBasicUserDetailsWithManageGamesRole());
            }

            private void test(final BasicUserDetails userDetails) throws Exception {
                final var performingUser = Fixtures.createBasicUserDetailsWithAllRoles();
                assert performingUser.getAuthorities()
                        .contains(Authority.ROLE_MANAGE_USERS);
                final var response = Add.this.test(performingUser, userDetails);

                final var location = response.andReturn().getResponse()
                        .getHeaderValue("Location");
                final var newUserOptional = service.getUsers().filter(
                                u -> u.username().equals(userDetails.getUsername()))
                        .findAny();
                assertTrue(newUserOptional.isPresent(),
                        "List of users includes the added user");// guard
                final var newUser = newUserOptional.get();
                assertAll(() -> response.andExpect(status().isFound()),
                        () -> assertEquals(
                                UserController.createPathForUser(newUser.id()),
                                location,
                                "redirection location is the resource for the added user"),
                        () -> assertEquivalentUserAttributes(
                                "Added user has the given attributes", userDetails,
                                newUser));
            }
        }

    }

    @Nested
    public class GetSelf {

        @Test
        public void a() throws Exception {
            test(Fixtures.createBasicUserDetailsWithAllRoles());
        }

        @Test
        public void b() throws Exception {
            test(Fixtures.createBasicUserDetailsWithPlayerRole());
        }

        private void test(final BasicUserDetails details) throws Exception {
            final var user = service.add(details);
            final var request = get("/api/self").accept(MediaType.APPLICATION_JSON)
                    .with(user(SpringUser.convertToSpring(user))).with(csrf());

            final var response = mockMvc.perform(request);

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
                    details, decodedResponse);
        }

        @Test
        public void twice() throws Exception {
            final var basicUserDetails = Fixtures.createBasicUserDetailsWithAllRoles();
            final var user = service.add(basicUserDetails);
            final var request1 = get("/api/self")
                    .accept(MediaType.APPLICATION_JSON).with(user(SpringUser.convertToSpring(user)))
                    .with(csrf());
            mockMvc.perform(request1);

            final var request2 = get("/api/self")
                    .accept(MediaType.APPLICATION_JSON).with(user(SpringUser.convertToSpring(user)))
                    .with(csrf());
            final var response2 = mockMvc.perform(request2);
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
                    basicUserDetails, decodedResponse);
        }

        @Test
        public void unknownUser() throws Exception {
            final var user = Fixtures.createUserWithAllRoles();
            final var headers = new HttpHeaders();
            headers.setBasicAuth(user.getUsername(), user.getPassword());
            final var request = get("/api/self").accept(MediaType.APPLICATION_JSON)
                    .headers(headers).with(csrf());

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
            final var request = get("/api/self").accept(MediaType.APPLICATION_JSON)
                    .headers(headers).with(csrf());

            final var response = mockMvc.perform(request);

            response.andExpect(status().isUnauthorized());
        }

    }

    @Nested
    public class GetUser {
        @Test
        public void forbidden() throws Exception {
            // Tough test: user exists, requester has all other permissions, and
            // request hasCSRF token
            // Tough test: requesting user has minimum authority
            final var requestingUserName = Fixtures.createUserName();
            final var authorities = EnumSet.allOf(Authority.class);
            authorities.remove(Authority.ROLE_MANAGE_USERS);
            final var requestingUser = service
                    .add(new BasicUserDetails(requestingUserName, "password1",
                            authorities, true, true, true, true));
            final var user = service.add(Fixtures.createBasicUserDetailsWithAllRoles());
            final var response = perform(user.getId(), requestingUser);

            response.andExpect(status().isForbidden());
        }

        @Test
        public void notLoggedIn() throws Exception {
            // Tough test: exists and has CSRF token
            final var basicUserDetails = Fixtures.createBasicUserDetailsWithAllRoles();
            final var user = service.add(basicUserDetails);
            final var response = perform(user.getId(), null);

            response.andExpect(status().isUnauthorized());
        }

        private ResultActions perform(final UUID id, final User requestingUser)
                throws Exception {
            final var path = UserController.createPathForUser(id);
            var request = get(path).accept(MediaType.APPLICATION_JSON);
            if (requestingUser != null) {
                request = request.with(user(SpringUser.convertToSpring(requestingUser)));
            }

            return mockMvc.perform(request);
        }

        @Test
        public void unknownUser() throws Exception {
            // Tough test: has permission and CSRF token
            final var requestingUserOptional = service.getUser(User.ADMINISTRATOR_ID);
            assert requestingUserOptional.isPresent();
            final var requestingUser = requestingUserOptional.get();
            final var response = perform(Fixtures.createUserWithAllRoles().getId(), requestingUser);

            response.andExpect(status().isNotFound());
        }

        @Nested
        public class Valid {

            @Test
            public void a() throws Exception {
                testNonAdministrator(Fixtures.createUserName(), Fixtures.createBasicUserDetailsWithPlayerRole());
            }

            @Test
            public void administrator() throws Exception {
                final Optional<User> userOptional = service.getUser(User.ADMINISTRATOR_ID);
                assertThat("user", userOptional.isPresent());
                test(Fixtures.createUserName(), userOptional.get());
            }

            @Test
            public void b() throws Exception {
                testNonAdministrator(Fixtures.createUserName(), Fixtures.createBasicUserDetailsWithAllRoles());
            }

            private void test(final String requestingUserName, final User user)
                    throws Exception {
                // Tough test: requesting user has minimum authority
                final var requestingUserDetails = new BasicUserDetails(
                        requestingUserName, "password1",
                        EnumSet.of(Authority.ROLE_MANAGE_USERS),
                        true, true, true,
                        true);
                final var requestingUser = service.add(requestingUserDetails);

                final var response = GetUser.this.perform(user.getId(), requestingUser);

                response.andExpect(status().isOk());
                final var jsonResponse = response.andReturn().getResponse()
                        .getContentAsString();
                final var decodedResponse = objectMapper.readValue(jsonResponse,
                        UserResponse.class);
                assertAll("Response is the identified user",
                        () -> assertEquivalentUserAttributes("user details", user,
                                decodedResponse),
                        () -> assertEquals(user.getId(), decodedResponse.id(),
                                "id"));
            }

            private void testNonAdministrator(final String requestingUserName,
                                              final BasicUserDetails userDetails) throws Exception {
                assert !requestingUserName.equals(userDetails.getUsername());
                final var user = service.add(userDetails);
                test(requestingUserName, user);
            }
        }
    }
}
