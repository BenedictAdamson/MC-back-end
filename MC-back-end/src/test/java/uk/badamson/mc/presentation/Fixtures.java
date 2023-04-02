package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2020-23.
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

import uk.badamson.mc.Authority;
import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.spring.SpringAuthority;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.UUID;

final class Fixtures {


    static final BasicUserDetails ADMINISTRATOR = BasicUserDetails
            .createAdministrator("password");

    private static String createUserName(@Nonnull final UUID id) {
        return "jeff-" + id;
    }

    static String createUserName() {
        return createUserName(UUID.randomUUID());
    }

    static BasicUserDetails createBasicUserDetailsWithAllRoles() {
        final var id = UUID.randomUUID();
        return new BasicUserDetails(createUserName(id),"secret",
                Authority.ALL,
                true, true, true, true);
    }

    static BasicUserDetails createBasicUserDetailsWithPlayerRole() {
        final var id = UUID.randomUUID();
        return new BasicUserDetails(createUserName(id),"secret",
                EnumSet.of(Authority.ROLE_PLAYER),
                true, true, true, true);
    }

    static BasicUserDetails createBasicUserDetailsWithManageGamesRole() {
        final var id = UUID.randomUUID();
        return new BasicUserDetails(createUserName(id),"secret",
                EnumSet.of(Authority.ROLE_MANAGE_GAMES),
                true, true, true, true);
    }

    static SpringUser createUserWithAllRoles() {
        final var id = UUID.randomUUID();
        return new SpringUser(id, createUserName(id),"secret",
                SpringAuthority.ALL,
                true, true, true, true);
    }

}
