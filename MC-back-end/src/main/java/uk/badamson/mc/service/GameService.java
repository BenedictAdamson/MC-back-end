package uk.badamson.mc.service;
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

import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.springframework.dao.DataAccessException;

import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;

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
    * <h2>Post Conditions</h2>
    * <ul>
    * <li>Always return a (non null) game.</li>
    * <li>The returned game has the given {@code scenario} as the
    * {@linkplain Identifier#getScenario() scenario} of its
    * {@linkplain Game#getIdentifier() identifier}.</li>
    * <li>The returned game has the {@linkplain Clock#instant() current time}
    * (as given by the {@linkplain #getClock() associated clock} of this
    * service) as the {@linkplain Identifier#getCreated() creation time} of its
    * {@linkplain Game#getIdentifier() identifier}.</li>
    * <li>The returned game can be {@linkplain #getGame(Identifier) retrieved}
    * later, using its {@linkplain Game#getIdentifier() identifier}.</li>
    * </ul>
    *
    * @param scenario
    *           The unique ID of the scenario of interest.
    * @return The created game.
    * @throws NullPointerException
    *            If {@code scenario} is null.
    * @throws NoSuchElementException
    *            If {@code scenario} is not the ID of a recognised scenario.
    * @throws DataAccessException
    *            If the service could not create the game because of a problem
    *            accessing a repository.
    */
   @Nonnull
   Game create(@Nonnull UUID scenario);

   /**
    * <p>
    * The clock that this service uses to access to the current
    * {@linkplain Instant instant} (point in time).
    * </p>
    * <ul>
    * <li>Not null.</li>
    * </ul>
    *
    * @return the clock
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
    * times to create the {@linkplain Identifier identifiers} of the games for
    * the given scenario.
    * </p>
    * <ul>
    * <li>Always returns a (non null) stream.</li>
    * <li>The returned stream will not include a null element</li>
    * <li>Does not contain {@linkplain Instant#equals(Object) duplicate}
    * identifiers.</li>
    * </ul>
    *
    * @param scenario
    *           The unique ID of the scenario of interest.
    * @return The creation times
    * @throws NullPointerException
    *            If {@code scenario} is null.
    */
   @Nonnull
   Stream<Instant> getCreationTimesOfGamesOfScenario(@Nonnull UUID scenario);

   /**
    * <p>
    * Retrieve the game that has a given unique ID.
    * </p>
    * <ul>
    * <li>Returns a (non null) optional value.</li>
    * <li>Returns an {@linkplain Optional#isEmpty() empty} value, or a value for
    * which the {@linkplain Game#getIdentifier() identifier}
    * {@linkplain Identifier#equals(Object) is equivalent to} the given ID</li>
    * </ul>
    *
    * @param id
    *           The unique ID of the wanted game.
    * @return The game.
    * @throws NullPointerException
    *            If {@code id} is null.
    */
   @Nonnull
   Optional<Game> getGame(@Nonnull final Game.Identifier id);

   /**
    * <p>
    * Retrieve a stream of the identifiers of the games of this instance of the
    * Mission Command game.
    * </p>
    * <ul>
    * <li>Always returns a (non null) stream.</li>
    * <li>The returned stream will not include a null element</li>
    * <li>Does not contain {@linkplain Identifier#equals(Object) duplicate}
    * identifiers.</li>
    * </ul>
    *
    * @return a {@linkplain Stream stream} of the game identifiers.
    */
   @Nonnull
   Stream<Game.Identifier> getGameIdentifiers();
}
