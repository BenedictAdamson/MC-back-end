package uk.badamson.mc.repository;
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

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import uk.badamson.mc.User;

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link UserRepository}
 * interface.
 */
public class UserRepositoryTest {

   public static final class Fake
            extends CrudRepositoryTest.AbstractFake<User, UUID>
            implements UserRepository {

      @Override
      protected User copy(final User user) {
         Objects.requireNonNull(user, "user");
         return new User(user.getId(), user);
      }

      @Override
      public Optional<User> findByUsername(final String username) {
         requireNonNull(username, "username");
         return entities.values().stream()
                  .filter(u -> username.equals(u.getUsername())).findAny();
      }

      @Override
      protected UUID getId(final User user) {
         Objects.requireNonNull(user, "user");
         return user.getId();
      }

   }// class

   public static void assertInvariants(final UserRepository repository) {
      CrudRepositoryTest.assertInvariants(repository);// inherited
   }

}
