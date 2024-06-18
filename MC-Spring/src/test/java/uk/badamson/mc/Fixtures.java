package uk.badamson.mc;
/*
 * © Copyright Benedict Adamson 2020-24.
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

import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class Fixtures {


    public static final BasicUserDetails ADMINISTRATOR = BasicUserDetails
            .createAdministrator("password");
    public static final DockerImageName MONGO_DB_IMAGE = DockerImageName.parse("mongo:4.4");

    private static String createUserName(@Nonnull final UUID id) {
        return "jeff-" + id;
    }

    public static String createUserName() {
        return createUserName(UUID.randomUUID());
    }

    public static BasicUserDetails createBasicUserDetailsWithAllRoles() {
        return createBasicUserDetailsWithAuthorities(Authority.ALL);
    }

    public static BasicUserDetails createBasicUserDetailsWithPlayerRole() {
        return createBasicUserDetailsWithAuthorities(EnumSet.of(Authority.ROLE_PLAYER));
    }

    public static BasicUserDetails createBasicUserDetailsWithManageGamesRole() {
        return createBasicUserDetailsWithAuthorities(EnumSet.of(Authority.ROLE_MANAGE_GAMES));
    }

    public static BasicUserDetails createBasicUserDetailsWithAuthorities(final Set<Authority> authorities) {
        final var id = UUID.randomUUID();
        return new BasicUserDetails(createUserName(id),"secret",
                authorities,
                true, true, true, true);
    }

}
