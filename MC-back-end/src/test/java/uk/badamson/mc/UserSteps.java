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

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import uk.badamson.mc.presentation.UserController;
import uk.badamson.mc.service.UserService;

/**
 * <p>
 * Definitions of BDD steps, for features about users.
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class UserSteps {

   private static Authority parseRole(final String role) {
      try {
         return Authority.valueOf(
                  "ROLE_" + role.replace(' ', '_').toUpperCase(Locale.ENGLISH));
      } catch (final Exception e) {
         throw new IllegalArgumentException("roleName " + role, e);
      }
   }

   @Autowired
   private BackEndWorld world;

   @Autowired
   private UserService service;

   @Autowired
   private ObjectMapper objectMapper;

   private User expectedUser;

   private User user;

   private List<User> userList;

   @When("adding a user named {string} with  password {string}")
   public void adding_a_user_named(final String name, final String password)
            throws Exception {
      addUser(name, password);
   }

   private void addUser(final String name, final String password)
            throws Exception {
      Objects.requireNonNull(world.loggedInUser, "loggedInUser");
      final var addedUser = new User(UUID.randomUUID(), name, password,
               Set.of(), true, true, true, true);
      final var encoded = objectMapper.writeValueAsString(addedUser);
      world.performRequest(post("/api/user")
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON)
               .with(user(world.loggedInUser)).with(csrf()).content(encoded));
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

   private void getResponseAsUser() throws IOException {
      final var response = world.getResponseBodyAsString();
      user = objectMapper.readValue(response, User.class);
   }

   private void getResponseAsUserList() throws IOException {
      final var response = world.getResponseBodyAsString();
      userList = objectMapper.readValue(response,
               new TypeReference<List<User>>() {
               });
   }

   @When("getting the users")
   public void getting_the_users() throws Exception {
      getUsers();
   }

   private void getUser(final UUID id) throws Exception {
      Objects.requireNonNull(world.loggedInUser, "loggedInUser");
      final var path = UserController.createPathForUser(id);
      world.performRequest(get(path).accept(MediaType.APPLICATION_JSON)
               .with(user(world.loggedInUser)).with(csrf()));
   }

   private void getUsers() throws Exception {
      Objects.requireNonNull(world.loggedInUser, "loggedInUser");
      world.performRequest(get("/api/user").accept(MediaType.APPLICATION_JSON)
               .with(user(world.loggedInUser)).with(csrf()));
   }

   @Given("logged in")
   public void logged_in() {
      Objects.requireNonNull(user, "user");
      world.loggedInUser = user;
   }

   @Then("MC accepts the logout")
   public void mc_accepts_logout() throws Exception {
      world.expectResponse(status().is2xxSuccessful());
   }

   @Then("MC accepts the addition")
   public void mc_accepts_the_addition() throws Exception {
      world.expectResponse(status().isFound());
   }

   @Then("MC accepts the login")
   public void mc_accepts_the_login() throws Exception {
      assertAll(() -> world.expectResponse(status().isFound()),
               () -> world.expectResponse(header().string("Location", "/")));
   }

   @Then("MC does not allow navigating to a user page")
   public void mc_does_not_alllow_navigating_to_user_page() throws Exception {
      Objects.requireNonNull(userList, "userList");

      expectedUser = null;
      getUser(userList.get(0).getId());
      world.getResponse().andExpect(status().isForbidden());
   }

   @Then("MC does not allow adding a user")
   public void mc_does_not_allow_adding_user() throws Exception {
      addUser("Allan", "letmein");
      world.expectResponse(status().isForbidden());
   }

   @Then("MC serves the user page")
   public void mc_serves_user_page() throws Exception {
      world.responseIsOk();
      try {
         getResponseAsUser();
      } catch (final IOException e) {
         throw new AssertionFailedError("Can decode response", e);
      }
   }

   @Then("MC serves the users page")
   public void mc_serves_users_page() throws Exception {
      world.responseIsOk();
   }

   @When("Navigate to one user page")
   public void navigate_to_one_user_page() throws Exception {
      Objects.requireNonNull(userList, "userList");

      expectedUser = userList.get(0);
      getUser(expectedUser.getId());
   }

   @Given("not logged in")
   public void not_logged_in() {
      world.loggedInUser = null;
   }

   @When("request logout")
   public void request_logout() throws Exception {
      world.performRequest(
               post("/logout").with(user(world.loggedInUser)).with(csrf()));
   }

   @Then("the list of users has at least one user")
   public void the_list_of_users_has_one_user() {
      assertThat(userList, not(empty()));
   }

   @Then("the list of users includes a user named {string}")
   public void the_list_of_users_includes_a_user_named(final String name) {
      Objects.requireNonNull(userList, "user list");
      assertTrue(userList.stream().filter(u -> u.getUsername().equals(name))
               .count() == 1);
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
      userHasAuthorities(Set.of());
   }

   @When("user has any role")
   public void user_has_any_role() {
      userHasAuthorities(Set.of(Authority.values()[0]));
   }

   @When("user has the {string} role")
   public void user_has_role(final String role) {
      userHasAuthorities(Set.of(parseRole(role)));
   }

   @When("user has the {string} role but not the {string} role")
   public void user_has_role_but_not_role(final String included,
            final String excluded) {
      final var includedRole = parseRole(included);
      final var excludedRole = parseRole(excluded);
      if (includedRole == excludedRole) {
         throw new IllegalArgumentException("Contradictory role constraints");
      }
      userHasAuthorities(Set.of(includedRole));
   }

   @Then("The user page includes the user name")
   public void user_page_includes_user_name() {
      Objects.requireNonNull(expectedUser, "expectedUser");
      Objects.requireNonNull(user, "user");

      assertEquals(expectedUser.getUsername(), user.getUsername());
   }

   @Then("The user page lists the roles of the user")
   public void user_page_lists_roles_of_user() {
      Objects.requireNonNull(expectedUser, "expectedUser");
      Objects.requireNonNull(user, "user");

      assertEquals(expectedUser.getAuthorities(), user.getAuthorities());
   }

   private void userHasAuthorities(final Set<Authority> authorities) {
      user = new User(UUID.randomUUID(), "Zoe", "password1", authorities, true,
               true, true, true);
      service.add(user);
   }

   @Given("Viewing the list of users")
   public void viewing_list_of_users() {
      userList = service.getUsers().collect(toList());
   }

}
