package uk.badamson.mc.rest;
/*
 * © Copyright Benedict Adamson 2021-22.
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.mc.Scenario;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "DTO")
public record ScenarioResponse(
        UUID identifier,
        String title,
        String description,
        List<NamedUUID> characters
) {

    public static ScenarioResponse convertToResponse(@Nonnull Scenario scenario) {
        return new ScenarioResponse(
                scenario.getIdentifier(), scenario.getTitle(), scenario.getDescription(),
                scenario.getCharacters().stream().map(ni -> new NamedUUID(ni.getId(), ni.getTitle())).collect(Collectors.toUnmodifiableList())
        );
    }
}
