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

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import uk.badamson.mc.Scenario;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link ScenarioServiceImpl} class.
 * </p>
 */
public class ScenarioServiceImplTest {

   public static void assertInvariants(final ScenarioServiceImpl service) {
      ScenarioServiceTest.assertInvariants(service);// inherited
   }

   public static Stream<Scenario.Identifier> getScenarioIdentifiers(
            final ScenarioServiceImpl service) {
      final var scenarios = ScenarioServiceTest.getScenarioIdentifiers(service);// inherited

      assertInvariants(service);

      return scenarios;
   }

   @Test
   public void constructor() {
      final var service = new ScenarioServiceImpl();

      assertInvariants(service);
   }

   @Test
   public void getScenarioIdentifiers() {
      final var service = new ScenarioServiceImpl();
      getScenarioIdentifiers(service);
   }
}
