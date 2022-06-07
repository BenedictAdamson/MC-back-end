package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2019-22.
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
    *
    * @param id
    *           The unique ID of the game to mutate.
    * @return The mutated game players' information.
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
    * <li>An {@linkplain Optional#isEmpty() empty} return value indicates either
    * that the user is not currently playing a game, or there is no user with
    * the give {@code user} ID.</li>
    * </ul>
    *
    * @param user
    *           The unique ID of the user.
    * @throws NullPointerException
    *            If {@code user} is null
    */
   @Nonnull
   Optional<Game.Identifier> getCurrentGameOfUser(@Nonnull UUID user);

   /**
    * <p>
    * Retrieve complete information about the game players for the game that has
    * a given unique ID.
    * </p>
    *
    * @param id
    *           The unique ID of the game.
    * @throws NullPointerException
    *            If {@code id} is null.
    */
   @Nonnull
   Optional<GamePlayers> getGamePlayersAsGameManager(
            @Nonnull Game.Identifier id);

   /**
    * <p>
    * Retrieve information about the game players for the game that has a given
    * unique ID, suitable for a non game manager.
    * </p>
    * <ul>
    * <li>The collection of {@linkplain GamePlayers#getUsers() players} is
    * either empty or contains only the requesting user: non game managers may
    * not see the complete list of players of a game, but may see that they are
    * a player of a game.</li>
    * </ul>
    *
    * @param id
    *           The unique ID of the game.
    * @param user
    *           The (unique ID) of the user requesting the information
    * @throws NullPointerException
    *            <ul>
    *            <li>If {@code id} is null.</li>
    *            <li>If {@code user} is null.</li>
    *            </ul>
    */
   @Nonnull
   Optional<GamePlayers> getGamePlayersAsNonGameManager(
            @Nonnull Game.Identifier id, @Nonnull UUID user);

   @Nonnull
   GameService getGameService();

   @Nonnull
   UserService getUserService();

   /**
    * <p>
    * Whether the {@link #userJoinsGame(UUID, Identifier)} operation would
    * succeed
    * </p>
    * <p>
    * That is, whether all the following are true.
    * </p>
    * <ul>
    * <li>The{@code user} is the ID of a known user, according to the associated
    * {@linkplain #getUserService() user service}.</li>
    * <li>The {@code game} is the ID of a known game, according to the
    * associated {@linkplain #getGameService() game service}.</li>
    * <li>The {@code user} is not already playing a different game.</li>
    * <li>The {@code user} {@linkplain User#getAuthorities() has}
    * {@linkplain Authority#ROLE_PLAYER permission} to play games. Note that the
    * given user need not be the current user.</li>
    * <li>The user has already joined the game <em>or</em> the game is
    * {@linkplain GamePlayers#isRecruiting() recruiting} players.</li>
    * </ul>
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
    */
   boolean mayUserJoinGame(@Nonnull UUID user, @Nonnull Game.Identifier game);

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
    *            <li>If {@code game} is not the ID of a game, according to the
    *            associated {@linkplain #getGameService() game service}.</li>
    *            </ul>
    * @throws UserAlreadyPlayingException
    *            If the {@code user} is already playing a different game.
    * @throws AccessControlException
    *            If the {@code user} does not {@linkplain User#getAuthorities()
    *            have} {@linkplain Authority#ROLE_PLAYER permission} to play
    *            games. Note that the given user need not be the current user.
    * @throws IllegalGameStateException
    *            <ul>
    *            <li>If the game is not {@linkplain GamePlayers#isRecruiting()
    *            recruiting} players.</li>
    *            <li>If the game has no characters free.</li>
    *            </ul>
    */
   void userJoinsGame(@Nonnull UUID user, @Nonnull Game.Identifier game)
            throws NoSuchElementException, UserAlreadyPlayingException,
            IllegalGameStateException, AccessControlException;
}
