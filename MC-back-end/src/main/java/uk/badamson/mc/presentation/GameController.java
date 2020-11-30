package uk.badamson.mc.presentation;
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

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.Scenario;
import uk.badamson.mc.service.GameService;

/**
 * <p>
 * End-points for the game HTTP resources.
 * </p>
 */
@RestController
public class GameController {

   /**
    * <p>
    * The format of URI paths for {@linkplain #createPathFor(Identifier) game
    * resources}.
    * </p>
    */
   public static final String GAME_PATH_PATTERN = "/api/scenario/{scenario}/game/{created:.+}";

   /**
    * <p>
    * The format of URI paths for {@linkplain #createPathForGames(UUID) games
    * collection resources}.
    * </p>
    */
   public static final String GAMES_PATH_PATTERN = "/api/scenario/{scenario}/game";

   /**
    * <p>
    * The format of time-stamps when used as parts of paths of URIs used by this
    * controller.
    * </p>
    */
   public static final DateTimeFormatter URI_DATETIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

   /**
    * <p>
    * Create a valid path for a game resource for a game that has a given
    * identifier.
    * </p>
    * <p>
    * The created value is consistent with the path used for
    * {@link #getGame(UUID, Instant)}.
    * </p>
    *
    *
    * @param id
    *           The identifier of the game
    * @return The path.
    * @throws NullPointerException
    *            If {@code id} is null.
    */
   public static String createPathFor(final Game.Identifier id) {
      Objects.requireNonNull(id, "id");
      return createPathForGames(id.getScenario())
               + URI_DATETIME_FORMATTER.format(id.getCreated());
   }

   /**
    * <p>
    * Create a valid path for the games collection resource for a scenario with
    * a given identifier.
    * </p>
    * <p>
    * The created value is consistent with the path used for
    * {@link #create(UUID)} and {@link #getCreationTimes(UUID)}.
    * </p>
    *
    * @param scenario
    *           The identifier of the scenario
    * @return The path.
    * @throws NullPointerException
    *            If {@code scenario} is null.
    */
   public static String createPathForGames(final UUID scenario) {
      return ScenarioController.createPathFor(scenario) + "/game/";
   }

   private final GameService gameService;

   /**
    * <p>
    * Construct a controller.
    * </p>
    *
    * @param gameService
    *           The part of the service layer instance that this uses.
    * @throws NullPointerException
    *            If {@code scenarioService} is null.
    */
   @Autowired
   public GameController(@Nonnull final GameService gameService) {
      this.gameService = Objects.requireNonNull(gameService, "gameService");
   }

   /**
    * <p>
    * Create a game for a given scenario.
    * </p>
    * <p>
    * The behaviour of the POST verb for a games collection resource.
    * </p>
    * <ul>
    * <li>Creates a new game for the given scenario.</li>
    * <li>Returns a redirect to the newly created game. That is, a response with
    * <ul>
    * <li>A {@linkplain ResponseEntity#getStatusCode() status code} of
    * {@linkplain HttpStatus#FOUND 302 (Found)}</li>
    * <li>A {@linkplain HttpHeaders#getLocation()
    * Location}{@linkplain ResponseEntity#getHeaders() header} giving the
    * {@linkplain #createPathFor(Identifier) path} of the cew game.</li>
    * </ul>
    * </li>
    * <li>The scenario ID part of the identifier of the newly created game is
    * equal to the given scenario identifier.</li>
    * </ul>
    *
    * @param scenario
    *           The unique ID of the scenario for which to create a game.
    * @return The response.
    * @throws NullPointerException
    *            If {@code scenario} is null.
    * @throws ResponseStatusException
    *            <ul>
    *            <li>With a {@linkplain ResponseStatusException#getStatus()
    *            status} of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if
    *            there is no scenario with the given {@code scenario} ID
    *            {@linkplain UUID#equals(Object) equivalent to} its
    *            {@linkplain Scenario#getIdentifier() identifier}.</li>
    *            <li>With a {@linkplain ResponseStatusException#getStatus()
    *            status} of {@linkplain HttpStatus#INTERNAL_SERVER_ERROR 500
    *            (Internal Server Error)} if there is data access error.</li>
    *            </ul>
    */
   @PostMapping(GAMES_PATH_PATTERN)
   @Nonnull
   @RolesAllowed("MANAGE_GAMES")
   public ResponseEntity<Void> create(
            @Nonnull @PathVariable("scenario") final UUID scenario) {
      try {
         final var game = gameService.create(scenario);

         final var location = URI.create(createPathFor(game.getIdentifier()));
         final var headers = new HttpHeaders();
         headers.setLocation(location);
         return new ResponseEntity<>(headers, HttpStatus.FOUND);
      } catch (final NoSuchElementException e) {
         throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                  "unrecognized ID", e);
      } catch (final DataAccessException e) {
         // Hard to test
         throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                  e.getMessage(), e);
      }
   }

   /**
    * <p>
    * Get the creation times of the games for a given scenario, formatted in a
    * manner suitable for use in URIs used by this controller.
    * </p>
    * <p>
    * The behaviour of the GET verb for a games collection resource.
    * </p>
    * <ul>
    * <li>Always returns a (non null) collection.</li>
    * <li>The returned collection has no null elements.</li>
    * <li>The returned collection has no duplicate elements.</li>
    * <li>The returned collection contains strings
    * {@linkplain #URI_DATETIME_FORMATTER suitably formatted for use as URI
    * paths of this controller}.</li>
    * </ul>
    *
    * @param scenario
    *           The unique ID of the scenario for which to create a game.
    * @return The response.
    * @throws NullPointerException
    *            If {@code scenario} is null.
    * @throws ResponseStatusException
    *            With a {@linkplain ResponseStatusException#getStatus() status}
    *            of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if there
    *            is no scenario with the given {@code scenario} ID
    *            {@linkplain UUID#equals(Object) equivalent to} its
    *            {@linkplain Scenario#getIdentifier() identifier}.
    */
   @GetMapping(GAMES_PATH_PATTERN)
   @Nonnull
   public Stream<String> getCreationTimes(
            @Nonnull @PathVariable("scenario") final UUID scenario) {
      try {
         return gameService.getCreationTimesOfGamesOfScenario(scenario)
                  .map(t -> URI_DATETIME_FORMATTER.format(t));
      } catch (final NoSuchElementException e) {
         throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                  "unrecognized ID", e);
      }
   }

   /**
    * <p>
    * Behaviour of the GET verb for a game resource.
    * </p>
    * <ul>
    * <li>Returns a (non null) game.</li>
    * <li>The {@linkplain Identifier#getScenario() scenario identifier} of the
    * {@linkplain Game#getIdentifier() identification information} of the game
    * {@linkplain UUID#equals(Object) is equivalent to} the given scenario
    * ID</li>
    * </ul>
    *
    * @param scenario
    *           The unique ID of the scenario of the wanted game.
    * @param created
    *           The creation time of the wanted game.
    * @return The response.
    * @throws NullPointerException
    *            <ul>
    *            <li>If {@code scenario} is null.</li>
    *            <li>If {@code created} is null.</li>
    *            </ul>
    * @throws ResponseStatusException
    *            With a {@linkplain ResponseStatusException#getStatus() status}
    *            of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if there
    *            is no game that has {@linkplain Game#getIdentifier()
    *            identification information} equivalent to the given
    *            {@code scenario} and {@code created}.
    */
   @GetMapping(GAME_PATH_PATTERN)
   @Nonnull
   public Game getGame(@Nonnull @PathVariable("scenario") final UUID scenario,
            @Nonnull @PathVariable("created") final Instant created) {
      final var id = new Game.Identifier(scenario, created);
      try {
         return gameService.getGame(id).get();
      } catch (final NoSuchElementException e) {
         throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                  "unrecognized IDs", e);
      }
   }

   /**
    * <p>
    * The part of the service layer instance that this uses.
    * </p>
    * <ul>
    * <li>Always associates with a (non null) service.</li>
    * </ul>
    *
    * @return the service
    */
   @Nonnull
   public final GameService getGameService() {
      return gameService;
   }

   /**
    * <p>
    * Behaviour of the PUT verb for a game resource.
    * </p>
    * <ul>
    * <li>Subsequent retrieval of the game with the given identification
    * information will get the given new game state.</li>
    * </ul>
    *
    * @param scenario
    *           The unique ID of the scenario of the game to modify.
    * @param created
    *           The creation time of the game to modify.
    * @param newGameState
    *           The new state for the game.
    * @throws NullPointerException
    *            <ul>
    *            <li>If {@code scenario} is null.</li>
    *            <li>If {@code created} is null.</li>
    *            <li>If {@code newGameState} is null.</li>
    *            </ul>
    * @throws ResponseStatusException
    *            <ul>
    *            <li>With a {@linkplain ResponseStatusException#getStatus()
    *            status} of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if
    *            there is no game that has {@linkplain Game#getIdentifier()
    *            identification information} equivalent to the given
    *            {@code scenario} and {@code created}.</li>
    *            <li>With a {@linkplain ResponseStatusException#getStatus()
    *            status} of {@linkplain HttpStatus#PRECONDITION_FAILED 412
    *            (Precondition Failed)} if
    *            <ul>
    *            <li>the {@linkplain Game#getIdentifier() identification
    *            information} of the given {@code newGameState} is inconsistent
    *            with the given {@code scenario} and {@code created}.</li>
    *            </ul>
    *            </li>
    *            </ul>
    */
   @PutMapping(GAME_PATH_PATTERN)
   @RolesAllowed("MANAGE_GAMES")
   public void modify(@Nonnull @PathVariable("scenario") final UUID scenario,
            @Nonnull @PathVariable("created") final Instant created,
            @Nonnull @RequestBody final Game newGameState) {
      final var identifier = new Game.Identifier(scenario, created);
      try {
         final var game0 = gameService.getGame(identifier).get();
         if (!identifier.equals(newGameState.getIdentifier())) {
            throw new IllegalArgumentException("Inconsistent identifier");
         }
      } catch (final NoSuchElementException e) {
         throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                  "unrecognized ID", e);
      } catch (final IllegalStateException | IllegalArgumentException e) {
         throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                  e.getMessage(), e);
      }
   }
}
