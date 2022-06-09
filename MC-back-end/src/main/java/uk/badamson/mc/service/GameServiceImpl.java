package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2020-22.
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
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.repository.GameRepository;

/**
 * <p>
 * Implementation of the part of the service layer pertaining to games of
 * Mission Command.
 * </p>
 */
@Service
public class GameServiceImpl implements GameService {

   private final GameRepository repository;

   private final Clock clock;

   private final ScenarioService scenarioService;

   /**
    * <p>
    * Construct a service with given associations.
    * </p>
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
   @Autowired
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
   @Transactional
   public Game create(@Nonnull final UUID scenario) {
      requireKnownScenario(scenario);// read-and-check
      final var identifier = new Identifier(scenario, getNow());
      final var game = new Game(identifier, Game.RunState.WAITING_TO_START);
      return repository.save(game);// write
   }

   private Optional<Game> get(final Identifier id) {
      Objects.requireNonNull(id, "id");
      return repository.findById(id);
   }

   @Nonnull
   @Override
   public final Clock getClock() {
      return clock;
   }

   @Nonnull
   @Override
   @Transactional
   public Stream<Instant> getCreationTimesOfGamesOfScenario(@Nonnull final UUID scenario)
            throws NoSuchElementException {
      requireKnownScenario(scenario);// read-and-check
      return getGameIdentifiers()// read
               .filter(id -> scenario.equals(id.getScenario()))
               .map(Identifier::getCreated);
   }

   @Override
   @Nonnull
   public Optional<Game> getGame(@Nonnull final Identifier id) {
      return get(id);
   }

   @Override
   @Nonnull
   public Stream<Identifier> getGameIdentifiers() {
      return getGames().map(Game::getIdentifier);
   }

   private Stream<Game> getGames() {
      return StreamSupport.stream(repository.findAll().spliterator(), false);
   }

   @Override
   @Nonnull
   public Instant getNow() {
      return clock.instant().truncatedTo(ChronoUnit.MILLIS);
   }

   @Nonnull
   public final GameRepository getRepository() {
      return repository;
   }

   @Override
   @Nonnull
   public final ScenarioService getScenarioService() {
      return scenarioService;
   }

   private void requireKnownScenario(final UUID scenario)
            throws NoSuchElementException {
      Objects.requireNonNull(scenario, "scenario");
      if (scenarioService.getScenarioIdentifiers()
               .noneMatch(scenario::equals)) {
         throw new NoSuchElementException("unknown scenario");
      }
   }

   @Override
   @Nonnull
   public Game startGame(@Nonnull final Identifier id)
            throws NoSuchElementException, IllegalGameStateException {
      Optional<Game> gameOptional = get(id);
      if (gameOptional.isEmpty()) {
         throw new NoSuchElementException("game");
      }
      var game = gameOptional.get();// read
      switch (game.getRunState()) {
      case WAITING_TO_START:
         game = new Game(game);
         game.setRunState(Game.RunState.RUNNING);
         return repository.save(game);// write
      case RUNNING:
         // do nothing
         return new Game(game);
      case STOPPED:
         throw new IllegalGameStateException("Game stopped");
      default:// never happens
         throw new AssertionError("Valid game state");
      }
   }

   @Override
   public void stopGame(@Nonnull final Identifier id)
            throws NoSuchElementException {
      Optional<Game> gameOptional = get(id);
      if (gameOptional.isEmpty()) {
         throw new NoSuchElementException("game");
      }
      var game = gameOptional.get();// read
      switch (game.getRunState()) {
         case WAITING_TO_START, RUNNING -> {
            game = new Game(game);
            game.setRunState(Game.RunState.STOPPED);
            repository.save(game);
            // write
         }
         case STOPPED -> // do nothing
                 new Game(game);
         default ->// never happens
                 throw new AssertionError("Valid game state");
      }
   }

}
