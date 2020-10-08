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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import uk.badamson.mc.BackEndWorldCore;
import uk.badamson.mc.TestConfiguration;
import uk.badamson.mc.User;
import uk.badamson.mc.service.Service;

/**
 * <p>
 * Unit tests of the {@link UserController} class.
 * <p>
 * <p>
 * We can not use JUnit 5 {@link Nested} test classes because
 * {@link SpringBootTest} does not work properly with them; in particular the
 * {@link DirtiesContext} annotation is ignored on nested tets.
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class UserControllerTest {

   private static final User USER_A = new User("jeff", "letmein", Authority.ALL,
            true, true, true, true);

   private static final User USER_B = new User("allan", "password1",
            Set.of(Authority.ROLE_PLAYER), false, false, false, false);

   @Autowired
   private Service service;

   @Autowired
   private MockMvc mockMvc;

   private ResultActions add(final User performingUser, final User addedUser)
            throws Exception {
      service.add(performingUser);
      final var request = post("/api/user")
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON).with(user(performingUser))
               .with(csrf()).content(BackEndWorldCore.encodeAsJson(addedUser));

      return mockMvc.perform(request);
   }

   @Test
   public void add_a() throws Exception {
      final var performingUser = USER_A;
      final var addedUser = USER_B;

      final ResultActions response = add(performingUser, addedUser);

      response.andExpect(status().isCreated());
      assertThat("List of users includes the added user", service.getUsers()
               .anyMatch(u -> u.getUsername().equals(addedUser.getUsername())));
   }

   @Test
   public void add_administrator() throws Exception {
      final var performingUser = USER_A;
      final var addedUser = new User(User.ADMINISTRATOR_USERNAME, "password1",
               Set.of(Authority.ROLE_PLAYER), true, true, true, true);

      final ResultActions response = add(performingUser, addedUser);

      response.andExpect(status().isBadRequest());
   }

   @Test
   public void add_duplicate() throws Exception {
      final var performingUser = USER_A;
      final var addedUser = USER_B;

      service.add(addedUser);
      final ResultActions response = add(performingUser, addedUser);

      response.andExpect(status().isConflict());
   }

   private void getSelf(final User user) throws Exception {
      service.add(user);
      final var request = get("/api/self").accept(MediaType.APPLICATION_JSON)
               .with(user(user)).with(csrf());

      final var response = mockMvc.perform(request);

      response.andExpect(status().isOk());
      /*
       * We can not check the response body for equivalence to a JSON encoding
       * of the user object, because the returned object has an encoded password
       * with a random salt. Checking the decoded response body for equivalence
       * to the user object is a weak test because User objects have only entity
       * semantics.
       */
      final var jsonResponse = response.andReturn().getResponse()
               .getContentAsString();
      final var mapper = new ObjectMapper();
      final var decodedResponse = mapper.readValue(jsonResponse, User.class);
      assertAll("Response is the authenticated user",
               () -> assertThat("username", decodedResponse.getUsername(),
                        is(user.getUsername())),
               () -> assertThat("authorities", decodedResponse.getAuthorities(),
                        is(user.getAuthorities())),
               () -> assertThat("password",
                        service.getPasswordEncoder().matches(user.getPassword(),
                                 decodedResponse.getPassword())),
               () -> assertThat("accountNonExpired",
                        decodedResponse.isAccountNonExpired(),
                        is(user.isAccountNonExpired())),
               () -> assertThat("accountNonLocked",
                        decodedResponse.isAccountNonLocked(),
                        is(user.isAccountNonLocked())),
               () -> assertThat("credentialsNonExpired",
                        decodedResponse.isCredentialsNonExpired(),
                        is(user.isCredentialsNonExpired())),
               () -> assertThat("enabled", decodedResponse.isEnabled(),
                        is(user.isEnabled())));
   }

   @Test
   public void getSelf_a() throws Exception {
      getSelf(USER_A);
   }

   @Test
   public void getSelf_b() throws Exception {
      getSelf(USER_B);
   }

   @Test
   public void getSelf_unknownUser() throws Exception {
      final var user = USER_A;
      final var headers = new HttpHeaders();
      headers.setBasicAuth(user.getUsername(), user.getPassword());
      final var request = get("/api/self").accept(MediaType.APPLICATION_JSON)
               .headers(headers).with(csrf());

      final var response = mockMvc.perform(request);

      response.andExpect(status().isUnauthorized());
   }

   @Test
   public void getSelf_wrongPassword() throws Exception {
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
}
