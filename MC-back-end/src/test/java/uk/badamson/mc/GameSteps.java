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
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.UriTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import uk.badamson.mc.presentation.GameController;
import uk.badamson.mc.presentation.GamePlayersController;
import uk.badamson.mc.service.GamePlayersService;
import uk.badamson.mc.service.GameService;
import uk.badamson.mc.service.ScenarioService;
import uk.badamson.mc.service.UserService;

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

   private static final UriTemplate GAME_PLAYERS_PATH_URI_TEMPLATE = new UriTemplate(
            GamePlayersController.GAME_PLAYERS_PATH_PATTERN);

   private static Game.Identifier parseGamePath(final String path) {
      return parseGamePath(path, GAME_PATH_URI_TEMPLATE);
   }

   private static Game.Identifier parseGamePath(final String path,
            final UriTemplate template) {
      Objects.requireNonNull(path, "path");
      Objects.requireNonNull(template, "template");
      final var pathVariable = template.match(path);
      try {
         final var scenarioId = UUID.fromString(pathVariable.get("scenario"));
         final var created = Instant.parse(pathVariable.get("created"));
         return new Game.Identifier(scenarioId, created);
      } catch (final RuntimeException e) {
         throw new IllegalArgumentException("Path " + path, e);
      }
   }

   private static Game.Identifier parseGamePlayersPath(final String path) {
      return parseGamePath(path, GAME_PLAYERS_PATH_URI_TEMPLATE);
   }

   @Autowired
   private BackEndWorld world;

   @Autowired
   private ScenarioService scenarioService;

   @Autowired
   private GameService gameService;

   @Autowired
   private GamePlayersService gamePlayersService;

   @Autowired
   private UserService userService;

   @Autowired
   private ObjectMapper objectMapper;

   private Scenario scenario;

   private Set<Instant> gameCreationTimes;

   private Game.Identifier gameId;

   private Game.Identifier joinedGameId;

   private Game game;

   private GamePlayers gamePlayers;

   private Boolean mayJoinGame;

   private void assertGamePagePlayersInvariants() {
      Objects.requireNonNull(gamePlayers, "gamePlayers");
      Objects.requireNonNull(world.loggedInUser, "loggedInUser");

      final var currentUserId = world.loggedInUser.getId();
      final var users = gamePlayers.getUsers();
      final var playing = gamePlayersService.getCurrentGameOfUser(currentUserId)
               .filter(currentGame -> currentGame.equals(gameId)).isPresent();
      assertAll("Collection of players of characters",
               () -> assertThat("has", users, anything()),
               () -> assertThat("lists self if playing", users.values(),
                        playing ? hasItem(currentUserId) : anything()));
   }

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
      updateGameCreationTimes();
   }

   private void createGame() throws Exception {
      Objects.requireNonNull(scenario, "scenario");
      Objects.requireNonNull(world.loggedInUser, "loggedInUser");

      final var path = GameController
               .createPathForGames(scenario.getIdentifier());
      world.performRequest(
               post(path).with(user(world.loggedInUser)).with(csrf()));
      updateGameCreationTimes();
   }

   @When("creating a game")
   public void creating_game() throws Exception {
      chooseScenario();
      createGame();
   }

   @Then("The current-game page indicates that the current-game is the game joined")
   public void current_game_page_indicates_current_game_is_game_joined() {
      assertThat("game ID is the ID of the joined game", gameId,
               is(joinedGameId));
   }

   @When("examining a game recruiting players")
   public void examining_game_recruiting_players() {
      prepareNewGame();
   }

   private <TYPE> TYPE expectEncodedResponse(final ResultActions response,
            final Class<? extends TYPE> clazz) throws Exception {
      try {
         response.andExpect(status().isOk());
         final var responseText = response.andReturn().getResponse()
                  .getContentAsString();
         return objectMapper.readValue(responseText, clazz);
      } catch (final Exception e) {
         throw new AssertionFailedError(
                  "Expected OK response encoding a " + clazz.getSimpleName(),
                  e);
      }
   }

   @Then("The game page does not indicate which characters are played by which \\(other) users")
   public void game_page_does_not_indicate_which_characters_are_played_by_which_other_users() {
      Objects.requireNonNull(gamePlayers, "gamePlayers");
      Objects.requireNonNull(world.loggedInUser, "loggedInUser");

      final var users = gamePlayers.getUsers().values();
      final var otherUsers = users.stream()
               .filter(u -> !u.equals(world.loggedInUser.getId()))
               .collect(toUnmodifiableSet());
      assertThat("No other users listed as users", otherUsers, empty());
   }

   @Then("The game page includes the scenario description")
   public void game_page_includes_scenario_description() {
      // Do nothing
   }

   @Then("The game page includes the scenario title")
   public void game_page_includes_scenario_title() {
      assertEquals(scenario.getIdentifier(), game.getIdentifier().getScenario(),
               "scenario ID");
   }

   @Then("The game page includes the date and time that the game was set up")
   public void game_page_includes_timestamp() {
      assertEquals(gameId.getCreated(), game.getIdentifier().getCreated());
   }

   @Then("The game page indicates that the game has no players")
   public void game_page_indicates_game_has_no_players() {
      Objects.requireNonNull(gamePlayers, "gamePlayers");
      assertThat(gamePlayers.getUsers().values(), empty());
   }

   @Then("the game page indicates that the game is recruiting players")
   public void game_page_indicates_game_recuiting_players() {
      Objects.requireNonNull(gamePlayers, "gamePlayers");
      assertTrue(gamePlayers.isRecruiting());
   }

   @Then("The game page indicates the number of players of the game")
   public void game_page_indicates_number_of_players_of_game() {
      Objects.requireNonNull(gamePlayers, "gamePlayers");
      assertThat(gamePlayers.getUsers().size(), anything());
   }

   @Then("The game page indicates that the game has a player")
   public void game_page_indicates_that_game_has_player() {
      assertThat("Game players list not empty", gamePlayers.getUsers().values(),
               not(empty()));
   }

   @Then("the game page indicates that the game is not recruiting players")
   public void game_page_indicates_that_game_is_not_recuiting_players() {
      Objects.requireNonNull(gamePlayers, "gamePlayers");
      assertFalse(gamePlayers.isRecruiting(), "game is not recruiting players");
   }

   @Then("The game page indicates that the user is not playing the game")
   public void game_page_indicates_user_is_not_playing_game() {
      Objects.requireNonNull(world.loggedInUser, "loggedInUser");
      Objects.requireNonNull(gamePlayers, "gamePlayers");

      final var userId = world.loggedInUser.getId();
      assertThat("User is not listed as a player",
               gamePlayers.getUsers().values(), not(hasItem(userId)));
   }

   @Then("The game page indicates that the user is playing the game")
   public void game_page_indicates_user_is_playing_game() {
      Objects.requireNonNull(world.loggedInUser, "loggedInUser");
      Objects.requireNonNull(gamePlayers, "gamePlayers");

      final var userId = world.loggedInUser.getId();
      assertThat("User is listed as a player", gamePlayers.getUsers().values(),
               hasItem(userId));
   }

   @Then("the game page indicates that the user may join the game")
   public void game_page_indicates_user_may_join_game() {
      assertTrue(mayJoinGame, "may join game");
   }

   @Then("The game page indicates that the user may not join the game")
   public void game_page_indicates_user_may_not_join_game() {
      assertFalse(mayJoinGame, "may not join game");
   }

   @Then("The game page indicates whether the game has players")
   public void game_page_indicates_whether_game_has_players() {
      Objects.requireNonNull(gamePlayers, "gamePlayers");
      assertThat(gamePlayers.getUsers().size(), anything());
   }

   @Then("The game page indicates whether the game is recruiting players")
   public void game_page_indicates_whether_recuiting_players() {
      Objects.requireNonNull(gamePlayers, "gamePlayers");
      assertThat(gamePlayers.isRecruiting(), anything());
   }

   @Then("The game page indicates whether the user is playing the game")
   public void game_page_indicates_whether_user_is_playing_game() {
      Objects.requireNonNull(world.loggedInUser, "loggedInUser");
      Objects.requireNonNull(gamePlayers, "gamePlayers");

      assertThat("Has a collection of users for the game",
               gamePlayers.getUsers().values(), anything());
   }

   @Then("The game page indicates whether the user may join the game")
   public void game_page_indicates_whether_user_may_join_game() {
      assertThat(mayJoinGame.booleanValue(), anything());
   }

   @Then("The game page indicates which character \\(if any) the user is playing")
   public void game_page_indicates_which_character_user_is_playing() {
      Objects.requireNonNull(gamePlayers, "gamePlayers");
      Objects.requireNonNull(world.loggedInUser, "loggedInUser");

      final var currentUserId = world.loggedInUser.getId();
      final var gameManager = world.loggedInUser.getAuthorities()
               .contains(Authority.ROLE_MANAGE_GAMES);
      final var expectedUsers = gamePlayersService
               .getGamePlayersAsGameManager(gameId).get().getUsers().values();
      final var allOtherUsers = expectedUsers.stream()
               .filter(user -> !user.equals(currentUserId))
               .collect(toUnmodifiableSet());
      final var otherUsers = gamePlayers.getUsers().values().stream()
               .filter(user -> !user.equals(currentUserId))
               .collect(toUnmodifiableSet());
      assertAll(() -> assertGamePagePlayersInvariants(), () -> assertThat(
               "Collection of players of characters lists other users only if game manager",
               otherUsers, gameManager ? is(allOtherUsers) : empty()));
   }

   @Then("The game page indicates which characters are played by which users")
   public void game_page_indicates_which_characters_are_played_by_which_users() {
      Objects.requireNonNull(gamePlayers, "gamePlayers");

      assertGamePagePlayersInvariants();
   }

   private void getGamePage() throws AssertionFailedError {
      Objects.requireNonNull(gameId, "gameId");
      try {
         requestGetGame();
         game = expectEncodedResponse(world.getResponse(), Game.class);
      } catch (final Exception e) {
         throw new AssertionFailedError("Can GET Game resource", e);
      }

      try {
         requestGetGamePlayers();
         if (isPlayerOrGameManager()) {
            gamePlayers = expectEncodedResponse(world.getResponse(),
                     GamePlayers.class);
         } else {
            world.getResponse().andExpect(status().is4xxClientError());
            gamePlayers = null;
         }
      } catch (final Exception e) {
         throw new AssertionFailedError(
                  "Correct behaviour for GET GamePlayers resource", e);
      }
      try {
         requestGetMayUserJoinGame();
         if (isPlayer()) {
            mayJoinGame = expectEncodedResponse(world.getResponse(),
                     Boolean.class);
         } else {
            world.getResponse().andExpect(status().is4xxClientError());
            mayJoinGame = Boolean.FALSE;
         }
      } catch (final Exception e) {
         throw new AssertionFailedError(
                  "Correct behaviour for GET MayJoinGame resource", e);
      }
   }

   private void getGames() throws Exception {
      Objects.requireNonNull(scenario, "scenario");
      final var path = GameController
               .createPathForGames(scenario.getIdentifier());
      world.performRequest(get(path).accept(MediaType.APPLICATION_JSON));
   }

   private void getResponseAsGameCreationTimes() throws IOException {
      final var response = world.getResponseBodyAsString();
      gameCreationTimes = objectMapper.readValue(response,
               new TypeReference<Set<Instant>>() {
               });
   }

   private boolean hasAnyAuthorities(final Set<Authority> options) {
      if (options.isEmpty()) {
         return true;
      } else if (world.loggedInUser == null) {
         return false;
      } else {
         return world.loggedInUser.getAuthorities().stream()
                  .map(a -> options.contains(a)).filter(present -> present)
                  .findAny().isPresent();
      }
   }

   private boolean isPlayer() {
      return hasAnyAuthorities(EnumSet.of(Authority.ROLE_PLAYER));
   }

   private boolean isPlayerOrGameManager() {
      return hasAnyAuthorities(
               EnumSet.of(Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES));
   }

   private void joinGame() throws Exception {
      Objects.requireNonNull(gameId, "gameId");
      Objects.requireNonNull(world.loggedInUser, "loggedInUser");

      final var path = GamePlayersController.createPathForJoining(gameId);
      var request = post(path).with(csrf());
      request = request.with(user(world.loggedInUser));
      world.performRequest(request);
      joinedGameId = gameId;
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

      final var location = world.getResponse().andReturn().getResponse()
               .getHeader("Location");
      assertAll(() -> world.expectResponse(status().isFound()),
               () -> assertNotNull(location, "has Location header"));// guard
      gameId = parseGamePath(location);
      final var indicatedGame = gameService.getGame(gameId);
      assertAll(
               () -> assertEquals(scenario.getIdentifier(),
                        gameId.getScenario(),
                        "Location is for a game of the given scenario"),
               () -> assertTrue(indicatedGame.isPresent(),
                        "identified game exists"));// guard
      game = indicatedGame.get();
   }

   @Then("MC accepts ending recruitment for the game")
   public void mc_accepts_ending_recruitment_for_game() throws Exception {
      world.getResponse().andExpect(status().isFound());
   }

   @Then("MC accepts joining the game")
   public void mc_accepts_joining_game() {
      Objects.requireNonNull(gameId, "gameId");
      Objects.requireNonNull(world.loggedInUser, "loggedInUser");

      final var location = world.getResponse().andReturn().getResponse()
               .getHeader("Location");
      gamePlayers = gamePlayersService.getGamePlayersAsGameManager(gameId)
               .get();
      assertAll(() -> world.expectResponse(status().isFound()),
               () -> assertNotNull(location, "has Location header")); // guard
      final var gameRedirectedTo = parseGamePlayersPath(location);
      assertThat("Redirected to the list of players of the current game",
               gameRedirectedTo, is(gameId));
   }

   @Then("MC does not allow creating a game")
   public void mc_does_not_allow_creating_game() throws Exception {
      chooseScenario();
      createGame();
      world.getResponse().andExpect(status().is4xxClientError());
   }

   @Then("MC does not allow ending recruitment for the game")
   public void mc_does_not_allow_ending_recuitment_for_game() throws Exception {
      chooseScenario();
      requestEndRecruitmentForGame();
      world.getResponse().andExpect(status().is4xxClientError());
   }

   @Then("MC provides a game page")
   public void mc_provides_game_page() {
      getGamePage();
   }

   @When("navigate to the current-game page")
   public void navigate_current_game() {
      try {
         requestGetCurrentGame();
         final var response = world.getResponse();
         try {
            final var location = response.andReturn().getResponse()
                     .getHeader("Location");
            assertAll(
                     () -> world.expectResponse(status().isTemporaryRedirect()),
                     () -> assertNotNull(location, "has Location header"));// guard
            gameId = parseGamePath(location);
         } catch (final Exception e) {
            throw new AssertionFailedError("Expected redirect to a game page");
         }
         getGamePage();
      } catch (final Exception e) {
         throw new AssertionFailedError("Can navigate to current-game resource",
                  e);
      }
   }

   @When("Navigate to one game page")
   public void navigate_to_one_game_page() {
      Objects.requireNonNull(scenario, "scenario");
      Objects.requireNonNull(gameCreationTimes, "gameCreationTimes");

      final var creationTime = gameCreationTimes.stream().findAny().get();
      gameId = new Game.Identifier(scenario.getIdentifier(), creationTime);

      if (gamePlayersService.getGamePlayersAsGameManager(gameId).get()
               .isRecruiting()) {
         // Tough test: the current user is playing the game
         final var currentUser = world.loggedInUser;
         if (currentUser != null
                  && currentUser.getAuthorities()
                           .contains(Authority.ROLE_PLAYER)
                  && gamePlayersService
                           .getCurrentGameOfUser(currentUser.getId())
                           .isEmpty()) {
            gamePlayersService.userJoinsGame(currentUser.getId(), gameId);
         }
      }
      if (gamePlayersService.getGamePlayersAsGameManager(gameId).get()
               .isRecruiting()) {
         // Tough test: the game has other player
         final var otherUser = userService.add(new BasicUserDetails(
                  "Other Player", "password2",
                  EnumSet.of(Authority.ROLE_PLAYER), true, true, true, true));
         gamePlayersService.userJoinsGame(otherUser.getId(), gameId);
      }
   }

   private void prepareNewGame() {
      chooseScenario();
      game = gameService.create(scenario.getIdentifier());
      gameId = game.getIdentifier();
      gamePlayers = gamePlayersService.getGamePlayersAsGameManager(gameId)
               .get();
      mayJoinGame = null;
      BackEndWorld.require(gamePlayers.isRecruiting(), "game is recruiting");
   }

   private void requestEndRecruitmentForGame() throws Exception {
      Objects.requireNonNull(gamePlayers, "gamePlayers");
      Objects.requireNonNull(gameId, "gameId");

      final var path = GamePlayersController
               .createPathForEndRecruitmentOf(gameId);
      var request = post(path).with(csrf());
      if (world.loggedInUser != null) {
         request = request.with(user(world.loggedInUser));
      }
      world.performRequest(request);
   }

   private void requestGetCurrentGame() throws Exception {
      final var path = GamePlayersController.CURRENT_GAME_PATH;
      var request = get(path).accept(MediaType.APPLICATION_JSON);
      if (world.loggedInUser != null) {
         request = request.with(user(world.loggedInUser));
      }
      world.performRequest(request);
   }

   private void requestGetGame() throws Exception {
      Objects.requireNonNull(gameId, "gameId");

      final var path = GameController.createPathFor(gameId);
      var request = get(path).accept(MediaType.APPLICATION_JSON);
      if (world.loggedInUser != null) {
         request = request.with(user(world.loggedInUser));
      }
      world.performRequest(request);
   }

   private void requestGetGamePlayers() throws Exception {
      Objects.requireNonNull(gameId, "gameId");

      final var path = GamePlayersController.createPathForGamePlayersOf(gameId);
      var request = get(path).accept(MediaType.APPLICATION_JSON);
      if (world.loggedInUser != null) {
         request = request.with(user(world.loggedInUser));
      }
      world.performRequest(request);
   }

   private void requestGetMayUserJoinGame() throws Exception {
      Objects.requireNonNull(gameId, "gameId");
      final var path = GamePlayersController
               .createPathForMayJoinQueryOf(gameId);
      var request = get(path);
      if (world.loggedInUser != null) {
         request = request.with(user(world.loggedInUser));
      }
      world.performRequest(request);
   }

   @When("A scenario has games")
   public void scenario_has_games() {
      chooseScenario();
      gameService.create(scenario.getIdentifier());
      updateGameCreationTimes();
   }

   private void updateGameCreationTimes() {
      gameCreationTimes = gameService
               .getCreationTimesOfGamesOfScenario(scenario.getIdentifier())
               .collect(toUnmodifiableSet());
   }

   @When("user ends recruitment for the game")
   public void user_ends_recuitment_for_game() {
      if (!isPlayerOrGameManager()) {
         throw new IllegalStateException("user not authorized");
      }
      try {
         requestEndRecruitmentForGame();
      } catch (final Exception e) {
         throw new AssertionFailedError("Can ask the server to change the game",
                  e);
      }
      gamePlayers = null;// local copy is out of date
   }

   @When("the user joins the game")
   public void user_joins_game() throws Exception {
      joinGame();
   }

   @Given("user is playing a game")
   public void user_playing_game() throws Exception {
      prepareNewGame();
      joinGame();
   }

   @Given("viewing a game that is recruiting players")
   public void viewing_game_recruiting_players() {
      prepareNewGame();
   }

   @Given("Viewing the games of the scenario")
   public void viewing_games_of_scenario() {
      Objects.requireNonNull(scenario, "scenario");
      Objects.requireNonNull(gameCreationTimes, "gameCreationTimes");
   }
}
