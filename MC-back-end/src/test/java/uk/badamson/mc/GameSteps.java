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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import uk.badamson.mc.presentation.GameController;
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

   private static final UriTemplate GAME_PATH_URI_TEMPLATE = new UriTemplate(
            GameController.GAME_PATH_PATTERN);

   private static String createGamePath(final Game.Identifier identifier) {
      return "/api/scenario/" + identifier.getScenario() + "/game/"
               + identifier.getCreated();
   }

   private static Game.Identifier parseGamePath(final String path) {
      Objects.requireNonNull(path, "path");
      final var pathVariable = GAME_PATH_URI_TEMPLATE.match(path);
      try {
         final var scenarioId = UUID.fromString(pathVariable.get("scenario"));
         final var created = Instant.parse(pathVariable.get("created"));
         return new Game.Identifier(scenarioId, created);
      } catch (final RuntimeException e) {
         throw new IllegalArgumentException("Path " + path, e);
      }
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

   @Then("can get the list of games")
   public void can_get_list_of_games() {
      try {
         getGames();
      } catch (final Exception e) {
         throw new AssertionFailedError("Can request games resource", e);
      }
      try {
         getResponseAsGameCreationTimes();
      } catch (final IOException e) {
         throw new AssertionFailedError("Can decode response", e);
      }
   }

   private void chooseScenario() {
      final var scenarioId = scenarioService.getScenarioIdentifiers().findAny()
               .get();
      scenario = scenarioService.getScenario(scenarioId).get();
   }

   private void createGame() throws Exception {
      Objects.requireNonNull(scenario, "scenario");
      Objects.requireNonNull(worldCore.loggedInUser, "loggedInUser");

      final var path = GameController
               .createPathForGames(scenario.getIdentifier());
      worldCore.performRequest(
               post(path).with(user(worldCore.loggedInUser)).with(csrf()));
   }

   @When("creating a game")
   public void creating_game() throws Exception {
      chooseScenario();
      createGame();
   }

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

   private void getGames() throws Exception {
      Objects.requireNonNull(scenario, "scenario");
      final var path = GameController
               .createPathForGames(scenario.getIdentifier());
      worldCore.performRequest(get(path).accept(MediaType.APPLICATION_JSON));
   }

   private void getResponseAsGameCreationTimes() throws IOException {
      final var response = worldCore.getResponseBodyAsString();
      gameCreationTimes = objectMapper.readValue(response,
               new TypeReference<Set<Instant>>() {
               });
   }

   @Then("the list of games includes the new game")
   public void list_of_games_includes_new_game() {
      Objects.requireNonNull(gameCreationTimes, "gameCreationTimes");
      Objects.requireNonNull(gameId, "gameId");

      assertThat(gameCreationTimes, hasItem(gameId.getCreated()));
   }

   @Then("MC accepts the creation of the game")
   public void mc_accepts_creation_of_game() {
      Objects.requireNonNull(scenario, "scenario");

      final var location = worldCore.getResponse().andReturn().getResponse()
               .getHeader("Location");
      assertAll(() -> worldCore.expectResponse(status().isFound()),
               () -> assertNotNull(location, "has Location header"));// guard
      gameId = parseGamePath(location);
      assertEquals(scenario.getIdentifier(), gameId.getScenario(),
               "Location is for a game of the given scenario");
      assertTrue(gameService.getGame(gameId).isPresent(),
               "identified game exists");
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
      chooseScenario();
      gameService.create(scenario.getIdentifier());
   }

   @When("Viewing the games of the scenario")
   public void viewing_games_of_scenario() {
      Objects.requireNonNull(scenario, "scenario");
      gameCreationTimes = gameService
               .getCreationTimesOfGamesOfScenario(scenario.getIdentifier())
               .collect(toUnmodifiableSet());
   }
}
