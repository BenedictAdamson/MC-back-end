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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;

/**
 * <p>
 * End-points for the game HTTP resources.
 * </p>
 */
@RestController
public class GameController {

   /**
    * <p>
    * Construct a controller.
    * </p>
    */
   @Autowired
   public GameController() {
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
   @GetMapping("/api/scenario/{scenario}/game/{created}")
   @Nonnull
   public Game getGame(@Nonnull @PathVariable("scenario") final UUID scenario,
            @Nonnull @PathVariable("created") @DateTimeFormat(
                     iso = DateTimeFormat.ISO.DATE_TIME) final Instant created) {
      Objects.requireNonNull(scenario, "scenario");
      Objects.requireNonNull(created, "created");
      try {
         return null;// FIXME
      } catch (final NoSuchElementException e) {
         throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                  "unrecognized IDs", e);
      }
   }

}
