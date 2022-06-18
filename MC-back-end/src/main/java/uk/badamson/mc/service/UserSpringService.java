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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.User;
import uk.badamson.mc.repository.MCSpringRepositoryAdapter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class UserSpringService implements UserDetailsService {

    private final UserService delegate;

    @Autowired
    public UserSpringService(@Nonnull final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                             @Nonnull @Value("${administrator.password:${random.uuid}}") final String administratorPassword,
                             @Nonnull MCSpringRepositoryAdapter repository) {
        this.delegate = new UserService(
                new PasswordEncoderAdapter(passwordEncoder), administratorPassword, repository
        );
    }

    final UserService getDelegate() {
        return delegate;
    }

    @Transactional
    @Nonnull
    public User add(@Nonnull final BasicUserDetails userDetails) {
        return delegate.add(userDetails);
    }

    @Nonnull
    public Optional<User> getUser(@Nonnull final UUID id) {
        return delegate.getUser(id);
    }

    @Nonnull
    public Stream<User> getUsers() {
        return delegate.getUsers();
    }

    @Override
    @Nonnull
    public User loadUserByUsername(@Nonnull final String username)
            throws UsernameNotFoundException {
        final var userOptional = delegate.getUserByUsername(username);
        if (userOptional.isPresent()) {
            return userOptional.get();
        } else {
            throw new UsernameNotFoundException(username);
        }
    }

    private record PasswordEncoderAdapter(
            org.springframework.security.crypto.password.PasswordEncoder springPasswordEncoder) implements PasswordEncoder {

        private PasswordEncoderAdapter(@Nonnull org.springframework.security.crypto.password.PasswordEncoder springPasswordEncoder) {
            this.springPasswordEncoder = Objects.requireNonNull(springPasswordEncoder);
        }

        @Nonnull
        @Override
        public String encode(@Nonnull CharSequence rawPassword) {
            return springPasswordEncoder.encode(rawPassword);
        }

        @Override
        public boolean matches(@Nonnull CharSequence rawPassword, @Nonnull String encryptedPassword) {
            return springPasswordEncoder.matches(rawPassword, encryptedPassword);
        }
    }

}
