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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.badamson.mc.Authority;
import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.TestConfiguration;
import uk.badamson.mc.User;
import uk.badamson.mc.service.UserService;

/**
 * <p>
 * Unit tests of the {@link UserController} class.
 * <p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class UserControllerTest {

   @Nested
   public class Add {

      @Nested
      public class Valid {

         @Test
         public void a() throws Exception {
            test(USER_B);
         }

         @Test
         public void b() throws Exception {
            test(USER_C);
         }

         private void test(final BasicUserDetails addedUser) throws Exception {
            final var performingUser = USER_A;
            assert performingUser.getAuthorities()
                     .contains(Authority.ROLE_MANAGE_USERS);
            final var response = Add.this.test(performingUser, addedUser);

            final var location = response.andReturn().getResponse()
                     .getHeaderValue("Location");
            final var newUserOptional = service.getUsers().filter(
                     u -> u.getUsername().equals(addedUser.getUsername()))
                     .findAny();
            assertTrue(newUserOptional.isPresent(),
                     "List of users includes the added user");// guard
            final var newUser = newUserOptional.get();
            assertAll(() -> response.andExpect(status().isFound()),
                     () -> assertEquals(
                              UserController.createPathForUser(newUser.getId()),
                              location,
                              "redirection location is the resource for the added user"),
                     () -> assertEquivalentUserAttributes(
                              "Added user has the given attributes", addedUser,
                              newUser));
         }
      }// class

      @Test
      public void administrator() throws Exception {
         final var performingUser = USER_A;
         final var addedUser = ADMINISTRATOR;

         final var response = test(performingUser, addedUser);

         response.andExpect(status().isBadRequest());
      }

      @Test
      public void duplicate() throws Exception {
         final var performingUser = USER_A;
         final var addedUser = USER_B;

         service.add(addedUser);
         final var response = test(performingUser, addedUser);

         response.andExpect(status().isConflict());
      }

      @Test
      public void noAuthentication() throws Exception {
         final var performingUser = USER_A;
         final var addedUser = USER_B;
         service.add(performingUser);
         final var encoded = objectMapper.writeValueAsString(addedUser);
         final var request = post("/api/user")
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON).with(csrf())
                  .content(encoded);

         final var response = mockMvc.perform(request);

         response.andExpect(status().is4xxClientError());
         assertThat("User not added", !service.getUsers().anyMatch(
                  u -> u.getUsername().equals(addedUser.getUsername())));
      }

      @Test
      public void noCsrfToken() throws Exception {
         final var performingUser = USER_A;
         final var addedUser = USER_B;
         service.add(performingUser);
         final var encoded = objectMapper.writeValueAsString(addedUser);
         final var request = post("/api/user")
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON).with(user(performingUser))
                  .content(encoded);

         final var response = mockMvc.perform(request);

         assertAll(() -> response.andExpect(status().isForbidden()),
                  () -> assertThat("User not added",
                           !service.getUsers().anyMatch(u -> u.getUsername()
                                    .equals(addedUser.getUsername()))));
      }

      private ResultActions test(final User performingUser,
               final BasicUserDetails addedUser) throws Exception {
         service.add(performingUser);
         final var encoded = objectMapper.writeValueAsString(addedUser);
         final var request = post("/api/user")
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON).with(user(performingUser))
                  .with(csrf()).content(encoded);

         return mockMvc.perform(request);
      }

   }// class

   @Nested
   public class GetSelf {

      @Test
      public void a() throws Exception {
         test(USER_A);
      }

      @Test
      public void b() throws Exception {
         test(USER_B);
      }

      private void test(final User user) throws Exception {
         service.add(user);
         final var request = get("/api/self").accept(MediaType.APPLICATION_JSON)
                  .with(user(user)).with(csrf());

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
                  User.class);
         assertEquivalentUserAttributes("Response is the authenticated user",
                  user, decodedResponse);
      }

      @Test
      public void twice() throws Exception {
         final var user = USER_A;
         service.add(user);
         final var request1 = get("/api/self")
                  .accept(MediaType.APPLICATION_JSON).with(user(user))
                  .with(csrf());
         mockMvc.perform(request1);

         final var request2 = get("/api/self")
                  .accept(MediaType.APPLICATION_JSON).with(user(user))
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
         final var decodedResponse = objectMapper.readValue(jsonResponse,
                  User.class);
         assertEquivalentUserAttributes("Response is the authenticated user",
                  user, decodedResponse);
      }

      @Test
      public void unknownUser() throws Exception {
         final var user = USER_A;
         final var headers = new HttpHeaders();
         headers.setBasicAuth(user.getUsername(), user.getPassword());
         final var request = get("/api/self").accept(MediaType.APPLICATION_JSON)
                  .headers(headers).with(csrf());

         final var response = mockMvc.perform(request);

         response.andExpect(status().isUnauthorized());
      }

      @Test
      public void wrongPassword() throws Exception {
         final var user = USER_A;
         final var wrongPassword = "****";
         final var headers = new HttpHeaders();
         headers.setBasicAuth(user.getUsername(), wrongPassword);
         service.add(user);
         final var request = get("/api/self").accept(MediaType.APPLICATION_JSON)
                  .headers(headers).with(csrf());

         final var response = mockMvc.perform(request);

         response.andExpect(status().isUnauthorized());
      }

   }// class

   @Nested
   public class GetUser {
      @Nested
      public class Valid {

         @Test
         public void a() throws Exception {
            test(USER_A.getUsername(), USER_B);
         }

         @Test
         public void b() throws Exception {
            test(USER_B.getUsername(), USER_A);
         }

         private void test(final String requestingUserName,
                  final BasicUserDetails userDetails) throws Exception {
            assert !requestingUserName.equals(userDetails.getUsername());
            // Tough test: requesting user has minimum authority
            final var requestingUserDetails = new BasicUserDetails(
                     requestingUserName, "password1",
                     Set.of(Authority.ROLE_MANAGE_USERS), true, true, true,
                     true);
            final var requestingUser = service.add(requestingUserDetails);
            final var user = service.add(userDetails);

            final var response = GetUser.this.perform(user.getId(),
                     requestingUser);

            response.andExpect(status().isOk());
            final var jsonResponse = response.andReturn().getResponse()
                     .getContentAsString();
            final var decodedResponse = objectMapper.readValue(jsonResponse,
                     User.class);
            assertAll("Response is the identified user",
                     () -> assertEquivalentUserAttributes("user details",
                              userDetails, decodedResponse),
                     () -> assertEquals(user.getId(), decodedResponse.getId(),
                              "id"));
         }
      }// class

      @Test
      public void forbidden() throws Exception {
         // Tough test: user exists, requester has all other permissinos, and
         // request hasCSRF token
         // Tough test: requesting user has minimum authority
         final var requestingUserName = USER_C.getUsername();
         final var authorities = EnumSet.allOf(Authority.class);
         authorities.remove(Authority.ROLE_MANAGE_USERS);
         final var requestingUser = service
                  .add(new BasicUserDetails(requestingUserName, "password1",
                           authorities, true, true, true, true));
         final var user = service.add(USER_A);
         final var response = perform(user.getId(), requestingUser);

         response.andExpect(status().isForbidden());
      }

      @Test
      public void notLoggedIn() throws Exception {
         // Tough test: exists and has CSRF token
         final var user = USER_A;
         service.add(user);
         final var response = perform(user.getId(), null);

         response.andExpect(status().isUnauthorized());
      }

      private ResultActions perform(final UUID id, final User requestingUser)
               throws Exception {
         final var path = UserController.createPathForUser(id);
         var request = get(path).accept(MediaType.APPLICATION_JSON);
         if (requestingUser != null) {
            request = request.with(user(requestingUser));
         }

         return mockMvc.perform(request);
      }

      @Test
      public void unknownUser() throws Exception {
         // Tough test: has permission and CSRF token
         final var response = perform(USER_A.getId(), ADMINISTRATOR);

         response.andExpect(status().isNotFound());
      }
   }// class

   private static final User ADMINISTRATOR = User
            .createAdministrator("password");
   private static final User USER_A = new User(UUID.randomUUID(), "jeff",
            "letmein", Authority.ALL, true, true, true, true);
   private static final User USER_B = new User(UUID.randomUUID(), "allan",
            "password1", Set.of(Authority.ROLE_PLAYER), false, false, false,
            false);
   private static final User USER_C = new User(UUID.randomUUID(), "john",
            "password2", Set.of(Authority.ROLE_MANAGE_GAMES), true, true, true,
            true);

   @Autowired
   private UserService service;

   @Autowired
   private MockMvc mockMvc;

   @Autowired
   private ObjectMapper objectMapper;

   private void assertEquivalentUserAttributes(final String message,
            final BasicUserDetails expected, final BasicUserDetails actual)
            throws MultipleFailuresError {
      assertAll(message,
               () -> assertThat("username", actual.getUsername(),
                        is(expected.getUsername())),
               () -> assertThat("authorities", actual.getAuthorities(),
                        is(expected.getAuthorities())),
               () -> assertThat("accountNonExpired",
                        actual.isAccountNonExpired(),
                        is(expected.isAccountNonExpired())),
               () -> assertThat("accountNonLocked", actual.isAccountNonLocked(),
                        is(expected.isAccountNonLocked())),
               () -> assertThat("credentialsNonExpired",
                        actual.isCredentialsNonExpired(),
                        is(expected.isCredentialsNonExpired())),
               () -> assertThat("enabled", actual.isEnabled(),
                        is(expected.isEnabled())));
   }
}
