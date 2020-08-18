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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link ServiceImpl} class.
 * </p>
 */
public class ServiceImplTest {

   @Nested
   public class Constructor {

      @Test
      public void a() {
         constructor();
      }
   }// class

   public static void assertInvariants(final ServiceImpl service) {
      ServiceTest.assertInvariants(service);// inherited
   }

   private static ServiceImpl constructor() {
      final var service = new ServiceImpl();

      assertInvariants(service);

      return service;
   }
}
