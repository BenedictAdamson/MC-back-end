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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

import uk.badamson.mc.Game;

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link GameRepository}
 * interface.
 */
public class GameRepositoryTest {

   public static final class Fake implements GameRepository {

      private static void requireNonNull(final Object object,
               final String message) {
         if (object == null) {
            throw new IllegalArgumentException("Null " + message);
         }
      }

      private final Map<Game.Identifier, Game> games = new ConcurrentHashMap<>();

      @Override
      public long count() {
         return games.size();
      }

      @Override
      public void delete(final Game game) {
         requireNonNull(game, "game");
         deleteById(game.getIdentifier());
      }

      @Override
      public void deleteAll() {
         games.clear();
      }

      @Override
      public void deleteAll(final Iterable<? extends Game> games) {
         requireNonNull(games, "games");
         for (final Game game : games) {
            this.games.remove(game.getIdentifier());
         }
      }

      @Override
      public void deleteById(final Game.Identifier identifier) {
         requireNonNull(identifier, "identifier");
         games.remove(identifier);
      }

      @Override
      public boolean existsById(final Game.Identifier identifier) {
         requireNonNull(identifier, "identifier");
         return games.containsKey(identifier);
      }

      @Override
      public Iterable<Game> findAll() {
         // Return copies of the games so we are isolated from downstream
         // mutations
         return games.values().stream().map(game -> new Game(game))
                  .collect(toList());
      }

      @Override
      public Iterable<Game> findAllById(
               final Iterable<Game.Identifier> identifiers) {
         requireNonNull(identifiers, "identifiers");
         // Return copies of the games so we are isolated from downstream
         // mutations
         return StreamSupport.stream(identifiers.spliterator(), false)
                  .distinct().map(un -> games.get(un)).filter(un -> un != null)
                  .map(game -> new Game(game)).collect(toUnmodifiableList());
      }

      @Override
      public Optional<Game> findById(final Game.Identifier identifier) {
         requireNonNull(identifier, "identifier");
         var game = games.get(identifier);
         // Return copy so we are isolated from downstream mutations
         if (game != null) {
            game = new Game(game);
         }
         return Optional.ofNullable(game);
      }

      @Override
      public <GAME extends Game> GAME save(final GAME game) {
         requireNonNull(game, "game");
         // Save a copy to we are insulated from changes to the given game
         // object.
         games.put(game.getIdentifier(), new Game(game));
         return game;
      }

      @Override
      public <GAME extends Game> Iterable<GAME> saveAll(
               final Iterable<GAME> games) {
         requireNonNull(games, "games");
         for (final var game : games) {
            save(game);
         }
         return games;
      }

   }

   public static void assertInvariants(final GameRepository repository) {
      // Do nothing
   }

}
