package uk.badamson.mc.rest;
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

import uk.badamson.mc.Game;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;

public record GameResponse(
        GameIdentifierResponse identifier,
        RunStateResponse runState
) {

    @Nonnull
    public static GameResponse convertToResponse(
            @Nonnull Game.Identifier identifier,
            @Nonnull Game game) {
        return new GameResponse(
                GameIdentifierResponse.convertToResponse(identifier),
                RunStateResponse.convertToResponse(game.getRunState())
        );
    }

    public enum RunStateResponse {
        WAITING_TO_START, RUNNING, STOPPED;

        private static final Map<Game.RunState, RunStateResponse> CONVERT_TO_RESPONSE_MAP;
        static {
            CONVERT_TO_RESPONSE_MAP = new EnumMap<>(Game.RunState.class);
            CONVERT_TO_RESPONSE_MAP.put(Game.RunState.WAITING_TO_START, RunStateResponse.WAITING_TO_START);
            CONVERT_TO_RESPONSE_MAP.put(Game.RunState.RUNNING, RunStateResponse.RUNNING);
            CONVERT_TO_RESPONSE_MAP.put(Game.RunState.STOPPED, RunStateResponse.STOPPED);
        }

        @Nonnull
        public static RunStateResponse convertToResponse(@Nonnull Game.RunState runState) {
            return CONVERT_TO_RESPONSE_MAP.get(runState);
        }
    }

}
