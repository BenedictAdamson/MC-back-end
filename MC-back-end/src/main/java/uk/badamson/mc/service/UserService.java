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

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    */
   @Nonnull
   PasswordEncoder getPasswordEncoder();

   /**
    * <p>
    * Get the {@linkplain User user information} for the user with a given ID.
    * </p>
    *
    * @throws NullPointerException
    *            If {@code id} is null
    */
   @Nonnull
   Optional<User> getUser(@Nonnull UUID id);

   /**
    * <p>
    * Retrieve a stream of the current users of this instance of the Mission
    * Command game.
    * </p>
    */
   @Nonnull
   Stream<User> getUsers();

   @Override
   @Nonnull
   User loadUserByUsername(@Nonnull String username)
            throws UsernameNotFoundException;
}
