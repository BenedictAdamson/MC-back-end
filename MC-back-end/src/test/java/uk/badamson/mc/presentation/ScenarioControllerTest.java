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

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.badamson.mc.Scenario;
import uk.badamson.mc.TestConfiguration;
import uk.badamson.mc.service.ScenarioService;

/**
 * <p>
 * Unit tests of the {@link ScenarioController} class.
 * <p>
 * <p>
 * We can not use JUnit 5 {@link Nested} test classes because
 * {@link SpringBootTest} does not work properly with them; in particular the
 * {@link DirtiesContext} annotation is ignored on nested tests.
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ScenarioControllerTest {

   @Autowired
   private MockMvc mockMvc;

   @Autowired
   private ScenarioService service;
   
   @Autowired
   private ObjectMapper objectMapper;

   @Test
   public void getAll() throws Exception {
      final var request = get("/api/scenario")
               .accept(MediaType.APPLICATION_JSON);

      final var response = mockMvc.perform(request);

      response.andExpect(status().isOk());
      final var jsonResponse = response.andReturn().getResponse()
               .getContentAsString();
      objectMapper.readValue(jsonResponse,
               new TypeReference<List<Scenario.Identifier>>() {
               });
   }

   private ResultActions getScenario(final UUID id) throws Exception {
      final var request = get("/api/scenario/" + id)
               .accept(MediaType.APPLICATION_JSON);

      final var response = mockMvc.perform(request);
      return response;
   }

   @Test
   public void getScenario_absent() throws Exception {
      final var ids = service.getScenarioIdentifiers().map(si -> si.getId())
               .collect(toUnmodifiableSet());
      var id = UUID.randomUUID();
      while (ids.contains(id)) {
         id = UUID.randomUUID();
      }

      final var response = getScenario(id);

      response.andExpect(status().isNotFound());
   }

   @Test
   public void getScenario_present() throws Exception {
      final var id = service.getScenarioIdentifiers().map(si -> si.getId())
               .findAny().get();

      final var response = getScenario(id);

      response.andExpect(status().isOk());
      final var jsonResponse = response.andReturn().getResponse()
               .getContentAsString();
      final var scenario = objectMapper.readValue(jsonResponse,
               Scenario.class);
      assertEquals(id, scenario.getIdentifier().getId(),
               "scenario has the requested ID");
   }
}
