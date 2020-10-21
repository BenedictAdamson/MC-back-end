package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2020.
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import uk.badamson.mc.Scenario;

public class ScenarioServiceImpl implements ScenarioService {

   // TODO have useful scenarios.
   private static final Scenario SCENARIO = new Scenario(
            new Scenario.Identifier(UUID.randomUUID(), "Section assault"),
            "Basic fire and movement tactics.", List.of()) {
   };
   private static final Map<Scenario.Identifier, Scenario> SCENARIOS = Map
            .of(SCENARIO.getIdentifier(), SCENARIO);

   @Override
   @Nonnull
   public Optional<Scenario> getScenario(@Nonnull final UUID id) {
      Objects.requireNonNull(id, "id");
      return SCENARIOS.values().stream()
               .filter(s -> id.equals(s.getIdentifier().getId())).findAny();
   }

   @Override
   @Nonnull
   public Stream<Scenario.Identifier> getScenarioIdentifiers() {
      return SCENARIOS.keySet().stream();
   }

}
