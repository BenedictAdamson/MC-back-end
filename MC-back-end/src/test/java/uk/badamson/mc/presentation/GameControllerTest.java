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

import java.time.Instant;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.badamson.mc.Game;
import uk.badamson.mc.TestConfiguration;
import uk.badamson.mc.service.ScenarioService;

/**
 * <p>
 * Unit tests of the {@link GameController} class.
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
public class GameControllerTest {

   @Autowired
   ScenarioService scenarioService;

   @Autowired
   private ObjectMapper objectMapper;

   @Autowired
   private MockMvc mockMvc;

   private ResultActions getGame(final Game.Identifier id) throws Exception {
      final var request = get(GameController.createPathFor(id))
               .accept(MediaType.APPLICATION_JSON);

      final var response = mockMvc.perform(request);
      return response;
   }

   @Test
   public void getGame_absent() throws Exception {
      final var scenarios = scenarioService.getScenarioIdentifiers()
               .map(si -> si.getId()).collect(toUnmodifiableSet());
      var scenario = UUID.randomUUID();
      while (scenarios.contains(scenario)) {
         scenario = UUID.randomUUID();
      }
      final var id = new Game.Identifier(scenario, Instant.now());

      final var response = getGame(id);

      response.andExpect(status().isNotFound());
   }

   @Test
   public void getGame_present() throws Exception {
      final var scenarioId = scenarioService.getScenarioIdentifiers()
               .map(si -> si.getId()).findAny().get();
      final var scenario = scenarioService.getScenario(scenarioId).get();
      final var gameId = scenario.getGames().stream().findAny().get()
               .getIdentifier();

      final var response = getGame(gameId);

      response.andExpect(status().isOk());
      final var jsonResponse = response.andReturn().getResponse()
               .getContentAsString();
      final var game = objectMapper.readValue(jsonResponse, Game.class);
      assertEquals(gameId, game.getIdentifier(), "game has the requested ID");
   }
}
