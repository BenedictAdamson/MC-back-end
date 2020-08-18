package uk.badamson.mc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.RequestHeadersSpec;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;

import io.cucumber.java.Before;
import io.cucumber.spring.ScenarioScope;

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

/**
 * <p>
 * Shared code and basic SUT objects (the core of the test world) for BDD steps
 * for the Cucumber-JVM BDD testing tool
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ScenarioScope
public class WorldCore {

   private static final String SCHEME = "http";

   private static final String HOST = "example.com";

   public static URI createRequestUri(final String path) {
      final String authority = HOST;
      final String query = null;
      final String fragment = null;
      try {
         return new URI(SCHEME, authority, path, query, fragment);
      } catch (final URISyntaxException e) {
         throw new IllegalArgumentException(e);
      }
   }

   @Autowired
   private ApplicationContext context;

   private WebTestClient client;

   private WebTestClient.ResponseSpec response;

   public void exchange(final RequestHeadersSpec<?> request) {
      response = request.exchange();
   }

   public WebTestClient getClient() {
      return client;
   }

   public void getHtml(final String path) {
      getResource(path, MediaType.TEXT_HTML);
   }

   public void getJson(final String path) {
      getResource(path, MediaType.APPLICATION_JSON);
   }

   void getResource(final String path, final MediaType mediaType) {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(client, "client");
      final var uri = createRequestUri(path);
      response = client.get().uri(uri.getPath()).accept(mediaType).exchange();
   }

   public WebTestClient.ResponseSpec getResponse() {
      return response;
   }

   public void mutateClientWith(final WebTestClientConfigurer configurer) {
      client = client.mutateWith(configurer);
   }

   public void postResource(final String path, final Object body) {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(client, "client");
      final var uri = createRequestUri(path);
      final var request = client.post().uri(uri.getPath())
               .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
               .accept(MediaType.APPLICATION_JSON);
      exchange(request);
   }

   @Before
   public void prepareScenario() {
      client = WebTestClient.bindToApplicationContext(context).build();
   }

   public void responseIsOk() {
      response.expectStatus().isOk();
   }
}
