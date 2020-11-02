package uk.badamson.mc;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import uk.badamson.mc.service.GameService;
import uk.badamson.mc.service.ScenarioService;

/**
 * <p>
 * Definitions of BDD steps, for features about games.
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class GameSteps {

   private static String createGamePath(final Game.Identifier identifier) {
      return "/api/scenario/" + identifier.getScenario() + "/game/"
               + identifier.getCreated();
   }

   @Autowired
   private BackEndWorldCore worldCore;

   @Autowired
   private GameService gameService;

   @Autowired
   private ScenarioService scenarioService;

   @Autowired
   private ObjectMapper objectMapper;

   private Scenario scenario;

   private Set<Instant> gameCreationTimes;

   private Game.Identifier gameId;

   private Game responseGame;

   @Then("The game page includes the scenario description")
   public void game_page_includes_scenario_description() {
      // Do nothing
   }

   @Then("The game page includes the scenario title")
   public void game_page_includes_scenario_title() {
      assertEquals(scenario.getIdentifier(),
               responseGame.getIdentifier().getScenario(), "scenario ID");
   }

   @Then("The game page includes the date and time that the game was set up")
   public void game_page_includes_timestamp() {
      assertEquals(gameId.getCreated(),
               responseGame.getIdentifier().getCreated());
   }

   @Then("MC serves the game page")
   public void mc_serves_game_page() {
      final var response = worldCore.getResponse();
      try {
         response.andExpect(status().isOk());
         final var responseText = worldCore.getResponseBodyAsString();
         responseGame = objectMapper.readValue(responseText, Game.class);
      } catch (final Exception e) {
         throw new AssertionFailedError("HTTP response provides a game", e);
      }
   }

   @When("Navigate to one game of the scenario")
   public void navigate_to_game_of_scenario() throws Exception {
      final var scenarioId = scenario.getIdentifier();
      final var created = gameCreationTimes.stream().findAny().get();
      gameId = new Game.Identifier(scenarioId, created);

      worldCore.getJson(createGamePath(gameId));
   }

   @When("A scenario has games")
   public void scenario_has_games() {
      final var scenarioId = scenarioService.getScenarioIdentifiers().findAny()
               .get();
      scenario = scenarioService.getScenario(scenarioId).get();
      gameService.create(scenarioId);
   }

   @When("Viewing the games of the scenario")
   public void viewing_games_of_scenario() {
      Objects.requireNonNull(scenario, "scenario");
      gameCreationTimes = gameService
               .getCreationTimesOfGamesOfScenario(scenario.getIdentifier())
               .collect(toUnmodifiableSet());
   }

}
