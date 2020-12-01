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

import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.Nonnull;

import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.GamePlayers;

/**
 * <p>
 * The part of the service layer pertaining to players of games of Mission
 * Command.
 * </p>
 * <p>
 * By default (that is, if the game players for a game has never been changed),
 * the game players of a game that exists
 * </p>
 * <ul>
 * <li>{@linkplain GamePlayers#isRecruiting() is recruiting}, and</li>
 * <li>no {@linkplain GamePlayers#getUsers() users} are playing it</li>
 * </ul>
 */
public interface GamePlayersService {

   /**
    * <p>
    * Indicate that a game is not {@linkplain GamePlayers#isRecruiting()
    * recruiting} players (any longer).
    * </p>
    * <p>
    * This mutator is idempotent: the mutator does not have the precondition
    * that the game is recruiting.
    * <ul>
    * <li>Returns a (non null) value.</li>
    * <li>The {@linkplain GamePlayers#getGame() game} ID of the returned value
    * {@linkplain Identifier#equals(Object) is equivalent to} the given ID</li>
    * <li>The returned value is not {@linkplain GamePlayers#isRecruiting()
    * recruiting}.</li>
    * <li>On return, subsequent {@linkplain #getGamePlayers(Identifier)
    * retrieval} of the game players using an identifier equivalent to the given
    * ID returns a value that is also not recruiting. That is, the method also
    * saves the mutated value.</li>
    * </ul>
    *
    * @param id
    *           The unique ID of the game to mutate.
    * @return The mutated game players information.
    * @throws NullPointerException
    *            If {@code id} is null.
    * @throws NoSuchElementException
    *            If the associated {@linkplain #getGameService() game service}
    *            indicates that a {@linkplain GameService#getGame(Identifier)
    *            game} with the given ID does not exist.
    */
   @Nonnull
   GamePlayers endRecruitment(@Nonnull final Game.Identifier id)
            throws NoSuchElementException;

   /**
    * <p>
    * Retrieve the game players for the game that has a given unique ID.
    * </p>
    * <ul>
    * <li>Returns a (non null) optional value.</li>
    * <li>Returns either an {@linkplain Optional#isEmpty() empty} value, or a
    * value for which the {@linkplain GamePlayers#getGame() game} ID
    * {@linkplain Identifier#equals(Object) is equivalent to} the given ID</li>
    * <li>Returns a {@linkplain Optional#isPresent() present} value if, and only
    * if, the associated {@linkplain #getGameService() game service} indicates
    * that a {@linkplain GameService#getGame(Identifier) game} with the given ID
    * exists.</li>
    * </ul>
    *
    * @param id
    *           The unique ID of the game.
    * @return The game players.
    * @throws NullPointerException
    *            If {@code id} is null.
    */
   @Nonnull
   Optional<GamePlayers> getGamePlayers(@Nonnull final Game.Identifier id);

   /**
    * <p>
    * The part of the service layer that this service uses for information about
    * games.
    * </p>
    * <ul>
    * <li>Not null.</li>
    * </ul>
    *
    * @return the game service
    */
   @Nonnull
   GameService getGameService();
}
