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

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.GamePlayers;
import uk.badamson.mc.UserGameAssociation;
import uk.badamson.mc.repository.CurrentUserGameRepository;
import uk.badamson.mc.repository.GamePlayersRepository;

/**
 * <p>
 * Implementation of the part of the service layer pertaining to players of
 * games of Mission Command.
 * </p>
 */
@Service
public class GamePlayersServiceImpl implements GamePlayersService {

   private static final Set<UUID> NO_USERS = Set.of();

   private static GamePlayers createDefault(final Game.Identifier id) {
      return new GamePlayers(id, true, NO_USERS);
   }

   private final GamePlayersRepository gamePlayersRepository;

   private final CurrentUserGameRepository currentUserGameRepository;

   private final GameService gameService;

   private final UserService userService;

   /**
    * <p>
    * Construct a service with given associations.
    * </p>
    * <ul>
    * <li>The created service has the given {@code gamePlayersRepository} as its
    * {@linkplain #getGamePlayersRepository() game players repository}.</li>
    * <li>The created service has the given {@code gamePlayersRepository} as its
    * {@linkplain #getCurrentUserGameRepository() current user game
    * repository}.</li>
    * <li>The created service has the given {@code gameService} as its
    * {@linkplain #getGameService() game service}.</li>
    * <li>The created service has the given {@code userService} as its
    * {@linkplain #getUserService() user service}.</li>
    * </ul>
    *
    * @param gamePlayersRepository
    *           The repository that this service uses for persistent storage of
    *           {@link GamePlayers} objects.
    * @param currentUserGameRepository
    *           The repository that this service uses for persistent storage of
    *           {@link UserGameAssociation} objects that indicate the current
    *           game of each user.
    * @param gameService
    *           The part of the service layer that this service uses for
    *           information about scenarios.
    * @param userService
    *           The part of the service layer that this service uses for
    *           information about users.
    * @throws NullPointerException
    *            <ul>
    *            <li>If {@code gamePlayersRepository} is null.</li>
    *            <li>If {@code currentUserGameRepository} is null.</li>
    *            <li>If {@code gameService} is null.</li>
    *            <li>If {@code userService} is null.</li>
    *            </ul>
    */
   @Autowired
   public GamePlayersServiceImpl(
            @Nonnull final GamePlayersRepository gamePlayersRepository,
            @Nonnull final CurrentUserGameRepository currentUserGameRepository,
            @Nonnull final GameService gameService,
            @Nonnull final UserService userService) {
      this.gamePlayersRepository = Objects.requireNonNull(gamePlayersRepository,
               "gamePlayersRepository");
      this.currentUserGameRepository = Objects.requireNonNull(
               currentUserGameRepository, "currentUserGameRepository");
      this.gameService = Objects.requireNonNull(gameService, "gameService");
      this.userService = Objects.requireNonNull(userService, "userService");
   }

   @Override
   @Nonnull
   public GamePlayers endRecruitment(@Nonnull final Identifier id)
            throws NoSuchElementException {
      final var players = get(id).get();
      players.endRecruitment();
      return gamePlayersRepository.save(players);
   }

   private Optional<GamePlayers> get(final Game.Identifier id) {
      Objects.requireNonNull(id, "id");
      var result = Optional.<GamePlayers>empty();
      if (gameService.getGame(id).isPresent()) {
         result = gamePlayersRepository.findById(id);
         if (result.isEmpty()) {
            result = Optional.of(createDefault(id));
         }
      }
      return result;
   }

   /**
    * <p>
    * The repository that this service uses for persistent storage of
    * {@link UserGameAssociation} objects that indicate the current game of each
    * user.
    * </p>
    * <ul>
    * <li>Always have a (non null) repository.</li>
    * </ul>
    *
    * @return the repository
    */
   @Nonnull
   public final CurrentUserGameRepository getCurrentUserGameRepository() {
      return currentUserGameRepository;
   }

   /**
    * {@inheritDoc}
    * <p>
    * Furthermore:
    * </p>
    * <ul>
    * <li>If returns a {@linkplain Optional#isPresent() present} value, and the
    * associated {@linkplain #getGamePlayersRepository() repository}
    * {@linkplain GamePlayersRepository#findById(Identifier) has} a stored value
    * with the given ID, that returned value is the value retrieved from the
    * repository.</li>
    * </ul>
    *
    * @param id
    *           {@inheritDoc}
    * @return {@inheritDoc}
    * @throws NullPointerException
    *            {@inheritDoc}
    */
   @Override
   @Nonnull
   public Optional<GamePlayers> getGamePlayers(
            @Nonnull final Game.Identifier id) {
      return get(id);
   }

   /**
    * <p>
    * The repository that this service uses for persistent storage of
    * {@link GamePlayers} objects.
    * </p>
    * <ul>
    * <li>Always have a (non null) repository.</li>
    * </ul>
    *
    * @return the repository
    */
   @Nonnull
   public final GamePlayersRepository getGamePlayersRepository() {
      return gamePlayersRepository;
   }

   @Override
   @Nonnull
   public final GameService getGameService() {
      return gameService;
   }

   @Override
   @Nonnull
   public UserService getUserService() {
      return userService;
   }

}
