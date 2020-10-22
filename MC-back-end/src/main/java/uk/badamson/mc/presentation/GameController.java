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
import java.time.format.DateTimeFormatter;
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
import uk.badamson.mc.service.ScenarioService;

/**
 * <p>
 * End-points for the game HTTP resources.
 * </p>
 */
@RestController
public class GameController {

   private static final DateTimeFormatter URI_DATETIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

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
   static String createPathFor(final Game.Identifier id) {
      Objects.requireNonNull(id, "id");
      return "/api/scenario/" + id.getScenario() + "/game/"
               + URI_DATETIME_FORMATTER.format(id.getCreated());
   }

   private final ScenarioService scenarioService;

   /**
    * <p>
    * Construct a controller.
    * </p>
    *
    * @param scenarioService
    *           The part of the service layer instance that this uses.
    * @throws NullPointerException
    *            If {@code scenarioService} is null.
    */
   @Autowired
   public GameController(@Nonnull final ScenarioService scenarioService) {
      this.scenarioService = Objects.requireNonNull(scenarioService,
               "scenarioService");
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
    *            <li>If {@code scenario} is created.</li></li>
    * @throws ResponseStatusException
    *            With a {@linkplain ResponseStatusException#getStatus() status}
    *            of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if there
    *            is no game that has {@linkplain Game#getIdentifier()
    *            identification information} equivalent to the given
    *            {@code scenario} and {@code created}.
    */
   @GetMapping("/api/scenario/{scenario}/game/{created:.+}")
   @Nonnull
   public Game getGame(@Nonnull @PathVariable("scenario") final UUID scenario,
            @Nonnull @PathVariable("created") final Instant created) {
      Objects.requireNonNull(scenario, "scenario");
      Objects.requireNonNull(created, "created");
      try {
         return scenarioService.getScenario(scenario).get().getGames().stream()
                  .filter(g -> g.getIdentifier().getCreated().equals(created))
                  .findAny().get();
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
   public final ScenarioService getScenarioService() {
      return scenarioService;
   }

}
