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

import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
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

   private final Clock clock;

   private final ScenarioService scenarioService;

   /**
    * <p>
    * Construct a service with give associations.
    * </p>
    * <ul>
    * <li>The created service has the given {@code repository} as its
    * {@linkplain #getRepository() repository}.</li>
    * <li>The created service has the given {@code clock} as its
    * {@linkplain #getClock() clock}.</li>
    * <li>The created service has the given {@code scenarioService} as its
    * {@linkplain #getScenarioService() scenario service}.</li>
    * </ul>
    *
    * @param repository
    *           The repository that this service uses for persistent storage.
    * @param clock
    *           The clock that this service uses to access to the current
    *           {@linkplain Instant instant} (point in time).
    * @param scenarioService
    *           The part of the service layer that this service uses for
    *           information about scenarios.
    * @throws NullPointerException
    *            <ul>
    *            <li>If {@code repository} is null.</li>
    *            <li>If {@code clock} is null.</li>
    *            <li>If {@code scenarioService} is null.</li>
    *            </ul>
    */
   public GameServiceImpl(@Nonnull final GameRepository repository,
            @Nonnull final Clock clock,
            @Nonnull final ScenarioService scenarioService) {
      this.repository = Objects.requireNonNull(repository, "repository");
      this.clock = Objects.requireNonNull(clock, "clock");
      this.scenarioService = Objects.requireNonNull(scenarioService,
               "scenarioService");
   }

   @Override
   @Nonnull
   public Game create(@Nonnull final UUID scenario) {
      requireKnownScenario(scenario);
      final var identifier = new Game.Identifier(scenario, clock.instant());
      final var game = new Game(identifier);
      return repository.save(game);
   }

   @Nonnull
   @Override
   public final Clock getClock() {
      return clock;
   }

   @Override
   public Stream<Instant> getCreationTimesOfGamesOfScenario(
            final UUID scenario) {
      requireKnownScenario(scenario);
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

   @Override
   @Nonnull
   public final ScenarioService getScenarioService() {
      return scenarioService;
   }

   private void requireKnownScenario(final UUID scenario) {
      Objects.requireNonNull(scenario, "scenario");
      if (!scenarioService.getScenarioIdentifiers()
               .anyMatch(id -> scenario.equals(id))) {
         throw new NoSuchElementException("unknown scenario");
      }
   }

}
