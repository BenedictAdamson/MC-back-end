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

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ListBodySpec;
import org.springframework.test.web.reactive.server.WebTestClient.RequestHeadersSpec;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.reactive.function.BodyInserters;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import reactor.core.publisher.Hooks;
import uk.badamson.mc.repository.UserRepository;
import uk.badamson.mc.service.Service;

/**
 * <p>
 * Definitions of BDD steps, for features about users.
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext
public class UserSteps {

   static {
      Hooks.onOperatorDebug();
   }

   @Autowired
   private WorldCore worldCore;

   @Autowired
   private WebTestClient client;

   @Autowired
   private Service service;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private PasswordEncoder passwordEncoder;

   private WebTestClient.ResponseSpec response;
   ListBodySpec<User> responseUserList;

   @When("adding a user named {string} with  password {string}")
   public void adding_a_user_named(final String name, final String password) {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(password, "password");
      postResource("/api/user", new User(name, password, Set.of()));
   }

   @Before
   public void beginScenario() {
   }

   @Then("can get the list of users")
   public void can_get_the_list_of_users() {
      getJson("/api/user");
      responseIsOk();
      responseUserList = response.expectBodyList(User.class);
   }

   public void exchange(final RequestHeadersSpec<?> request) {
      response = request.exchange();
   }

   private void getHtml(final String path) {
      getResource(path, MediaType.TEXT_HTML);
   }

   private void getJson(final String path) {
      getResource(path, MediaType.APPLICATION_JSON);
   }

   private void getResource(final String path, final MediaType mediaType) {
      Objects.requireNonNull(client, "client");
      exchange(client.get().uri(path).accept(mediaType));
   }

   @When("getting the users")
   public void getting_the_users() {
      worldCore.getJson("/api/user");
   }

   @When("log in as {string} using password {string}")
   public void log_in_as_using_password(final String user,
            final String password) {
      Objects.requireNonNull(user, "user");
      Objects.requireNonNull(password, "password");
      Objects.requireNonNull(client, "client");

      exchange(client.post().uri("/login")
               .contentType(MediaType.APPLICATION_FORM_URLENCODED)
               .body(BodyInserters.fromFormData("username", user)
                        .with("password", password)));
   }

   @Given("logged in as {string}")
   public void logged_in_as(final String name) {
      mutateClientWith(mockUser(name));
   }

   @Then("MC accepts the addition")
   public void mc_accepts_the_addition() {
      response.expectStatus().isCreated();
   }

   @Then("MC accepts the login")
   public void mc_accepts_the_login() {
      response.expectStatus().isFound().expectHeader().valueEquals("Location",
               "/");
   }

   @Then("MC forbids the request")
   public void mc_forbids_the_request() {
      response.expectStatus().isForbidden();
   }

   @Then("MC serves the resource")
   public void mc_serves_the_users_resource() {
      worldCore.responseIsOk();
   }

   private void mutateClientWith(final WebTestClientConfigurer configurer) {
      client = client.mutateWith(configurer);
   }

   private void postResource(final String path, final Object body) {
      Objects.requireNonNull(client, "client");
      final var request = client.post().uri(path)
               .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
               .accept(MediaType.APPLICATION_JSON);
      exchange(request);
   }

   @Given("presenting a valid CSRF token")
   public void presenting_a_valid_CSRF_token() {
      mutateClientWith(csrf());
   }

   private void responseIsOk() {
      response.expectStatus().isOk();
   }

   @Given("that user {string} exists with  password {string}")
   public void that_user_exists_with_password(final String user,
            final String password) {
      Objects.requireNonNull(user, "user");
      Objects.requireNonNull(password, "password");
      Objects.requireNonNull(userRepository, "userRepository");
      userRepository.save(
               new User(user, passwordEncoder.encode(password), Set.of()))
               .block();
   }

   @Then("the list of users has one user")
   public void the_list_of_users_has_one_user() {
      responseUserList.hasSize(1);
   }

   @Then("the list of users includes a user named {string}")
   public void the_list_of_users_includes_a_user_named(final String name) {
      assertNotNull(responseUserList, "user list");
      responseUserList.contains(new User(name, null, Set.of()));
   }

   @Then("the list of users includes the administrator")
   public void the_list_of_users_includes_the_administrator() {
      responseUserList.value(
               users -> users.stream()
                        .filter(user -> user.ADMINISTRATOR_USERNAME
                                 .equals(user.getUsername()))
                        .count(),
               is(1L));
   }

   @When("the potential user gives the DNS name to a web browser")
   public void the_potential_user_gives_the_DNS_name_to_a_web_browser() {
      final String path = null;
      getHtml(path);
   }

   @Then("the response message is a list of users")
   public void the_response_message_is_a_list_of_users() {
      responseUserList = worldCore.getResponse().expectBodyList(User.class);
   }

   @Given("user authenticated as Administrator")
   public void user_authenticated_as_Administrator() {
      final UserDetails administrator = service
               .findByUsername(user.ADMINISTRATOR_USERNAME).block();
      assert administrator != null;
      mutateClientWith(mockUser(administrator));
   }

}
