package uk.badamson.mc.rest;
/*
 * © Copyright Benedict Adamson 2019-22.
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

import uk.badamson.mc.BasicUserDetails;

import javax.annotation.Nonnull;
import java.util.Set;

public record UserDetailsRequest(
        String username,
        String password,
        Set<AuthorityValue> authorities,
        boolean accountNonExpired,
        boolean accountNonLocked,
        boolean credentialsNonExpired,
        boolean enabled
) {

    @Nonnull
    public static BasicUserDetails convertFromRequest(@Nonnull UserDetailsRequest request) {
        return new BasicUserDetails(
                request.username(),
                request.password(),
                AuthorityValue.convertFromValue(request.authorities()),
                request.accountNonExpired(),
                request.accountNonLocked(),
                request.credentialsNonExpired(),
                request.enabled()
        );
    }
}
