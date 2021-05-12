package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2019-21.
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.springframework.dao.DataAccessException;

import uk.badamson.mc.Game;

/**
 * <p>
 * The part of the service layer pertaining to games of Mission Command.
 * </p>
 */
public interface GameService {

   /**
    * <p>
    * Create a new game for a given scenario.
    * </p>
    *
    * @param scenario
    *           The unique ID of the scenario of interest.
    * @return The created game.
    * @throws NullPointerException
    *            If {@code scenario} is null.
    * @throws NoSuchElementException
    *            If {@code scenario} is not the ID of a recognised scenario.
    *            That is, if {@code scenario} is not one of the
    *            {@linkplain ScenarioService#getScenarioIdentifiers()
    *            identifiers} of the associated
    *            {@linkplain #getScenarioService() scenario service}.
    * @throws DataAccessException
    *            If the service could not create the game because of a problem
    *            accessing a repository.
    */
   @Nonnull
   Game create(@Nonnull UUID scenario)
            throws DataAccessException, NoSuchElementException;

   /**
    * @see #getNow()
    */
   @Nonnull
   Clock getClock();

   /**
    * <p>
    * Retrieve a stream of the creation times of the games that are for a given
    * scenario.
    * </p>
    * <p>
    * The given {@code scenario} ID could be combined with the returned creation
    * times to create the identifiers of the games for the given scenario.
    * </p>
    *
    * @param scenario
    *           The unique ID of the scenario of interest.
    * @throws NullPointerException
    *            If {@code scenario} is null.
    * @throws NoSuchElementException
    *            If {@code scenario} is not the ID of a recognised scenario.
    *            That is, if {@code scenario} is not one of the
    *            {@linkplain ScenarioService#getScenarioIdentifiers()
    *            identifiers} of the associated
    *            {@linkplain #getScenarioService() scenario service}.
    */
   @Nonnull
   Stream<Instant> getCreationTimesOfGamesOfScenario(@Nonnull UUID scenario)
            throws NoSuchElementException;

   /**
    * <p>
    * Retrieve the game that has a given unique ID.
    * </p>
    *
    * @param id
    *           The unique ID of the wanted game.
    * @throws NullPointerException
    *            If {@code id} is null.
    */
   @Nonnull
   Optional<Game> getGame(@Nonnull final Game.Identifier id);

   /**
    * <p>
    * Retrieve a stream of the identifiers of all the games of this instance of
    * the Mission Command game.
    * </p>
    */
   @Nonnull
   Stream<Game.Identifier> getGameIdentifiers();

   /**
    * <p>
    * Retrieve the current point in time, using the {@linkplain #getClock()
    * clock} associated with this service, truncated to practical precision.
    * </p>
    * <p>
    * Ideally, we would simply delegate to {@link Clock#instant()}.
    * Unfortunately, some repository implementations (including the MongoDB
    * Spring Data repository) can not store very high precision time-stamps, but
    * are limited to millisecond precision. So we must truncate any excess
    * precision in some cases.
    * </p>
    */

   @Nonnull
   Instant getNow();

   @Nonnull
   ScenarioService getScenarioService();

   @Nonnull
   Game startGame(@Nonnull Game.Identifier id)
            throws NoSuchElementException, IllegalGameStateException;

   @Nonnull
   Game stopGame(@Nonnull Game.Identifier id) throws NoSuchElementException;
}
