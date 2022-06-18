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
import org.springframework.security.core.userdetails.UserDetails;
import uk.badamson.mc.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serial;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * A specification for a new {@linkplain User user}.
 * </p>
 */
public class BasicUserDetails implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * The {@linkplain #getUsername() username} of an administrator user.
     * </p>
     *
     * @see #createAdministrator(String)
     */
    public static final String ADMINISTRATOR_USERNAME = "Administrator";

    /**
     * <p>
     * Create {@link BasicUserDetails} for a a valid administrator user.
     * </p>
     *
     * @param password the password used to authenticate the user, or null if the
     *                 password is being hidden or is unknown. This might be the
     *                 password in an encrypted form.
     * @see #ADMINISTRATOR_USERNAME
     */
    @Nonnull
    public static BasicUserDetails createAdministrator(
            @Nullable final String password) {
        return new BasicUserDetails(password);
    }

    private final String username;
    private String password;
    private final Set<GrantedMCAuthority> authorities;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;

    /**
     * <p>
     * Copy a specification for a new {@linkplain User user}.
     * </p>
     *
     * @param that the specification to copy
     * @throws NullPointerException If {@code that} is null
     */
    public BasicUserDetails(@Nonnull final BasicUserDetails that) {
        Objects.requireNonNull(that, "that");
        this.username = that.username;
        this.password = that.password;
        this.authorities = that.authorities;
        this.accountNonExpired = that.accountNonExpired;
        this.accountNonLocked = that.accountNonLocked;
        this.credentialsNonExpired = that.credentialsNonExpired;
        this.enabled = that.enabled;
    }

    protected BasicUserDetails(final String password) {
        this.username = ADMINISTRATOR_USERNAME;
        this.password = password;
        this.authorities = GrantedMCAuthority.ALL;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
    }

    /**
     * <p>
     * Construct a specification for a new {@linkplain User user}, with given
     * attribute values.
     * </p>
     *
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
     *                                         <li>If {@code username} is null</li>
     *                                         <li>If {@code authorities} is null</li>
     *                                         <li>If {@code authorities} contains null</li>
     *                                         </ul>
     */
    @JsonCreator
    public BasicUserDetails(
            @Nonnull @JsonProperty("username") final String username,
            @Nullable @JsonProperty("password") final String password,
            @Nonnull @JsonProperty("authorities") final Set<GrantedMCAuthority> authorities,
            @JsonProperty("accountNonExpired") final boolean accountNonExpired,
            @JsonProperty("accountNonLocked") final boolean accountNonLocked,
            @JsonProperty("credentialsNonExpired") final boolean credentialsNonExpired,
            @JsonProperty("enabled") final boolean enabled) {
        this.username = Objects.requireNonNull(username, "username");
        this.password = password;
        this.authorities = authorities.isEmpty() ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(authorities));
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "authorities is unmodifiable")
    @Override
    public final Set<GrantedMCAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public final String getPassword() {
        return password;
    }

    @Override
    @Nonnull
    public final String getUsername() {
        return username;
    }

    @Override
    public final boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public final boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public final boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    public final void setPassword(@Nullable final String password) {
        this.password = password;
    }

}
