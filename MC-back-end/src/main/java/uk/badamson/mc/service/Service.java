package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2019-20.
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

import java.util.Collection;
import java.util.stream.Stream;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import uk.badamson.mc.Authority;
import uk.badamson.mc.User;

/**
 * <p>
 * The service layer of the Mission Command game.
 * </p>
 */
public interface Service extends UserDetailsService {

   /**
    * <p>
    * Add a player to the {@linkplain #getUsers() list of players}.
    * </p>
    * <ul>
    * <li>A subsequently retrieved {@linkplain #getUsers() sequence of the
    * players} will include a {@linkplain User player}
    * {@linkplain User#equals(Object) equivalent to} the given player.</li>
    * <li>Subsequently {@linkplain #findByUsername(String) finding user details}
    * using the {@linkplain User#getUsername() username} of the given player
    * will retrieve {@linkplain UserDetails user details} equivalent to the user
    * details of the given player. However, the
    * {@linkplain UserDetails#getPassword() password} will have been encrypted
    * using the {@linkplain #getPasswordEncoder() password encoder} of this
    * service.</li>
    * </ul>
    *
    * @param user
    *           The player to add, with an unencrypted
    *           {@linkplain User#getPassword() password}.
    * @throws NullPointerException
    *            If {@code player} is null
    * @throws IllegalArgumentException
    *            If the {@linkplain User#getUsername() username} of
    *            {@code player} indicates it is the
    *            {@linkplain User#ADMINISTRATOR_USERNAME administrator}.
    */
   @Secured("ROLE_ADMIN")
   void add(final User user);

   /**
    * {@inheritDoc}
    *
    * <ul>
    * <li>Always have user details for the
    * {@linkplain User#ADMINISTRATOR_USERNAME administrator}.</li>
    * <li>The {@linkplain User#ADMINISTRATOR_USERNAME administrator} has a
    * complete {@linkplain Authority set} of
    * {@linkplain UserDetails#getAuthorities() authorities}.</li>
    * </ul>
    */
   @Override
   UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

   /**
    * <p>
    * The encoder that this service uses to encrypt passwords.
    * </p>
    * <ul>
    * <li>Always have a non-null password encoder.</li>
    * </ul>
    *
    * @return the encoder.
    */
   PasswordEncoder getPasswordEncoder();

   /**
    * <p>
    * Retrieve a collection of the current players of this instance
    * of the Mission Command game.
    * </p>
    * <ul>
    * <li>Always returns a (non null) collection.</li>
    * <li>The returned collection will not
    * include a null element</li>
    * <li>The collection of players always {@linkplain Collection#contains(Object) contains}
    * player with the {@linkplain User#ADMINISTRATOR_USERNAME administrator
    * username} as its {@linkplain User#getUsername() username}.</li>
    * <li>Does not contain players with duplicate
    * {@linkplain User#getUsername() usernames}.</li>
    * </ul>
    *
    * @return a {@linkplain Stream stream} of the players.
    */
   Stream<User> getUsers();

}
