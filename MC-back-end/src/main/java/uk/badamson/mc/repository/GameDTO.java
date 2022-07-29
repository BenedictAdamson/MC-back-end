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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.badamson.mc.Game;
import uk.badamson.mc.GameIdentifier;

import javax.annotation.Nonnull;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Document(collection = "game")
@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "DTO")
public record GameDTO(
        @Id
        GameIdentifierDTO identifier,
        RunStateDTO runState,
        Boolean recruiting,
        List<PlayedCharacterDTO> users
) {
    @Nonnull
    static GameDTO convertToDTO(@Nonnull GameIdentifier id, @Nonnull Game game) {
        return new GameDTO(
                GameIdentifierDTO.convertToDTO(id),
                RunStateDTO.convertToDTO(game.getRunState()),
                game.isRecruiting(),
                convertToUsersDTO(game.getUsers())
        );
    }

    private static List<PlayedCharacterDTO> convertToUsersDTO(Map<UUID, UUID> users) {
        return users.entrySet().stream()
                .map(PlayedCharacterDTO::convertToDTO)
                .toList();
    }

    @Nonnull
    static Game convertFromDTO(@Nonnull GameDTO dto) {
        return new Game(
                dto.identifier().scenario(),
                dto.identifier().created(),
                RunStateDTO.convertFromDTO(dto.runState()),
                dto.recruiting() != null && dto.recruiting(),
                convertFromUsersDTO(dto.users())
        );
    }

    private static Map<UUID, UUID> convertFromUsersDTO(List<PlayedCharacterDTO> users) {
        if (users == null) {
            return Map.of();
        } else {
            return users.stream()
                    .map(PlayedCharacterDTO::convertFromDTO)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
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

    @Document(collection = "played_character")
    public record PlayedCharacterDTO(
            UUID characterId,
            UUID userId
    ) {

        @Nonnull
        static PlayedCharacterDTO convertToDTO(@Nonnull Map.Entry<UUID, UUID> entry) {
            return new PlayedCharacterDTO(entry.getKey(), entry.getValue());
        }

        @Nonnull
        static Map.Entry<UUID, UUID> convertFromDTO(@Nonnull PlayedCharacterDTO dto) {
            return new AbstractMap.SimpleEntry<>(dto.characterId(), dto.userId());
        }
    }
}
