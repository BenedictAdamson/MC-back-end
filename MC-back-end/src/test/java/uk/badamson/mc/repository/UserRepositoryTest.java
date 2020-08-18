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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.badamson.mc.Player;

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
      public Mono<Long> count() {
         return Mono.fromSupplier(() -> Long.valueOf(users.size()));
      }

      @Override
      public Mono<Void> delete(final User user) {
         requireNonNull(user, "user");
         return deleteById(user.getUsername());
      }

      @Override
      public Mono<Void> deleteAll() {
         return Mono.fromRunnable(() -> users.clear());
      }

      @Override
      public Mono<Void> deleteAll(final Iterable<? extends User> users) {
         requireNonNull(users, "users");
         return deleteAllOfFlux(Flux.fromIterable(users));
      }

      @Override
      public Mono<Void> deleteAll(final Publisher<? extends User> users) {
         requireNonNull(users, "users");
         return deleteAllOfFlux(Flux.from(users));
      }

      private Mono<Void> deleteAllOfFlux(final Flux<? extends User> users) {
         return users.map(user -> this.users.remove(user.getUsername()))
                  .then();
      }

      @Override
      public Mono<Void> deleteById(final Publisher<String> username) {
         requireNonNull(username, "username");
         return Mono.from(username).map(u -> users.remove(u)).then();
      }

      @Override
      public Mono<Void> deleteById(final String username) {
         requireNonNull(username, "username");
         return Mono.fromRunnable(() -> users.remove(username));
      }

      @Override
      public Mono<Boolean> existsById(final Publisher<String> username) {
         requireNonNull(username, "username");
         return Mono.from(username).flatMap(u -> existsById(u));
      }

      @Override
      public Mono<Boolean> existsById(final String username) {
         requireNonNull(username, "username");
         return Mono.fromSupplier(() -> users.containsKey(username));
      }

      @Override
      public Flux<User> findAll() {
         return Mono.fromSupplier(() -> users.values())
                  .flatMapIterable(c -> c);
      }

      private Flux<User> findAllByFluxOfUsernames(
               final Flux<String> usernames) {
         return usernames.map(username -> users.get(username))
                  .filter(user -> user != null);
      }

      @Override
      public Flux<User> findAllById(final Iterable<String> usernames) {
         requireNonNull(usernames, "usernames");
         return findAllByFluxOfUsernames(Flux.fromIterable(usernames));
      }

      @Override
      public Flux<User> findAllById(final Publisher<String> usernames) {
         requireNonNull(usernames, "usernames");
         return findAllByFluxOfUsernames(Flux.from(usernames));
      }

      @Override
      public Mono<User> findById(final Publisher<String> username) {
         requireNonNull(username, "username");
         return Mono.from(username).flatMap(u -> findById(u));
      }

      @Override
      public Mono<User> findById(final String username) {
         requireNonNull(username, "username");
         return Mono.fromSupplier(() -> users.get(username))
                  .filter(p -> p != null);
      }

      @Override
      public Mono<User> save(final User user) {
         requireNonNull(user, "user");
         return Mono.fromSupplier(() -> {
            users.put(user.getUsername(), user);
            return user;
         });
      }

      @Override
      public <P extends User> Flux<P> saveAll(final Iterable<P> users) {
         requireNonNull(users, "users");
         return saveAllOfFlux(Flux.fromIterable(users));
      }

      @Override
      public <S extends User> Flux<S> saveAll(final Publisher<S> users) {
         requireNonNull(users, "users");
         return saveAllOfFlux(Flux.from(users));
      }

      private <P extends User> Flux<P> saveAllOfFlux(final Flux<P> users) {
         return users.map(user -> {
            this.users.put(user.getUsername(), user);
            return user;
         });
      }

   }

   public static void assertInvariants(final UserRepository repository) {
      // Do nothing
   }

   public static Mono<User> findById(final UserRepository repository,
            final String username) {
      final var publisher = repository.findById(username);

      assertInvariants(repository);
      assertNotNull(publisher, "result");
      final var user = publisher.block();
      if (user != null) {
         assertEquals(username, user.getUsername(),
                  "Found the user with the given ID");
      }

      return publisher;
   }

   public static Mono<User> save(final UserRepository repository,
            final User user) {
      final var publisher = repository.save(user);

      assertInvariants(repository);
      assertNotNull(publisher, "result");

      return publisher;
   }
}
