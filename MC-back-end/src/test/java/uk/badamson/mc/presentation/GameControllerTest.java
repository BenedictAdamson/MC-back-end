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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

import uk.badamson.mc.Game;
import uk.badamson.mc.TestConfiguration;
import uk.badamson.mc.repository.GameRepository;
import uk.badamson.mc.service.GameService;
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

   private static final TypeReference<List<Instant>> INSTANT_LIST = new TypeReference<>() {
   };

   @Autowired
   GameRepository gameRepository;

   @Autowired
   ScenarioService scenarioService;

   @Autowired
   GameService gameService;

   @Autowired
   private ObjectMapper objectMapper;

   @Autowired
   private MockMvc mockMvc;

   private ResultActions create(final UUID scenario) throws Exception {
      final var request = post(GameController.createPathForGames(scenario));

      final var response = mockMvc.perform(request);
      return response;
   }

   @Test
   public void create_knowScenario() throws Exception {
      final var scenario = scenarioService.getScenarioIdentifiers().findAny()
               .get();

      final var response = create(scenario);

      final var id = gameService.getGameIdentifiers()
               .filter(gi -> scenario.equals(gi.getScenario())).findAny();
      assertTrue(id.isPresent(), "created a game for the scenario");
      response.andExpect(status().isFound());
      final var location = response.andReturn().getResponse()
               .getHeaderValue("Location");
      assertEquals(GameController.createPathFor(id.get()), location,
               "redirection location");
   }

   @Test
   public void create_unknowScenario() throws Exception {
      final var scenario = UUID.randomUUID();

      final var response = create(scenario);

      response.andExpect(status().isNotFound());
   }

   private ResultActions getGame(final Game.Identifier id) throws Exception {
      final var request = get(GameController.createPathFor(id))
               .accept(MediaType.APPLICATION_JSON);

      final var response = mockMvc.perform(request);
      return response;
   }

   @Test
   public void getGame_absent() throws Exception {
      final var id = new Game.Identifier(UUID.randomUUID(), Instant.now());

      final var response = getGame(id);

      response.andExpect(status().isNotFound());
   }

   @Test
   public void getGame_present() throws Exception {
      final var id = new Game.Identifier(UUID.randomUUID(), Instant.now());
      gameRepository.save(new Game(id));

      final var response = getGame(id);

      response.andExpect(status().isOk());
      final var jsonResponse = response.andReturn().getResponse()
               .getContentAsString();
      final var game = objectMapper.readValue(jsonResponse, Game.class);
      assertEquals(id, game.getIdentifier(), "game has the requested ID");
   }

   private ResultActions getGames(final UUID scenario) throws Exception {
      final var path = GameController.createPathForGames(scenario);
      final var request = get(path).accept(MediaType.APPLICATION_JSON);

      return mockMvc.perform(request);
   }

   @Test
   public void getGames_0() throws Exception {
      final var scenario = scenarioService.getScenarioIdentifiers().findAny()
               .get();

      final var response = getGames(scenario);

      response.andExpect(status().isOk());
      final var jsonResponse = response.andReturn().getResponse()
               .getContentAsString();
      final var creationTimes = objectMapper.readValue(jsonResponse,
               INSTANT_LIST);
      assertThat("creation times", creationTimes, empty());
   }

   @Test
   public void getGames_1() throws Exception {
      final var scenario = scenarioService.getScenarioIdentifiers().findAny()
               .get();
      final var created = gameService.create(scenario).getIdentifier()
               .getCreated();

      final var response = getGames(scenario);

      response.andExpect(status().isOk());
      final var jsonResponse = response.andReturn().getResponse()
               .getContentAsString();
      assertThat("Creation time is in ISO format", jsonResponse,
               containsString(created.toString()));
      final var creationTimes = objectMapper.readValue(jsonResponse,
               INSTANT_LIST);
      assertEquals(List.of(created), creationTimes, "creation times");
   }

   @Test
   public void getGames_absent() throws Exception {
      final var scenario = UUID.randomUUID();

      final var response = getGames(scenario);

      response.andExpect(status().isNotFound());
   }
}
