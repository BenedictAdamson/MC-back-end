package uk.badamson.mc.rest;
/*
 * © Copyright Benedict Adamson 2019-23.
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

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.UUID;

public final class Paths {

    public static final String USERS_PATH = "/api/user";

    public static final String USER_PATH_PATTERN = "/api/user/{id}";

    public static final String SELF_PATH = "/api/self";

    public static final String SCENARIOS_PATH = "/api/scenario";

    public static final String SCENARIO_PATH_PATTERN = "/api/scenario/{id}";

    public static final String CURRENT_GAME_PATH = "/api/self/current-game";

    public static final String GAMES_PATH_PATTERN = "/api/scenario/{scenario}/games";

    public static final String GAME_PATH_PATTERN = "/api/game/{game}";

    public static final String GAME_START_PARAM = "start";

    public static final String GAME_STOP_PARAM = "stop";

    public static final String END_GAME_RECRUITMENT_PARAM = "endRecruitment";

    public static final String MAY_JOIN_GAME_PARAM = "mayJoin";

    public static final String JOIN_GAME_PARAM = "join";

    private Paths() {
        throw new AssertionError("must not instantiate");
    }

    @Nonnull
    public static String createPathForUser(@Nonnull final UUID id) {
        Objects.requireNonNull(id, "id");
        return "/api/user/" + id;
    }

    @Nonnull
    public static String createPathForScenario(final UUID id) {
        Objects.requireNonNull(id, "id");
        return "/api/scenario/" + id;
    }

    @Nonnull
    public static String createPathForGamesOfScenario(@Nonnull final UUID scenario) {
        Objects.requireNonNull(scenario);
        return "/api/scenario/" + scenario + "/games";
    }

    @Nonnull
    public static String createPathForGame(@Nonnull final UUID game) {
        Objects.requireNonNull(game);
        return "/api/game/" + game;
    }

    @Nonnull
    public static String createPathForStartingGame(@Nonnull final UUID game) {
        return createPathForGame(game) + "?" + GAME_START_PARAM;
    }

    @Nonnull
    public static String createPathForStoppingGame(@Nonnull final UUID game) {
        return createPathForGame(game) + "?" + GAME_STOP_PARAM;
    }

    @Nonnull
    public static String createPathForEndRecruitmentOfGame(
            @Nonnull final UUID game) {
        return createPathForGame(game) + "?" + END_GAME_RECRUITMENT_PARAM;
    }

    @Nonnull
    public static String createPathForJoiningGame(@Nonnull final UUID game) {
        return createPathForGame(game) + "?" + JOIN_GAME_PARAM;
    }

    @Nonnull
    public static String createPathForMayJoinQueryOfGame(@Nonnull final UUID game) {
        return createPathForGame(game) + "?" + MAY_JOIN_GAME_PARAM;
    }
}
