package uk.badamson.mc.repository;
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

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.badamson.mc.Game;

import javax.annotation.Nonnull;

@Document(collection="game")
public record GameDTO(
        @Id
        GameIdentifierDTO identifier,
        RunStateDTO runState
) {
    @Nonnull
    static GameDTO convertToDTO(@Nonnull Game.Identifier id, @Nonnull Game game) {
        return new GameDTO(GameIdentifierDTO.convertToDTO(id), RunStateDTO.convertToDTO(game.getRunState()));
    }

    @Nonnull
    static Game convertFromDTO(@Nonnull GameDTO dto) {
        return new Game(GameIdentifierDTO.convertFromDTO(dto.identifier()), RunStateDTO.convertFromDTO(dto.runState()));
    }

    public enum RunStateDTO {
        WAITING_TO_START, RUNNING, STOPPED;

        @Nonnull
        static RunStateDTO convertToDTO(@Nonnull Game.RunState runState) {
            return valueOf(runState.toString());
        }

        @Nonnull
        static Game.RunState convertFromDTO(@Nonnull RunStateDTO dto) {
            return Game.RunState.valueOf(dto.toString());
        }
    }
}
