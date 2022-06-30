package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2020-22.
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

import com.fasterxml.jackson.annotation.JsonFormat;
import uk.badamson.mc.Game;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.UUID;

public record GameIdentifierResponse(
        UUID scenario,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant created
) {
    @Nonnull
    public static Game.Identifier convertFromResponse(@Nonnull GameIdentifierResponse dto) {
        return new Game.Identifier(dto.scenario(), dto.created());
    }

    @Nonnull
    public static GameIdentifierResponse convertToResponse(@Nonnull Game.Identifier id) {
        return new GameIdentifierResponse(id.getScenario(), id.getCreated());
    }
}
