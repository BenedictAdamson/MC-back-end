package uk.badamson.mc;
/*
 * © Copyright Benedict Adamson 2020-21.
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
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
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.presentation.GameController;
import uk.badamson.mc.presentation.GamePlayersController;
import uk.badamson.mc.service.GameService;
import uk.badamson.mc.service.ScenarioService;

/**
 * <p>
 * Definitions of BDD steps, for features about game scenarios.
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class ScenarioSteps {

   @Autowired
   private BackEndWorld world;

   @Autowired
   private GameService gameService;

   @Autowired
   private ScenarioService scenarioService;

   @Autowired
   private ObjectMapper objectMapper;

   private UUID id;

   private Scenario responseScenario;

   private Identifier chooseGameOfScenario() {
      return gameService.getCreationTimesOfGamesOfScenario(id)
               .map(t -> new Game.Identifier(id, t)).findAny().get();
   }

   private void chooseScenario() {
      id = gameService.getGameIdentifiers().map(game -> game.getScenario())
               .findAny().get();
   }

   @Given("examining scenario")
   public void examining_scenario() {
      chooseScenario();
   }

   private void getResponseAsScenarioIdentifierList() throws IOException {
      final var response = world.getResponseBodyAsString();
      objectMapper.readValue(response, new TypeReference<List<NamedUUID>>() {
      });
   }

   private void getScenarios() throws Exception {
      world.performRequest(
               get("/api/scenario").accept(MediaType.APPLICATION_JSON));
   }

   @When("getting the scenarios")
   public void getting_scenarios() throws Exception {
      getScenarios();
   }

   @When("MC serves the scenario page")
   public void mc_serves_scenario_page() throws Exception {
      final var responseText = world.getResponseBodyAsString();
      responseScenario = objectMapper.readValue(responseText, Scenario.class);
      assertEquals(id, responseScenario.getIdentifier(),
               "scenario has the requested ID");
   }

   @Then("MC serves the scenarios page")
   public void mc_serves_scenarios_page() throws Exception {
      world.responseIsOk();
   }

   @When("Navigate to a scenario with games")
   public void navigate_to_a_scenario_with_games() throws Exception {
      chooseScenario();
      world.performRequest(
               get("/api/scenario/" + id).accept(MediaType.APPLICATION_JSON));
   }

   private void requestGameOfScenario(final Game.Identifier game)
            throws Exception {
      final var path = GameController.createPathFor(game);
      final var request = get(path).accept(MediaType.APPLICATION_JSON);
      if (world.loggedInUser != null) {
         request.with(user(world.loggedInUser));
      }
      world.performRequest(request);
   }

   private void requestGamePlayersOfScenario(final Game.Identifier game)
            throws Exception {
      final var path = GamePlayersController.createPathForGamePlayersOf(game);
      final var request = get(path).accept(MediaType.APPLICATION_JSON);
      if (world.loggedInUser != null) {
         request.with(user(world.loggedInUser));
      }
      world.performRequest(request);
   }

   @Then("the response is a list of scenarios")
   public void response_is_list_of_scenarios() {
      try {
         getResponseAsScenarioIdentifierList();
      } catch (final IOException e) {
         throw new AssertionFailedError("Can decode response", e);
      }
   }

   @Then("The scenario page allows navigation to game pages")
   public void scenario_page_allows_navigation_to_game_pages()
            throws Exception {
      final var game = chooseGameOfScenario();

      /* The game page is rendered using data from two end-points. */
      try {
         requestGameOfScenario(game);
         world.getResponse().andExpect(status().isOk());
      } catch (final AssertionError e) {
         throw new AssertionError("Allows GET of game resource", e);
      }
      try {
         requestGamePlayersOfScenario(game);
         world.getResponse().andExpect(status().isOk());
      } catch (final AssertionError e) {
         throw new AssertionError("Allows GET of game-players resource", e);
      }
   }

   @Then("The scenario page does not allow navigation to game pages")
   public void scenario_page_does_not_allow_navigation_to_game_pages()
            throws Exception {
      final var game = chooseGameOfScenario();

      /* The game page is rendered using data from two end-points. */
      try {
         requestGameOfScenario(game);
         world.getResponse().andExpect(status().is4xxClientError());
      } catch (final AssertionError e) {
         throw new AssertionError("Does not allow GET of game resource", e);
      }
      try {
         requestGamePlayersOfScenario(game);
         world.getResponse().andExpect(status().is4xxClientError());
      } catch (final AssertionError e) {
         throw new AssertionError("Does not allow GET of game resource", e);
      }
   }

   @Then("The scenario page includes the list of games of that scenario")
   public void scenario_page_includes_games() {
      Objects.requireNonNull(id, "id");
      assertNotNull(gameService.getCreationTimesOfGamesOfScenario(id));
   }

   @Then("The scenario page includes the list of playable characters of that scenario")
   public void scenario_page_includes_list_of_playable_characters_of_that_scenario() {
      Objects.requireNonNull(scenarioService, "service");
      Objects.requireNonNull(id, "id");
      Objects.requireNonNull(responseScenario, "responseScenario");

      final var expectedScenario = scenarioService.getScenario(id).get();
      assertThat("characters", responseScenario.getCharacters(),
               is(expectedScenario.getCharacters()));
   }

   @Then("The scenario page includes the scenario description")
   public void scenario_page_includes_scenario_description() {
      Objects.requireNonNull(scenarioService, "service");
      Objects.requireNonNull(id, "id");
      Objects.requireNonNull(responseScenario, "responseScenario");

      final var expectedScenario = scenarioService.getScenario(id).get();
      assertThat("description", responseScenario.getDescription(),
               is(expectedScenario.getDescription()));
   }

   @When("Viewing the scenarios")
   public void viewing_scenarios() {
      scenarioService.getScenarioIdentifiers().collect(toUnmodifiableSet());
   }
}
