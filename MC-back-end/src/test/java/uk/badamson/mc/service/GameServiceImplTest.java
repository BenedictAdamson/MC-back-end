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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.badamson.mc.Game;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link GameServiceImpl} class.
 * </p>
 */
public class GameServiceImplTest {

   @Nested
   public class GetGame {

      @Test
      public void absent() {
         final var service = new GameServiceImpl();
         final var id = new Game.Identifier(UUID.randomUUID(), Instant.now());

         final var result = getGame(service, id);

         assertTrue(result.isEmpty(), "absent");
      }

      @Test
      public void present() {
         final var service = new GameServiceImpl();
         final var id = service.getGameIdentifiers().findAny().get();

         final var result = getGame(service, id);

         assertTrue(result.isPresent(), "present");
      }
   }// class

   public static void assertInvariants(final GameServiceImpl service) {
      GameServiceTest.assertInvariants(service);// inherited
   }

   public static Optional<Game> getGame(final GameServiceImpl service,
            final Game.Identifier id) {
      final var result = GameServiceTest.getGame(service, id);

      assertInvariants(service);
      return result;
   }

   public static Stream<Game.Identifier> getGameIdentifiers(
            final GameServiceImpl service) {
      final var games = GameServiceTest.getGameIdentifiers(service);// inherited

      assertInvariants(service);

      return games;
   }

   @Test
   public void constructor() {
      final var service = new GameServiceImpl();

      assertInvariants(service);
   }

   @Test
   public void getGameIdentifiers() {
      final var service = new GameServiceImpl();
      getGameIdentifiers(service);
   }
}
