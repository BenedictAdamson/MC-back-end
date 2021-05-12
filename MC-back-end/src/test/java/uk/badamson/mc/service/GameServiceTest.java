package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2019-21.
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.dao.DataAccessException;

import uk.badamson.mc.Game;

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link GameService}
 * interface.
 * </p>
 */
public class GameServiceTest {

   public static void assertInvariants(final GameService service) {
      assertAll("Not null", () -> assertNotNull(service.getClock(), "clock"),
               () -> assertNotNull(service.getScenarioService(),
                        "scenarioService"));
   }

   public static Game create(final GameService service, final UUID scenario)
            throws DataAccessException, NoSuchElementException {
      final Game game;
      try {
         game = service.create(scenario);
      } catch (DataAccessException | NoSuchElementException e) {
         assertInvariants(service);
         throw e;
      }

      assertInvariants(service);
      assertNotNull(game, "Always returns a (non null) game.");// guard
      final var gameId = game.getIdentifier();
      assertAll("The identifier of the returned game",
               () -> assertEquals(scenario, gameId.getScenario(),
                        "has the given scenario"),
               () -> assertThat("has the current time as its creation time.",
                        gameId.getCreated(),
                        /*
                         * because of a clock race hazard, should not compare
                         * for equality
                         */
                        lessThanOrEqualTo(service.getNow())));

      final var subsequentRetrieval = service.getGame(gameId);
      assertThat(
               "The returned game can be retrieved later, using its identifier.",
               subsequentRetrieval.isPresent());

      return game;
   }

   public static Stream<Instant> getCreationTimesOfGamesOfScenario(
            final GameService service, final UUID scenario)
            throws NoSuchElementException {
      final Stream<Instant> times;
      try {
         times = service.getCreationTimesOfGamesOfScenario(scenario);
      } catch (final NoSuchElementException e) {
         assertInvariants(service);
         throw e;
      }

      assertInvariants(service);
      assertNotNull(times, "Always returns a (non null) stream.");// guard
      final var timesList = times.collect(toList());
      final Set<Instant> timesSet;
      try {
         timesSet = timesList.stream().collect(toUnmodifiableSet());
      } catch (final NullPointerException e) {
         throw new AssertionError(
                  "The returned stream will not include a null element", e);
      }
      assertEquals(timesSet.size(), timesList.size(),
               "Does not contain duplicates.");

      return timesList.stream();
   }

   public static Optional<Game> getGame(final GameService service,
            final Game.Identifier id) {
      final var result = service.getGame(id);

      assertInvariants(service);
      assertNotNull(result, "Returns a (non null) optional value.");// guard
      if (result.isPresent()) {
         assertEquals(id, result.get().getIdentifier(), "identifier");
      }
      return result;
   }

   public static Stream<Game.Identifier> getGameIdentifiers(
            final GameService service) {
      final var games = service.getGameIdentifiers();

      assertInvariants(service);
      assertNotNull(games, "Always returns a (non null) stream.");// guard
      final var gamesList = games.collect(toList());
      final Set<Game.Identifier> gamesSet;
      try {
         gamesSet = gamesList.stream().collect(toUnmodifiableSet());
      } catch (final NullPointerException e) {
         throw new AssertionError(
                  "The returned stream will not include a null element", e);
      }
      assertEquals(gamesSet.size(), gamesList.size(),
               "Does not contain duplicates.");

      return gamesList.stream();
   }
}
