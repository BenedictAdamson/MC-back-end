package uk.badamson.mc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

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
public class BackEndWorldCore {

   private static final String SCHEME = "http";

   private static final String HOST = "example.com";

   private static String asJsonString(final Object obj) {
      try {
         return new ObjectMapper().writeValueAsString(obj);
      } catch (final Exception e) {
         throw new RuntimeException(e);
      }
   }

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
   private WebApplicationContext context;

   private MockMvc mockMvc;

   private ResultActions response;

   public void exchange(final RequestBuilder request) throws Exception {
      response = mockMvc.perform(request);
   }

   public void getHtml(final String path) throws Exception {
      getResource(path, MediaType.TEXT_HTML);
   }

   public void getJson(final String path) throws Exception {
      getResource(path, MediaType.APPLICATION_JSON);
   }

   void getResource(final String path, final MediaType mediaType)
            throws Exception {
      Objects.requireNonNull(context, "context");
      final var uri = createRequestUri(path);
      exchange(MockMvcRequestBuilders.get(uri).accept(mediaType));
   }

   public ResultActions getResponse() {
      return response;
   }

   public void postResource(final String path, final Object body)
            throws Exception {
      Objects.requireNonNull(context, "context");
      final var uri = createRequestUri(path);
      final var request = MockMvcRequestBuilders.post(uri.getPath())
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON).content(asJsonString(body));
      exchange(request);
   }

   @Before
   public void prepareScenario() {
      mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
   }

   public void responseIsOk() throws Exception {
      response.andExpect(MockMvcResultMatchers.status().isOk());
   }
}
