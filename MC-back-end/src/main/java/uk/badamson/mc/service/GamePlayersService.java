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

import java.security.AccessControlException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import uk.badamson.mc.Authority;
import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.GamePlayers;
import uk.badamson.mc.User;

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
   GamePlayers endRecruitment(@Nonnull Game.Identifier id)
            throws NoSuchElementException;

   /**
    * <p>
    * The {@linkplain Game#getIdentifier() unique ID} of the <i>current game</i>
    * of a user who has a given {@linkplain User#getId() unique ID}.
    * </p>
    * <ul>
    * <li>Returns a (non null) optional value.</li>
    * <li>An {@linkplain Optional#isEmpty() empty} return value indicates either
    * that the user is not currently playing a game, or that he associated
    * {@linkplain #getUserService() user service} indicates that there is no
    * user with the give {@code user} ID.</li>
    * </ul>
    *
    * @param user
    *           The unique ID of the user.
    * @return the current game of the user
    * @throws NullPointerException
    *            If {@code user} is null
    */
   @Nonnull
   Optional<Game.Identifier> getCurrentGameOfUser(@Nonnull UUID user);

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
   Optional<GamePlayers> getGamePlayers(@Nonnull Game.Identifier id);

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

   /**
    * <p>
    * The part of the service layer that this service uses for information about
    * users.
    * </p>
    * <ul>
    * <li>Not null.</li>
    * </ul>
    *
    * @return the user service
    */
   @Nonnull
   UserService getUserService();

   /**
    * <p>
    * Have a {@linkplain User user} become one of the
    * {@linkplain GamePlayers#getUsers() players of a game}.
    * </p>
    *
    * @param user
    *           The unique ID of the player.
    * @param game
    *           The unique ID of the game.
    * @throws NullPointerException
    *            <ul>
    *            <li>If {@code user} is null.</li>
    *            <li>If {@code game} is null.</li>
    *            </ul>
    * @throws NoSuchElementException
    *            <ul>
    *            <li>If {@code user} is not the ID of a known user, according to
    *            the associated {@linkplain #getUserService() user
    *            service}.</li>
    *            <li>If {@code game} is not the ID of a known user, according to
    *            the associated {@linkplain #getGameService() game
    *            service}.</li>
    *            </ul>
    * @throws UserAlreadyPlayingException
    *            If the {@code user} is already playing a game.
    * @throws AccessControlException
    *            If the {@code does not  User#getAuthorities() have}
    *            {@linkplain Authority#ROLE_PLAYER permission} to play games.
    *            Note that the given user need not be the current user.
    * @throws IllegalGameStateException
    *            If the game is not {@linkplain GamePlayers#isRecruiting()
    *            recruiting} players.
    */
   void userJoinsGame(@Nonnull UUID user, @Nonnull Game.Identifier game)
            throws NoSuchElementException, UserAlreadyPlayingException,
            IllegalGameStateException, AccessControlException;
}
