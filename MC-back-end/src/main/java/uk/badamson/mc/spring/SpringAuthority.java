package uk.badamson.mc.spring;
/*
 * © Copyright Benedict Adamson 2019-20,22.
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

import org.springframework.security.core.GrantedAuthority;
import uk.badamson.mc.Authority;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum SpringAuthority implements GrantedAuthority {
    /**
     * <p>
     * May play games.
     * </p>
     */
    ROLE_PLAYER,
    /**
     * <p>
     * May add a user.
     * </p>
     */
    ROLE_MANAGE_USERS,
    /**
     * <p>
     * May create a game.
     * </p>
     */
    ROLE_MANAGE_GAMES;

    /**
     * <p>
     * The complete set of authorities.
     * </p>
     */
    public static final Set<SpringAuthority> ALL = Collections
            .unmodifiableSet(EnumSet.allOf(SpringAuthority.class));

    public static Set<SpringAuthority> convertToSpring(@Nonnull Set<Authority> authorities) {
        return authorities.stream().map(SpringAuthority::convertToSpring).collect(Collectors.toUnmodifiableSet());
    }

    public  static SpringAuthority convertToSpring(@Nonnull Authority authority) {
        return valueOf(authority.toString());
    }

    @Nonnull
    public static Set<Authority> convertFromSpring(@Nonnull Set<SpringAuthority> authorities) {
        return authorities.stream().map(SpringAuthority::convertFromSpring).collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    public static Authority convertFromSpring(@Nonnull SpringAuthority authority) {
        return Authority.valueOf(authority.toString());
    }

    @Override
    public String getAuthority() {
        return name();
    }

}
