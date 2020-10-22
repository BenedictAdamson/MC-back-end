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

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.badamson.mc.NamedUUID;
import uk.badamson.mc.Scenario;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link ScenarioServiceImpl} class.
 * </p>
 */
public class ScenarioServiceImplTest {

   @Nested
   public class GetScenario {

      @Test
      public void absent() {
         final var service = new ScenarioServiceImpl();
         final var ids = getIds(service);
         var id = UUID.randomUUID();
         while (ids.contains(id)) {
            id = UUID.randomUUID();
         }

         final var result = getScenario(service, id);

         assertTrue(result.isEmpty(), "absent");
      }

      private Set<UUID> getIds(final ScenarioService service) {
         return service.getScenarioIdentifiers().map(si -> si.getId())
                  .collect(toUnmodifiableSet());
      }

      @Test
      public void present() {
         final var service = new ScenarioServiceImpl();
         final var id = getIds(service).stream().findAny().get();

         final var result = getScenario(service, id);

         assertTrue(result.isPresent(), "present");
      }
   }// class

   public static void assertInvariants(final ScenarioServiceImpl service) {
      ScenarioServiceTest.assertInvariants(service);// inherited
   }

   public static Optional<Scenario> getScenario(
            final ScenarioServiceImpl service, final UUID id) {
      final var result = ScenarioServiceTest.getScenario(service, id);

      assertInvariants(service);
      return result;
   }

   public static Stream<NamedUUID> getScenarioIdentifiers(
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
