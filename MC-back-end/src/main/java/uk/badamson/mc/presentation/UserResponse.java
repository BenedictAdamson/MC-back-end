package uk.badamson.mc.presentation;
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
import java.io.Serial;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * A user of the Mission Command game.
 * </p>
 */
public final class UserResponse extends BasicUserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * The {@linkplain #getId() ID} of an administrator user.
     * </p>
     */
    public static final UUID ADMINISTRATOR_ID = new UUID(0L, 0L);

    /**
     * <p>
     * Create a {@link UserResponse} that is a valid administrator user.
     * </p>
     *
     * @param password the password used to authenticate the user, or null if the
     *                 password is being hidden or is unknown. This might be the
     *                 password in an encrypted form.
     */
    @Nonnull
    public static UserResponse createAdministrator(@Nullable final String password) {
        return new UserResponse(password);
    }

    @org.springframework.data.annotation.Id
    private final UUID id;

    private UserResponse(final String password) {
        super(password);
        this.id = ADMINISTRATOR_ID;
    }

    /**
     * <p>
     * Construct a user of the Mission Command game, with given user details.
     * </p>
     *
     * @param id          The unique ID of this user.
     * @param userDetails the specification for this user.
     * @throws NullPointerException <ul>
     *                                         <li>If {@code id} is null</li>
     *                                         <li>If {@code userDetails} is null</li>
     *                                         </ul>
     */
    public UserResponse(@Nonnull final UUID id,
                        @Nonnull final BasicUserDetails userDetails) {
        super(userDetails);
        this.id = Objects.requireNonNull(id, "id");
    }

    /**
     * <p>
     * Construct a user of the Mission Command game, with given attribute values.
     * </p>
     *
     * @param id                    The unique ID of this user.
     * @param username              the username used to authenticate the user
     * @param password              the password used to authenticate the user, or null if the
     *                              password is being hidden or is unknown. This might be the
     *                              password in an encrypted form.
     * @param authorities           The authorities granted to the user.
     * @param accountNonExpired     whether the user's account has expired, and so cannot be
     *                              authenticated.
     * @param accountNonLocked      whether the user's account is locked, and so cannot be
     *                              authenticated.
     * @param credentialsNonExpired whether the user's credentials (password) has expired, and so
     *                              can not be authenticated.
     * @param enabled               whether the user is enabled; a disabled user cannot be
     *                              authenticated.
     * @throws NullPointerException <ul>
     *                                         <li>If {@code id} is null</li>
     *                                         <li>If {@code username} is null</li>
     *                                         <li>If {@code authorities} is null</li>
     *                                         <li>If {@code authorities} contains null</li>
     *                                         </ul>
     */
    @JsonCreator
    @PersistenceCreator
    public UserResponse(@Nonnull @JsonProperty("id") final UUID id,
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
     * <li>The {@link UserResponse} class has <i>entity semantics</i>, with the
     * {@linkplain #getId() ID} attribute serving as a unique ID: this object is
     * equivalent to another object if, and only of, the other object is also a
     * {@link UserResponse} and the two have {@linkplain String#equals(Object)
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
        if (!(that instanceof final UserResponse other)) {
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
