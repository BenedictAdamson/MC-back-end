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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import uk.badamson.mc.User;

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link UserRepository}
 * interface.
 */
public class UserRepositoryTest {

   public static final class Fake implements UserRepository {

      private static void requireNonNull(final Object object,
               final String message) {
         if (object == null) {
            throw new IllegalArgumentException("Null " + message);
         }
      }

      private final Map<String, User> users = new ConcurrentHashMap<>();

      @Override
      public long count() {
         return users.size();
      }

      @Override
      public void delete(final User user) {
         requireNonNull(user, "user");
         deleteById(user.getUsername());
      }

      @Override
      public void deleteAll() {
         users.clear();
      }

      @Override
      public void deleteAll(final Iterable<? extends User> users) {
         requireNonNull(users, "users");
         for (final User user : users) {
            this.users.remove(user.getUsername());
         }
      }

      @Override
      public void deleteById(final String username) {
         requireNonNull(username, "username");
         users.remove(username);
      }

      @Override
      public boolean existsById(final String username) {
         requireNonNull(username, "username");
         return users.containsKey(username);
      }

      @Override
      public Iterable<User> findAll() {
         return users.values();
      }

      @Override
      public Iterable<User> findAllById(final Iterable<String> usernames) {
         requireNonNull(usernames, "usernames");
         return StreamSupport.stream(usernames.spliterator(), false).distinct()
                  .map(un -> users.get(un)).filter(un -> un != null)
                  .collect(Collectors.toUnmodifiableList());
      }

      @Override
      public Optional<User> findById(final String username) {
         requireNonNull(username, "username");
         return Optional.ofNullable(users.get(username));
      }

      @Override
      public <USER extends User> USER save(final USER user) {
         requireNonNull(user, "user");
         users.put(user.getUsername(), user);
         return user;
      }

      @Override
      public <USER extends User> Iterable<USER> saveAll(
               final Iterable<USER> users) {
         requireNonNull(users, "users");
         for (final var user : users) {
            this.users.put(user.getUsername(), user);
         }
         return users;
      }

   }

   public static void assertInvariants(final UserRepository repository) {
      // Do nothing
   }

}
