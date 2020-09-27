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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.Before;
import io.cucumber.spring.ScenarioScope;

/**
 * <p>
 * Shared code and basic SUT objects (the core of the test world) for BDD steps
 * for the Cucumber-JVM BDD testing tool
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ScenarioScope
@AutoConfigureMockMvc
public class WorldCore {

   public static String encodeAsJson(final Object obj) {
      try {
         final ObjectMapper mapper = new ObjectMapper();
         final String jsonContent = mapper.writeValueAsString(obj);
         System.out.println(jsonContent);
         return jsonContent;
      } catch (final Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Autowired
   private WebApplicationContext context;

   private MockMvc mockMvc;

   private ResultActions response;

   public void exchangeJson(final HttpMethod method, final String path)
            throws Exception {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(mockMvc, "mockMvc");
      final var uri = URI.create(path);
      performRequest(request(method, uri));
   }

   public void expectResponse(final ResultMatcher expectation)
            throws Exception {
      response.andExpect(expectation);
   }

   public void getHtml(final String path) throws Exception {
      getResource(path, MediaType.TEXT_HTML);
   }

   public void getJson(final String path) throws Exception {
      getResource(path, MediaType.APPLICATION_JSON);
   }

   private void getResource(final String path, final MediaType mediaType)
            throws Exception {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(mockMvc, "mockMvc");
      performRequest(get(path).accept(mediaType));
   }

   public ResultActions getResponse() {
      return response;
   }

   public String getResponseBodyAsString() throws UnsupportedEncodingException {
      return response.andReturn().getResponse().getContentAsString();
   }

   public void performRequest(final RequestBuilder requestBuilder)
            throws Exception {
      response = mockMvc.perform(requestBuilder);
   }

   public void postResource(final String path, final Object body)
            throws Exception {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(mockMvc, "mockMvc");
      final var encodedBody = encodeAsJson(body);
      performRequest(post(path).contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON).content(encodedBody));
   }

   public void responseIsOk() throws Exception {
      expectResponse(status().isOk());
   }

   @Before
   public void setUp() {
      mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
   }
}
