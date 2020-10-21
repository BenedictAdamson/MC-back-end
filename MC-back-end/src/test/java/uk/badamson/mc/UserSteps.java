package uk.badamson.mc;
/*
 * © Copyright Benedict Adamson 2019-20.
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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import uk.badamson.mc.service.UserService;

/**
 * <p>
 * Definitions of BDD steps, for features about users.
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class UserSteps {

   @Autowired
   private BackEndWorldCore worldCore;

   @Autowired
   private UserService service;

   @Autowired
   private ObjectMapper objectMapper;

   private User user;

   private User loggedInUser;

   private List<User> responseUserList;

   @Then("MC does not present adding a user as an option")
   public void add_user_not_permitted() throws Exception {
      addUser("Allan", "letmein");
      worldCore.expectResponse(status().isForbidden());
   }

   @When("adding a user named {string} with  password {string}")
   public void adding_a_user_named(final String name, final String password)
            throws Exception {
      addUser(name, password);
   }

   private void addUser(final String name, final String password)
            throws Exception {
      Objects.requireNonNull(loggedInUser, "loggedInUser");
      final var addedUser = new User(name, password, Set.of(), true, true, true,
               true);
      final var encoded = objectMapper.writeValueAsString(addedUser);
      worldCore.performRequest(post("/api/user")
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON).with(user(loggedInUser))
               .with(csrf()).content(encoded));
   }

   @Then("can get the list of users")
   public void can_get_the_list_of_users() {
      try {
         getUsers();
      } catch (final Exception e) {
         throw new AssertionFailedError("Can request users resource", e);
      }
      try {
         getResponseAsUserList();
      } catch (final IOException e) {
         throw new AssertionFailedError("Can decode response", e);
      }
   }

   private void getResponseAsUserList() throws IOException {
      final var response = worldCore.getResponseBodyAsString();
      responseUserList = objectMapper.readValue(response,
               new TypeReference<List<User>>() {
               });
   }

   @When("getting the users")
   public void getting_the_users() throws Exception {
      getUsers();
   }

   private void getUsers() throws Exception {
      Objects.requireNonNull(loggedInUser, "loggedInUser");
      worldCore.performRequest(
               get("/api/user").accept(MediaType.APPLICATION_JSON)
                        .with(user(loggedInUser)).with(csrf()));
   }

   @Given("logged in")
   public void logged_in() {
      Objects.requireNonNull(user, "user");
      loggedInUser = user;
   }

   @Then("MC accepts the addition")
   public void mc_accepts_the_addition() throws Exception {
      worldCore.expectResponse(status().isCreated());
   }

   @Then("MC accepts the login")
   public void mc_accepts_the_login() throws Exception {
      assertAll(() -> worldCore.expectResponse(status().isFound()),
               () -> worldCore
                        .expectResponse(header().string("Location", "/")));
   }

   @Then("MC serves the users page")
   public void mc_serves_users_page() throws Exception {
      worldCore.responseIsOk();
   }

   @Then("the list of users has at least one user")
   public void the_list_of_users_has_one_user() {
      assertThat(responseUserList, not(empty()));
   }

   @Then("the list of users includes a user named {string}")
   public void the_list_of_users_includes_a_user_named(final String name) {
      Objects.requireNonNull(responseUserList, "user list");
      assertTrue(responseUserList.stream()
               .filter(u -> u.getUsername().equals(name)).count() == 1);
   }

   @Then("the response is a list of users")
   public void the_response_message_is_a_list_of_users() {
      try {
         getResponseAsUserList();
      } catch (final IOException e) {
         throw new AssertionFailedError("Can decode response", e);
      }
   }

   @When("user does not have the {string} role")
   public void user_does_not_have_role(final String role) {
      final Set<Authority> authorities = Set.of();
      user_has_authorities(authorities);
   }

   private void user_has_authorities(final Set<Authority> authorities) {
      user = new User("Zoe", "password1", authorities, true, true, true, true);
      service.add(user);
   }

   @When("user has the {string} role")
   public void user_has_role(final String role) {
      user_has_authorities(Set.of(Authority.valueOf(role)));
   }
}
