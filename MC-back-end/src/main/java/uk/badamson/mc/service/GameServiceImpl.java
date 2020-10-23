package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2020.
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

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.repository.GameRepository;

public class GameServiceImpl implements GameService {

   private final GameRepository repository;

   /**
    * <p>
    * Construct a service that uses a give repository.
    * </p>
    * <ul>
    * <li>The created service has the given {@code repository} as its
    * {@linkplain #getRepository() repository}.</li>
    * </ul>
    *
    * @param repository
    *           The repository that this service uses for persistent storage.
    * @throws NullPointerException
    *            If {@code repository} is null.
    */
   public GameServiceImpl(final GameRepository repository) {
      this.repository = Objects.requireNonNull(repository, "repository");
   }

   @Override
   public Stream<Instant> getCreationTimesOfGamesOfScenario(
            final UUID scenario) {
      return getGameIdentifiers()
               .filter(id -> scenario.equals(id.getScenario()))
               .map(gameId -> gameId.getCreated());
   }

   @Override
   @Nonnull
   public Optional<Game> getGame(@Nonnull final Game.Identifier id) {
      return repository.findById(id);
   }

   @Override
   @Nonnull
   public Stream<Identifier> getGameIdentifiers() {
      return getGames().map(game -> game.getIdentifier());
   }

   private Stream<Game> getGames() {
      return StreamSupport.stream(repository.findAll().spliterator(), false);
   }

   /**
    * <p>
    * The repository that this service uses for persistent storage.
    * </p>
    * <ul>
    * <li>Always have a (non null) repository.</li>
    * </ul>
    *
    * @return the repository
    */
   @Nonnull
   public final GameRepository getRepository() {
      return repository;
   }

}
