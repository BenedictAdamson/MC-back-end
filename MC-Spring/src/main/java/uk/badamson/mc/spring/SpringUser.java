package uk.badamson.mc.spring;
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.userdetails.UserDetails;
import uk.badamson.mc.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serial;
import java.util.*;

/**
 * <p>
 * A user of the Mission Command game.
 * </p>
 */
@Document(collection="user")
public final class SpringUser implements UserDetails  {

    @Serial
    private static final long serialVersionUID = 1L;

    @Nonnull
    public static SpringUser convertToSpring(@Nonnull User user) {
        return new SpringUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                SpringAuthority.convertToSpring(user.getAuthorities()),
                user.isAccountNonExpired(),
                user.isAccountNonLocked(),
                user.isCredentialsNonExpired(),
                user.isEnabled()
        );
    }

    @Nonnull
    public static User convertFromSpring(@Nonnull SpringUser user) {
        return new User(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                SpringAuthority.convertFromSpring(user.getAuthorities()),
                user.isAccountNonExpired(),
                user.isAccountNonLocked(),
                user.isCredentialsNonExpired(),
                user.isEnabled()
        );
    }

    @org.springframework.data.annotation.Id
    private final UUID id;



    @Indexed
    private final String username;
    private String password;
    private final Set<SpringAuthority> authorities;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;

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
    public SpringUser(@Nonnull @JsonProperty("id") final UUID id,
                      @Nonnull @JsonProperty("username") final String username,
                      @Nullable @JsonProperty("password") final String password,
                      @Nonnull @JsonProperty("authorities") final Set<SpringAuthority> authorities,
                      @JsonProperty("accountNonExpired") final boolean accountNonExpired,
                      @JsonProperty("accountNonLocked") final boolean accountNonLocked,
                      @JsonProperty("credentialsNonExpired") final boolean credentialsNonExpired,
                      @JsonProperty("enabled") final boolean enabled) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = Objects.requireNonNull(username, "username");
        this.password = password;
        this.authorities = authorities.isEmpty() ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(authorities));
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to another object.
     * </p>
     * <ul>
     * <li>The {@link SpringUser} class has <i>entity semantics</i>, with the
     * {@linkplain #getId() ID} attribute serving as a unique ID: this object is
     * equivalent to another object if, and only of, the other object is also a
     * {@link SpringUser} and the two have {@linkplain String#equals(Object)
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
        if (!(that instanceof final SpringUser other)) {
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


    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "authorities is unmodifiable")
    @Override
    public Set<SpringAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    @Nonnull
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public  boolean isEnabled() {
        return enabled;
    }

    public void setPassword(@Nullable final String password) {
        this.password = password;
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
