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

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import uk.badamson.mc.NamedUUID;
import uk.badamson.mc.Scenario;

/**
 * <p>
 * The part of the service layer pertaining to scenarios of the Mission Command
 * game.
 * </p>
 */
public interface ScenarioService {

   /**
    * <p>
    * Retrieve a stream of the {@linkplain NamedUUID named identifiers} of the
    * scenarios of this instance of the Mission Command game.
    * </p>
    */
   @Nonnull
   Stream<NamedUUID> getNamedScenarioIdentifiers();

   /**
    * <p>
    * Retrieve the scenario that has a given unique ID.
    * </p>
    *
    * @param id
    *           The unique ID of the wanted scenario.
    * @return The scenario.
    * @throws NullPointerException
    *            If {@code id} is null.
    */
   @Nonnull
   Optional<Scenario> getScenario(@Nonnull final UUID id);

   @Nonnull
   Stream<UUID> getScenarioIdentifiers();

}
