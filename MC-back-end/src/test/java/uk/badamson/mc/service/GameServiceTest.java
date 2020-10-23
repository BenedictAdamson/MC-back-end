package uk.badamson.mc.service;
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;
import java.util.stream.Stream;

import uk.badamson.mc.Game;

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link GameService}
 * interface.
 * </p>
 */
public class GameServiceTest {

   public static void assertInvariants(final GameService service) {
      // Do nothing
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
      final var gamesSet = gamesList.stream().collect(toUnmodifiableSet());
      assertEquals(gamesSet.size(), gamesList.size(),
               "Does not contain duplicates.");

      return gamesList.stream();
   }
}
