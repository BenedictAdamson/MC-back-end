package uk.badamson.mc;
/*
 * © Copyright Benedict Adamson 2019.
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * <p>
 * Auxiliary test code for classes implementing the{@link UserDetails}
 * interface.
 * </p>
 */
public class UserDetailsTest {

   public static void assertInvariants(final UserDetails userDetails) {
      assertNotNull(userDetails.getAuthorities(), "Non null, authorities");
      assertNotNull(userDetails.getUsername(), "Non null, username");
   }

   public static void assertInvariants(final UserDetails userDetails1,
            final UserDetails userDetails2) {
      final boolean equals = userDetails1.equals(userDetails2);
      assertTrue(
               !(equals && !userDetails1.getUsername()
                        .equals(userDetails2.getUsername())),
               "Equivalence requires equivalent username values");
   }
}
