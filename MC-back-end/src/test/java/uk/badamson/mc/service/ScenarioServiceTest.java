package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2019-20,22.
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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import uk.badamson.mc.NamedUUID;
import uk.badamson.mc.Scenario;

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link ScenarioService}
 * interface.
 * </p>
 */
public class ScenarioServiceTest {

   public static void assertInvariants(final ScenarioService service) {
      // Do nothing
   }

   public static Stream<NamedUUID> getNamedScenarioIdentifiers(
            final ScenarioService service) {
      final Set<UUID> expectedIdentifiers = service.getScenarioIdentifiers()
               .collect(toUnmodifiableSet());

      final var scenarios = service.getNamedScenarioIdentifiers();

      assertInvariants(service);
      assertNotNull(scenarios, "Always returns a (non null) stream.");// guard
      final var scenariosList = scenarios.collect(toList());
      final Set<NamedUUID> scenariosSet;
      try {
         scenariosSet = scenariosList.stream().collect(toUnmodifiableSet());
      } catch (final NullPointerException e) {
         throw new AssertionError(
                  "The returned stream will not include a null element", e);
      }
      assertEquals(scenariosSet.size(), scenariosList.size(),
               "Does not contain duplicates.");
      final var identifiersOfScenarios = scenariosSet.stream()
               .map(NamedUUID::getId).collect(toUnmodifiableSet());
      assertThat(
               "Contains a named identifier corresponding to each scenario identifier",
               identifiersOfScenarios, is(expectedIdentifiers));

      return scenariosList.stream();
   }

   public static Optional<Scenario> getScenario(final ScenarioService service,
            final UUID id) {
      final var result = service.getScenario(id);

      assertInvariants(service);
      assertNotNull(result, "Returns a (non null) optional value.");// guard
      result.ifPresent(scenario -> assertEquals(id, scenario.getIdentifier(), "identifier"));
      return result;
   }

   public static Stream<UUID> getScenarioIdentifiers(
            final ScenarioService service) {
      final var scenarios = service.getScenarioIdentifiers();

      assertInvariants(service);
      assertNotNull(scenarios, "Always returns a (non null) stream.");// guard
      final var scenariosList = scenarios.collect(toList());
      final Set<UUID> scenariosSet;
      try {
         scenariosSet = scenariosList.stream().collect(toUnmodifiableSet());
      } catch (final NullPointerException e) {
         throw new AssertionError(
                  "The returned stream will not include a null element", e);
      }
      assertEquals(scenariosSet.size(), scenariosList.size(),
               "Does not contain duplicates.");

      return scenariosList.stream();
   }
}
