package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2020,22.
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

import org.springframework.stereotype.Service;

import uk.badamson.mc.NamedUUID;
import uk.badamson.mc.Scenario;

/**
 * <p>
 * Implementation of the part of the service layer pertaining to scenarios of
 * the Mission Command game.
 * </p>
 */
@Service
public class ScenarioSpringService implements ScenarioService {

   // TODO have useful scenarios.
   private static final Scenario SCENARIO = new Scenario(UUID.randomUUID(),
            "Section assault", "Basic fire and movement tactics.",
            List.of(new NamedUUID(UUID.randomUUID(), "Lt. Winters"),
                     new NamedUUID(UUID.randomUUID(), "Sgt. Summer"))) {
   };
   private static final Map<UUID, Scenario> SCENARIOS = Map
            .of(SCENARIO.getIdentifier(), SCENARIO);

   @Override
   @Nonnull
   public Stream<NamedUUID> getNamedScenarioIdentifiers() {
      return SCENARIOS.values().stream().map(Scenario::getNamedUUID);
   }

   @Override
   @Nonnull
   public Optional<Scenario> getScenario(@Nonnull final UUID id) {
      Objects.requireNonNull(id, "id");
      return Optional.ofNullable(SCENARIOS.get(id));
   }

   @Override
   @Nonnull
   public Stream<UUID> getScenarioIdentifiers() {
      return SCENARIOS.keySet().stream();
   }

}
