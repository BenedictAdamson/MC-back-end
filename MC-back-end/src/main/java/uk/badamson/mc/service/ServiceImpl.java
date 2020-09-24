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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import uk.badamson.mc.Authority;
import uk.badamson.mc.User;
import uk.badamson.mc.repository.PlayerRepository;

/**
 * <p>
 * The concrete implementation of the service layer of the Mission Command game.
 * </p>
 */
@org.springframework.stereotype.Service
public class ServiceImpl implements Service {

   private final PasswordEncoder passwordEncoder;
   private final PlayerRepository playerRepository;
   private final User administrator;

   /**
    * <p>
    * Construct a service layer instance that uses a given repository.
    * </p>
    * <ul>
    * <li>The {@linkplain #getPlayerRepository() player repository} of this
    * service is the given player repository.</li>
    * <li>The {@linkplain #getPasswordEncoder() password encoder} of this
    * service is the given password encoder.</li>
    * <li>The {@linkplain User#getPassword() password} of the
    * {@linkplain User#ADMINISTRATOR_USERNAME administrator}
    * {@linkplain #findByUsername(String) user details found through this
    * service} is {@linkplain String#equals(Object) equal to} the given
    * administrator password encrypted by the given password encoder.</li>
    * </ul>
    *
    * @param passwordEncoder
    *           The encoder that this service uses to encrypt passwords.
    * @param playerRepository
    *           The {@link Player} repository that this service layer instance
    *           uses.
    * @param administratorPassword
    *           The (unencrypted) {@linkplain User#getPassword() password} of
    *           the {@linkplain User#ADMINISTRATOR_USERNAME administrator}
    * @throws NullPointerException
    *            <ul>
    *            <li>If {@code playerRepository} is null.</li>
    *            <li>If {@code administratorPasword} is null.</li>
    *            </ul>
    */
   public ServiceImpl(@NonNull final PasswordEncoder passwordEncoder,
            @NonNull final PlayerRepository playerRepository,
            @NonNull final String administratorPassword) {
      this.playerRepository = Objects.requireNonNull(playerRepository,
               "playerRepository");
      Objects.requireNonNull(administratorPassword, "administratorPassword");
      this.passwordEncoder = Objects.requireNonNull(passwordEncoder,
               "passwordEncoder");
      administrator = new User(User.ADMINISTRATOR_USERNAME,
               passwordEncoder.encode(administratorPassword), Authority.ALL);
   }

   @Override
   public void add(User user) {
      Objects.requireNonNull(user, "user");
      if (User.ADMINISTRATOR_USERNAME.equals(user.getUsername())) {
         throw new IllegalArgumentException("User is administrator");
      }
      user = new User(user.getUsername(),
               passwordEncoder.encode(user.getPassword()),
               user.getAuthorities());
      playerRepository.save(user);
   }

   @Override
   public User loadUserByUsername(final String username)
            throws UsernameNotFoundException {
      Objects.requireNonNull(username, "username");
      if (User.ADMINISTRATOR_USERNAME.equals(username)) {
         return administrator;
      } else {
         try {
            return playerRepository.findById(username).get();
         } catch (NoSuchElementException e) {
            throw new UsernameNotFoundException("No such user, " + username, e);
         }
      }
   }

   @Override
   public final PasswordEncoder getPasswordEncoder() {
      return passwordEncoder;
   }

   /**
    * <p>
    * The {@link User} repository that this service layer instance uses.
    * </p>
    * <ul>
    * <li>Always have a (non null) player repository.</li>
    * </ul>
    *
    * @return the repository.
    */
   public final PlayerRepository getPlayerRepository() {
      return playerRepository;
   }

   @Override
   public Stream<User> getUsers() {
      final var repositoryIterable = playerRepository.findAll();
      final Stream<User> adminUses = Stream.of(administrator);
      final Stream<User> normalUsers = StreamSupport.stream(repositoryIterable.spliterator(), false);
      return Stream.concat(adminUses, normalUsers).
   }

}
