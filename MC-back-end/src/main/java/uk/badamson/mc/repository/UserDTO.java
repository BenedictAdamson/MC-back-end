package uk.badamson.mc.repository;
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.PersistenceCreator;
import uk.badamson.mc.spring.BasicUserDetails;
import uk.badamson.mc.spring.GrantedMCAuthority;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.annotation.Id;

public final class UserDTO extends BasicUserDetails {

    @Id
    private final UUID id;

    @JsonCreator
    @PersistenceCreator
    public UserDTO(@Nonnull @JsonProperty("id") final UUID id,
                   @Nonnull @JsonProperty("username") final String username,
                   @Nullable @JsonProperty("password") final String password,
                   @Nonnull @JsonProperty("authorities") final Set<GrantedMCAuthority> authorities,
                   @JsonProperty("accountNonExpired") final boolean accountNonExpired,
                   @JsonProperty("accountNonLocked") final boolean accountNonLocked,
                   @JsonProperty("credentialsNonExpired") final boolean credentialsNonExpired,
                   @JsonProperty("enabled") final boolean enabled) {
        super(username, password, authorities, accountNonExpired,
                accountNonLocked, credentialsNonExpired, enabled);
        this.id = Objects.requireNonNull(id, "id");
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to another object.
     * </p>
     * <ul>
     * <li>The {@link UserDTO} class has <i>entity semantics</i>, with the
     * {@linkplain #getId() ID} attribute serving as a unique ID: this object is
     * equivalent to another object if, and only of, the other object is also a
     * {@link UserDTO} and the two have {@linkplain String#equals(Object)
     * equivalent} {@linkplain #getId() IDs}.</li>
     * </ul>
     *
     * @param that The object to compare with this.
     * @return Whether this is equivalent to that.
     */
    @Override
    public boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (!(that instanceof final UserDTO other)) {
            return false;
        }
        return id.equals(other.id);
    }

    /**
     * <p>
     * The unique ID of this user.
     * </p>
     * <p>
     * Note that the {@linkplain #getUsername() username} need not be unique.
     * However, in practice it will be enforced to be unique, with the username
     * used as the human readable ID.
     * </p>
     */
    @Nonnull
    public UUID getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "User [id=" + id + "]";
    }

}
