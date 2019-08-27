package uk.badamson.mc.repository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.badamson.mc.Player;
import uk.badamson.mc.PlayerTest;

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link PlayerRepository}
 * interface.
 */
public class PlayerRepositoryTest {

   public static final class Fake implements PlayerRepository {

      private static void requireNonNull(final Object object,
               final String message) {
         if (object == null) {
            throw new IllegalArgumentException("Null " + message);
         }
      }

      private final Map<String, Player> players = new ConcurrentHashMap<>();

      @Override
      public Mono<Long> count() {
         return Mono.fromSupplier(() -> Long.valueOf(players.size()));
      }

      @Override
      public Mono<Void> delete(final Player player) {
         requireNonNull(player, "player");
         return deleteById(player.getUsername());
      }

      @Override
      public Mono<Void> deleteAll() {
         return Mono.fromRunnable(() -> players.clear());
      }

      @Override
      public Mono<Void> deleteAll(final Iterable<? extends Player> players) {
         requireNonNull(players, "players");
         return deleteAllOfFlux(Flux.fromIterable(players));
      }

      @Override
      public Mono<Void> deleteAll(final Publisher<? extends Player> players) {
         requireNonNull(players, "players");
         return deleteAllOfFlux(Flux.from(players));
      }

      private Mono<Void> deleteAllOfFlux(final Flux<? extends Player> players) {
         return players.map(player -> this.players.remove(player.getUsername()))
                  .then();
      }

      @Override
      public Mono<Void> deleteById(final Publisher<String> username) {
         requireNonNull(username, "username");
         return Mono.from(username).map(u -> players.remove(u)).then();
      }

      @Override
      public Mono<Void> deleteById(final String username) {
         requireNonNull(username, "username");
         return Mono.fromRunnable(() -> players.remove(username));
      }

      @Override
      public Mono<Boolean> existsById(final Publisher<String> username) {
         requireNonNull(username, "username");
         return Mono.from(username).flatMap(u -> existsById(u));
      }

      @Override
      public Mono<Boolean> existsById(final String username) {
         requireNonNull(username, "username");
         return Mono.fromSupplier(() -> players.containsKey(username));
      }

      @Override
      public Flux<Player> findAll() {
         return Mono.fromSupplier(() -> players.values())
                  .flatMapIterable(c -> c);
      }

      private Flux<Player> findAllByFluxOfUsernames(
               final Flux<String> usernames) {
         return usernames.map(username -> players.get(username))
                  .filter(player -> player != null);
      }

      @Override
      public Flux<Player> findAllById(final Iterable<String> usernames) {
         requireNonNull(usernames, "usernames");
         return findAllByFluxOfUsernames(Flux.fromIterable(usernames));
      }

      @Override
      public Flux<Player> findAllById(final Publisher<String> usernames) {
         requireNonNull(usernames, "usernames");
         return findAllByFluxOfUsernames(Flux.from(usernames));
      }

      @Override
      public Mono<Player> findById(final Publisher<String> username) {
         requireNonNull(username, "username");
         return Mono.from(username).flatMap(u -> findById(u));
      }

      @Override
      public Mono<Player> findById(final String username) {
         requireNonNull(username, "username");
         return Mono.fromSupplier(() -> players.get(username))
                  .filter(p -> p != null);
      }

      @Override
      public Mono<Player> save(final Player player) {
         requireNonNull(player, "player");
         return Mono.fromSupplier(() -> {
            players.put(player.getUsername(), player);
            return player;
         });
      }

      @Override
      public <P extends Player> Flux<P> saveAll(final Iterable<P> players) {
         requireNonNull(players, "players");
         return saveAllOfFlux(Flux.fromIterable(players));
      }

      @Override
      public <S extends Player> Flux<S> saveAll(final Publisher<S> players) {
         requireNonNull(players, "players");
         return saveAllOfFlux(Flux.from(players));
      }

      private <P extends Player> Flux<P> saveAllOfFlux(final Flux<P> players) {
         return players.map(player -> {
            this.players.put(player.getUsername(), player);
            return player;
         });
      }

   }

   public static void assertInvariants(final PlayerRepository repository) {
      // Do nothing
   }

   public static Mono<Player> findById(final PlayerRepository repository,
            final String username) {
      final var publisher = repository.findById(username);

      assertInvariants(repository);
      assertNotNull(publisher, "result");
      final var player = publisher.block();
      if (player != null) {
         PlayerTest.assertInvariants(player);
         assertEquals(username, player.getUsername(),
                  "Found the player with the given ID");
      }

      return publisher;
   }

   public static Mono<Player> save(final PlayerRepository repository,
            final Player player) {
      final var publisher = repository.save(player);

      assertInvariants(repository);
      PlayerTest.assertInvariants(player);
      assertNotNull(publisher, "result");

      return publisher;
   }
}
