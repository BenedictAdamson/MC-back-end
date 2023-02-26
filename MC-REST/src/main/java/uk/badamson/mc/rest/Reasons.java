package uk.badamson.mc.rest;
/*
 * © Copyright Benedict Adamson 2023.
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

public final class Reasons {
    public static final String SCENARIO_NOT_FOUND = "Scenario Not Found";
    public static final String GAME_NOT_FOUND = "Game Not Found";
    public static final String GAME_STATE_CONFLICT = "Game State Conflict";
    public static final String USER_STATE_CONFLICT = "User State Conflict";
    public static final String USER_EXISTS_CONFLICT = "User Exists Conflict";
    public static final String USER_NOT_FOUND = "User Not Found";

    private Reasons() {
        throw new AssertionError("must not instantiate");
    }
}
