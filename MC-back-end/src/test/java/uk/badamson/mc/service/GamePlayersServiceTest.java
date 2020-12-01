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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;

import uk.badamson.mc.Game;
import uk.badamson.mc.GamePlayers;

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link GamePlayersService}
 * interface.
 * </p>
 */
public class GamePlayersServiceTest {

   public static void assertInvariants(final GamePlayersService service) {
      assertNotNull(service.getGameService(), "Not null, gameService");
   }

   public static Optional<GamePlayers> endRecruitment(
            final GamePlayersService service, final Game.Identifier id) {
      final var result = service.endRecruitment(id);

      assertInvariants(service);
      assertNotNull(result, "Returns a (non null) optional value.");// guard
      if (result.isPresent()) {
         final var gamePlayers = result.get();
         assertAll(() -> assertEquals(id, gamePlayers.getGame(), "game"),
                  () -> assertFalse(gamePlayers.isRecruiting(), "recruiting"));
         assertFalse(service.getGamePlayers(id).get().isRecruiting(),
                  "Subsequent retrieval of game players using an identifier equivalent to the given ID returns "
                           + "a value that is also not recruiting.");
      }
      return result;
   }

   public static Optional<GamePlayers> getGamePlayers(
            final GamePlayersService service, final Game.Identifier id) {
      final var result = service.getGamePlayers(id);

      assertInvariants(service);
      assertNotNull(result, "Returns a (non null) optional value.");// guard
      if (result.isPresent()) {
         assertEquals(id, result.get().getGame(), "game");
      }
      return result;
   }

}
