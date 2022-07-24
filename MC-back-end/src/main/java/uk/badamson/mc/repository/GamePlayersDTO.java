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
import uk.badamson.mc.GamePlayers;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;


@Document(collection = "game_players")
@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "DTO")
public record GamePlayersDTO(
        @Id
        GameIdentifierDTO game,
        boolean recruiting,
        List<PlayedCharacterDTO> users
) {
    @Nonnull
    static GamePlayersDTO convertToDTO(@Nonnull Game.Identifier id, @Nonnull GamePlayers gamePlayers) {
        return new GamePlayersDTO(
                GameIdentifierDTO.convertToDTO(id),
                gamePlayers.isRecruiting(),
                convertToUsersDTO(gamePlayers)
        );
    }

    private static List<PlayedCharacterDTO> convertToUsersDTO(GamePlayers gamePlayers) {
        return gamePlayers.getUsers().entrySet().stream()
                .map(PlayedCharacterDTO::convertToDTO)
                .toList();
    }

    @Nonnull
    static GamePlayers convertFromDTO(@Nonnull GamePlayersDTO dto) {
        return new GamePlayers(
                GameIdentifierDTO.convertFromDTO(dto.game()),
                dto.recruiting(),
                convertFromUsersDTO(dto)
        );
    }

    private static Map<UUID, UUID> convertFromUsersDTO(GamePlayersDTO dto) {
        return dto.users().stream()
                .map(PlayedCharacterDTO::convertFromDTO)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
