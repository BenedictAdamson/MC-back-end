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

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import uk.badamson.mc.Authority;
import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.User;

/**
 * <p>
 * The part of the service layer pertaining to users of the Mission Command
 * game.
 * </p>
 */
public interface UserService extends UserDetailsService {

   /**
    * <p>
    * Create a new user, having given {@linkplain BasicUserDetails user
    * details}, and add them to the {@linkplain #getUsers() list of users}.
    * </p>
    * <ul>
    * <li>Always returns the (non null) created users.</li>
    * <li>The attributes of the returned user are equivalent to those of the
    * given user details. However, the {@linkplain User#getPassword() password}
    * will have been encrypted using the {@linkplain #getPasswordEncoder()
    * password encoder} of this service.</li>
    * <li>A subsequently retrieved {@linkplain #getUsers() sequence of the
    * users} will include a {@linkplain User user}
    * {@linkplain User#equals(Object) equivalent to} the returned user.</li>
    * <li>Subsequently {@linkplain #loadUserByUsername(String) finding user
    * details} using the {@linkplain User#getUsername() username} of the given
    * user details will retrieve {@linkplain UserDetails user details}
    * equivalent to the user details of the returned user.
    * </ul>
    *
    * @param userDetails
    *           The details of the user to add, with an unencrypted
    *           {@linkplain BasicUserDetails#getPassword() password}.
    * @throws NullPointerException
    *            If {@code userDetails} is null
    * @throws IllegalArgumentException
    *            If the {@linkplain BasicUserDetails#getUsername() username} of
    *            {@code userDetails} indicates it is the
    *            {@linkplain User#ADMINISTRATOR_USERNAME administrator}.
    * @throws UserExistsException
    *            If the {@linkplain BasicUserDetails#getUsername() username} of
    *            {@code userDetails} is already the username of a user, and is
    *            not the administrator.
    */
   @Nonnull
   User add(@Nonnull BasicUserDetails userDetails);

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
   @Nonnull
   PasswordEncoder getPasswordEncoder();

   /**
    * <p>
    * Get the {@linkplain User user information} for the user with a given ID.
    * </p>
    * <ul>
    * <li>Returns a (non null) optional value.</li>
    * <li>Returns either an {@linkplain Optional#isEmpty() empty} value, or a
    * value for which the {@linkplain User#getId() identifier}
    * {@linkplain UUID#equals(Object) is equivalent to} the given ID</li>
    * </ul>
    *
    * @param id
    *           The unique ID of the user.
    * @return The user
    */
   @Nonnull
   Optional<User> getUser(UUID id);

   /**
    * <p>
    * Retrieve a stream of the current users of this instance of the Mission
    * Command game.
    * </p>
    * <ul>
    * <li>Always returns a (non null) stream.</li>
    * <li>The returned stream will not include a null element</li>
    * <li>The stream of users always contains a user with the
    * {@linkplain User#ADMINISTRATOR_USERNAME administrator username} as its
    * {@linkplain User#getUsername() username}.</li>
    * <li>Does not contain users with duplicate {@linkplain User#getUsername()
    * usernames}.</li>
    * </ul>
    *
    * @return a {@linkplain Stream stream} of the users.
    */
   @Nonnull
   Stream<User> getUsers();

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
   @Nonnull
   User loadUserByUsername(String username) throws UsernameNotFoundException;
}
