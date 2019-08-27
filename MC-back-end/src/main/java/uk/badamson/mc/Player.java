package uk.badamson.mc;
/*
 * © Copyright Benedict Adamson 2019.
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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>
 * A player of the Mission Command game.
 * </p>
 */
public final class Player implements UserDetails {

   private static final long serialVersionUID = 1L;

   /**
    * <p>
    * The {@linkplain #getUsername() username} of the administrator player.
    * </p>
    */
   public static final String ADMINISTRATOR_USERNAME = "Administrator";

   private final String username;
   private final String password;
   private final Set<Authority> authorities;

   /**
    * <p>
    * Construct a player of the Mission Command game, with a given user-name.
    * </p>
    * <ul>
    * <li>The {@linkplain #getUsername() username} of this player is the given
    * username.</li>
    * <li>The {@linkplain #getPassword() password} of this player is the given
    * password.</li>
    * <li>The {@linkplain #getAuthorities() authorities} granted to this player
    * are {@linkplain Set#equals(Object) equal to} the given authorities.</li>
    * </ul>
    *
    * @param username
    *           the username used to authenticate the player
    * @param password
    *           the password used to authenticate the user, or null if the
    *           password is being hidden or is unknown. This might be the
    *           password in an encrypted form.
    * @param authorities
    *           The authorities granted to the player.
    * @throws NullPointerException
    *            <ul>
    *            <li>If {@code username} is null</li>
    *            <li>If {@code authorities} is null</li>
    *            <li>If {@code authorities} contains null</li>
    *            </ul>
    */
   @JsonCreator
   public Player(@NonNull @JsonProperty("username") final String username,
            @Nullable @JsonProperty("password") final String password,
            @NonNull @JsonProperty("authorities") final Set<Authority> authorities) {
      this.username = Objects.requireNonNull(username, "username");
      this.password = password;
      this.authorities = authorities.isEmpty() ? Collections.emptySet()
               : Collections.unmodifiableSet(EnumSet.copyOf(authorities));
   }

   /**
    * <p>
    * With this object is <dfn>equivalent</dfn> to another object.
    * </p>
    * <ul>
    * <li>The {@link Player} class has <i>entity semantics</i>, with the
    * {@linkplain #getUsername() username} serving as a unique ID: this object
    * is equivalent to another object if, and only of, the other object is also
    * a {@link Player} and the two have {@linkplain String#equals(Object)
    * equivalent} {@linkplain #getUsername() usernames}.</li>
    * </ul>
    *
    * @param that
    *           The object to compare with this.
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
      if (!(that instanceof Player)) {
         return false;
      }
      final Player other = (Player) that;
      return username.equals(other.username);
   }

   @Override
   public final Set<Authority> getAuthorities() {
      return authorities;
   }

   @Override
   public final String getPassword() {
      return password;
   }

   @Override
   @NonNull
   public final String getUsername() {
      return username;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + username.hashCode();
      return result;
   }

   @Override
   public final boolean isAccountNonExpired() {
      // TODO player accounts can expire
      return true;
   }

   @Override
   public final boolean isAccountNonLocked() {
      // TODO player acccounts can be locked
      return true;
   }

   @Override
   public final boolean isCredentialsNonExpired() {
      // TODO player credentials can expire
      return true;
   }

   @Override
   public final boolean isEnabled() {
      // TODO players can be disabled
      return true;
   }

   @Override
   public String toString() {
      return "Player [username=" + username + "]";
   }

}
