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
import java.util.UUID;

public final class Fixtures {


    public static final DockerImageName MONGO_DB_IMAGE = DockerImageName.parse("mongo:4.4");

    private static String createUserName(@Nonnull final UUID id) {
        return "jeff-" + id;
    }

    public static BasicUserDetails createBasicUserDetailsWithAllRoles() {
        final var id = UUID.randomUUID();
        return new BasicUserDetails(createUserName(id),"secret",
                Authority.ALL,
                true, true, true, true);
    }

}
