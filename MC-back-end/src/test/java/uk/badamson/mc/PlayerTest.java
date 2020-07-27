package uk.badamson.mc;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.lang.NonNull;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link Player} class
 */
public class PlayerTest {

   @Nested
   public class Constructor {

      @Nested
      public class TwoEquivalent {

         @Test
         public void a() {
            test(USERNAME_A, PASSWORD_A, Authority.ALL);
         }

         @Test
         public void b() {
            test(USERNAME_B, PASSWORD_B, Set.of());
         }

         @Test
         public void c() {
            final String password = null;
            test(USERNAME_A, password, Set.of());
         }

         private void test(@NonNull final String username,
                  final String password, final Set<Authority> authorities) {
            final var player1 = new Player(username, password, authorities);
            final var player2 = new Player(new String(username),
                     password == null ? password : new String(password),
                     authorities.isEmpty() ? authorities
                              : EnumSet.copyOf(authorities));

            assertInvariants(player1, player2);
            assertEquals(player1, player2);
         }

      }// class

      @Test
      public void a() {
         constructor(USERNAME_A, PASSWORD_A, Authority.ALL);
      }

      @Test
      public void b() {
         constructor(USERNAME_B, PASSWORD_B, Set.of());
      }

      @Test
      public void c() {
         final String password = null;
         constructor(USERNAME_A, password, Set.of());
      }

   }// class

   private static final String USERNAME_A = "John";

   private static final String USERNAME_B = "Alan";

   private static final String PASSWORD_A = "letmein";

   private static final String PASSWORD_B = "password123";

   public static void assertInvariants(final Player player) {
      UserDetailsTest.assertInvariants(player);
      assertEquals(player, player, "An object is always equivalent to itself.");
   }

   public static void assertInvariants(final Player player1,
            final Player player2) {
      UserDetailsTest.assertInvariants(player1, player2);
      final boolean equals = player1.equals(player2);
      assertTrue(!(equals && !player2.equals(player1)),
               "Equality is symmetric");
   }

   private static Player constructor(final String username,
            final String password, final Set<Authority> authorities) {
      final var player = new Player(username, password, authorities);

      assertInvariants(player);
      assertSame(username, player.getUsername(),
               "The username of this player is the given username.");
      assertSame(password, player.getPassword(),
               "The password of this player is the given password.");
      assertEquals(authorities, player.getAuthorities(),
               "The authorities granted to this player are equal to the given authorities..");

      return player;
   }
}
