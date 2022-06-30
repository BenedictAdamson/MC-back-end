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

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.badamson.mc.Authority;
import uk.badamson.mc.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponse(
        @Nonnull @JsonProperty("id") UUID id,
        @Nonnull @JsonProperty("username") String username,
        @Nullable @JsonProperty("password") String password,
        @Nonnull @JsonProperty("authorities") Set<AuthorityResponse> authorities,
        @JsonProperty("accountNonExpired") boolean accountNonExpired,
        @JsonProperty("accountNonLocked") boolean accountNonLocked,
        @JsonProperty("credentialsNonExpired") boolean credentialsNonExpired,
        @JsonProperty("enabled") boolean enabled
) {

    public enum AuthorityResponse{
        ROLE_PLAYER,
        ROLE_MANAGE_USERS,
        ROLE_MANAGE_GAMES;

        private static final Map<Authority, AuthorityResponse> CONVERT_TO_RESPONSE_MAP;
        static {
            CONVERT_TO_RESPONSE_MAP = new EnumMap<>(Authority.class);
            CONVERT_TO_RESPONSE_MAP.put(Authority.ROLE_PLAYER, AuthorityResponse.ROLE_PLAYER);
            CONVERT_TO_RESPONSE_MAP.put(Authority.ROLE_MANAGE_USERS, AuthorityResponse.ROLE_MANAGE_USERS);
            CONVERT_TO_RESPONSE_MAP.put(Authority.ROLE_MANAGE_GAMES, AuthorityResponse.ROLE_MANAGE_GAMES);
        }

        @Nonnull
        public static AuthorityResponse convertToResponse(@Nonnull Authority authority) {
            return CONVERT_TO_RESPONSE_MAP.get(authority);
        }

        @Nonnull
        public static Set<AuthorityResponse> convertToResponse(@Nonnull Set<Authority> authorities) {
            return authorities.stream().map(a -> convertToResponse(a)).collect(Collectors.toUnmodifiableSet());
        }
    }

    @Nonnull
    public static UserResponse convertToResponse(@Nonnull User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                AuthorityResponse.convertToResponse(user.getAuthorities()),
                user.isAccountNonExpired(),
                user.isAccountNonLocked(),
                user.isCredentialsNonExpired(),
                user.isEnabled()
        );
    }
}
