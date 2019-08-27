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
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

/**
 * <p>
 * An authority that can be {@linkplain Player#getAuthorities() granted to} a
 * {@linkplain Player player}.
 * </p>
 */
public enum Authority implements GrantedAuthority {
   /**
    * <p>
    * May add a player.
    * </p>
    */
   ROLE_ADMIN;

   /**
    * <p>
    * The complete set of authorities.
    * </p>
    */
   public static final Set<Authority> ALL = Collections
            .unmodifiableSet(EnumSet.allOf(Authority.class));

   @Override
   public String getAuthority() {
      return name();
   }

}
