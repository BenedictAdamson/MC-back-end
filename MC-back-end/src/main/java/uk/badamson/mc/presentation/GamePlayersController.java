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

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.GamePlayers;
import uk.badamson.mc.service.GamePlayersService;

/**
 * <p>
 * End-points for the game players HTTP resources.
 * </p>
 */
@RestController
public class GamePlayersController {

   /**
    * <p>
    * The format of URI paths for
    * {@linkplain #createPathForGamePlayersOf(Identifier) game player
    * resources}.
    * </p>
    */
   public static final String GAME_PLAYERS_PATH_PATTERN = "/api/scenario/{scenario}/game/{created}/players";

   /**
    * <p>
    * Create a valid path for a game players resource for a game that has a
    * given identifier.
    * </p>
    * <p>
    * The created value is consistent with the path used for
    * {@link #getGamePlayers(UUID, Instant)}.
    * </p>
    *
    *
    * @param id
    *           The identifier of the game
    * @return The path.
    * @throws NullPointerException
    *            If {@code id} is null.
    */
   public static String createPathForGamePlayersOf(final Game.Identifier id) {
      return GameController.createPathFor(id) + "/players";
   }

   private final GamePlayersService gamePlayersService;

   /**
    * <p>
    * Construct a controller.
    * </p>
    *
    * @param gamePlayersService
    *           The part of the service layer instance that this uses.
    * @throws NullPointerException
    *            If {@code gamePlayersService} is null.
    */
   @Autowired
   public GamePlayersController(
            @Nonnull final GamePlayersService gamePlayersService) {
      this.gamePlayersService = Objects.requireNonNull(gamePlayersService,
               "gamePlayersService");
   }

   /**
    * <p>
    * Behaviour of the GET verb for a game players resource.
    * </p>
    * <ul>
    * <li>Returns a (non null) game players container.</li>
    * <li>The {@linkplain Identifier#getScenario() scenario identifier} of the
    * {@linkplain GamePlayers#getGame() game identifier} of the game players
    * container {@linkplain UUID#equals(Object) is equivalent to} the given
    * scenario ID</li>
    * <li>The {@linkplain Identifier#getCreated() creation time} of the
    * {@linkplain GamePlayers#getGame() game identifier} of the game players
    * container {@linkplain Instant#equals(Object) is equivalent to} the given
    * creation time</li>
    * </ul>
    *
    * @param scenario
    *           The unique ID of the scenario of the game.
    * @param created
    *           The creation time of the game.
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
   @GetMapping(GAME_PLAYERS_PATH_PATTERN)
   @Nonnull
   public Game getGamePlayers(
            @Nonnull @PathVariable("scenario") final UUID scenario,
            @Nonnull @PathVariable("created") final Instant created) {
      new Game.Identifier(scenario, created);
      try {
         return null; // FIXME return gamePlayersService.getGame(id).get();
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
   public final GamePlayersService getGamePlayersService() {
      return gamePlayersService;
   }
}
