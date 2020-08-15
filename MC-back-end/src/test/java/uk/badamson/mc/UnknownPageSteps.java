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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ListBodySpec;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import reactor.core.publisher.Hooks;

/**
 * <p>
 * Definitions of BDD steps for, for features about unknown pages (resources).
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext
public class UnknownPageSteps {

   private static final String SCHEME = "http";

   private static final String HOST = "example.com";

   static {
      Hooks.onOperatorDebug();
   }

   @Autowired
   private ApplicationContext context;

   @Autowired
   private WebTestClient client;

   private URI requestUri;
   private WebTestClient.ResponseSpec response;
   ListBodySpec<Player> responsePlayerList;

   private void getJson(final String path) {
      getResource(path, MediaType.APPLICATION_JSON);
   }

   private void getResource(final String path, final MediaType mediaType) {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(client, "client");
      setRequestUri(path);
      response = client.get().uri(requestUri.getPath()).accept(mediaType)
               .exchange();
   }

   @When("getting the unknown resource at {string}")
   public void getting_the_unknown_resource_at(final String path) {
      getJson(path);
   }

   @Then("MC replies with Forbidden")
   public void mc_replies_with_forbidden() {
      response.expectStatus().isForbidden();
   }

   @Then("MC replies with Not Found")
   public void mc_replies_with_not_found() {
      response.expectStatus().isNotFound();
   }

   @When("modifying the unknown resource with a {string} at {string}")
   public void modifying_the_unknown_resource_with_a(final String verb,
            final String path) {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(client, "client");
      setRequestUri(path);
      final HttpMethod method = HttpMethod.valueOf(verb);
      assert method != null;
      response = client.method(method).uri(requestUri.getPath())
               .contentType(MediaType.APPLICATION_JSON).exchange();
   }

   private void setRequestUri(final String path) {
      final String authority = HOST;
      final String query = null;
      final String fragment = null;
      try {
         requestUri = new URI(SCHEME, authority, path, query, fragment);
      } catch (final URISyntaxException e) {
         throw new IllegalArgumentException(e);
      }
   }

}
