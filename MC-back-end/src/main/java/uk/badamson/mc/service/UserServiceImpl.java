package uk.badamson.mc.service;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.User;
import uk.badamson.mc.repository.UserSpringRepository;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * <p>
 * Implementation of the part of the service layer pertaining to users of the
 * Mission Command game.
 * </p>
 */
@Service
public class UserServiceImpl implements UserService {

   @Nonnull
   private final PasswordEncoder passwordEncoder;
   @Nonnull
   private final UserSpringRepository userRepository;
   @Nonnull
   private final User administrator;

   /**
    * <p>
    * Construct a service layer instance that uses a given repository.
    * </p>
    *
    * @param passwordEncoder
    *           The encoder that this service uses to encrypt passwords.
    * @param userRepository
    *           The {@link User} repository that this service layer instance
    *           uses.
    * @param administratorPassword
    *           The (unencrypted) {@linkplain User#getPassword() password} of
    *           the {@linkplain User#ADMINISTRATOR_USERNAME administrator}
    * @throws NullPointerException
    *            <ul>
    *            <li>If {@code userRepository} is null.</li>
    *            <li>If {@code administratorPasword} is null.</li>
    *            </ul>
    */
   @Autowired
   public UserServiceImpl(@Nonnull final PasswordEncoder passwordEncoder,
            @Nonnull final UserSpringRepository userRepository,
            @Nonnull @Value("${administrator.password:${random.uuid}}") final String administratorPassword) {
      this.userRepository = Objects.requireNonNull(userRepository,
               "userRepository");
      Objects.requireNonNull(administratorPassword, "administratorPassword");
      this.passwordEncoder = Objects.requireNonNull(passwordEncoder,
               "passwordEncoder");
      administrator = User.createAdministrator(
               passwordEncoder.encode(administratorPassword));
   }

   @Override
   @Transactional
   @Nonnull
   public User add(@Nonnull final BasicUserDetails userDetails) {
      Objects.requireNonNull(userDetails, "userDetails");
      if (BasicUserDetails.ADMINISTRATOR_USERNAME
               .equals(userDetails.getUsername())) {
         throw new IllegalArgumentException("User is administrator");
      } else if (userRepository.findByUsername(userDetails.getUsername())
               .isPresent()) {// read
         throw new UserExistsException();
      }

      final var encryptedUserDetails = new BasicUserDetails(userDetails);
      encryptedUserDetails
               .setPassword(passwordEncoder.encode(userDetails.getPassword()));
      final var user = new User(UUID.randomUUID(), encryptedUserDetails);
      return userRepository.save(user);// write
   }

   @Override
   @Nonnull
   public final PasswordEncoder getPasswordEncoder() {
      return passwordEncoder;
   }

   @Override
   @Nonnull
   public Optional<User> getUser(@Nonnull final UUID id) {
      Objects.requireNonNull(id, "id");
      if (User.ADMINISTRATOR_ID.equals(id)) {
         return Optional.of(administrator);
      } else {
         return userRepository.findById(id);
      }
   }

   @Nonnull
   public final UserSpringRepository getUserRepository() {
      return userRepository;
   }

   @Override
   @Nonnull
   public Stream<User> getUsers() {
      final var repositoryIterable = userRepository.findAll();
      final var adminUses = Stream.of(administrator);
      final var normalUsers = StreamSupport
               .stream(repositoryIterable.spliterator(), false)
               .filter(u -> !u.getUsername()
                        .equals(BasicUserDetails.ADMINISTRATOR_USERNAME));
      return Stream.concat(adminUses, normalUsers);
   }

   @SuppressFBWarnings(value="EI_EXPOSE_REP", justification="reference semantics")
   @Override
   @Nonnull
   public User loadUserByUsername(@Nonnull final String username)
            throws UsernameNotFoundException {
      Objects.requireNonNull(username, "username");
      if (BasicUserDetails.ADMINISTRATOR_USERNAME.equals(username)) {
         return administrator;
      } else {
         final Optional<User> userOptional = userRepository.findByUsername(username);
         if (userOptional.isPresent()) {
            return userOptional.get();
         } else {
            throw new UsernameNotFoundException("No such user, " + username);
         }
      }
   }

}
