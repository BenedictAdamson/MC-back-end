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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.mc.Game;
import uk.badamson.mc.GamePlayers;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

@SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "DTO")
public record GamePlayersResponse(
        Game.Identifier game,
        boolean recruiting,
        Map<UUID, UUID> users
) {

    @Nonnull
    public static GamePlayersResponse convertToResponse(@Nonnull Game.Identifier identifier, @Nonnull GamePlayers gamePlayers) {
        return new GamePlayersResponse(
                gamePlayers.getGame(),
                gamePlayers.isRecruiting(),
                gamePlayers.getUsers()
        );
    }

    @Override
    public boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof final GamePlayersResponse other)) {
            return false;
        }
        return game.equals(other.game);
    }

    @Override
    public int hashCode() {
        return game.hashCode();
    }

}
