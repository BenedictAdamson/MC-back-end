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

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.GamePlayers;
import uk.badamson.mc.repository.GamePlayersRepository;

/**
 * <p>
 * Implementation of the part of the service layer pertaining to players of
 * games of Mission Command.
 * </p>
 */
@Service
public class GamePlayersServiceImpl implements GamePlayersService {

   private final GamePlayersRepository repository;

   private final GameService gameService;

   /**
    * <p>
    * Construct a service with give associations.
    * </p>
    * <ul>
    * <li>The created service has the given {@code repository} as its
    * {@linkplain #getRepository() repository}.</li>
    * <li>The created service has the given {@code gameService} as its
    * {@linkplain #getGameService() scenario service}.</li>
    * </ul>
    *
    * @param repository
    *           The repository that this service uses for persistent storage.
    * @param gameService
    *           The part of the service layer that this service uses for
    *           information about scenarios.
    * @throws NullPointerException
    *            <ul>
    *            <li>If {@code repository} is null.</li>
    *            <li>If {@code gameService} is null.</li>
    *            </ul>
    */
   @Autowired
   public GamePlayersServiceImpl(
            @Nonnull final GamePlayersRepository repository,
            @Nonnull final GameService gameService) {
      this.repository = Objects.requireNonNull(repository, "repository");
      this.gameService = Objects.requireNonNull(gameService, "gameService");
   }

   @Override
   @Nonnull
   public Optional<GamePlayers> endRecruitment(@Nonnull final Identifier id) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   @Nonnull
   public Optional<GamePlayers> getGamePlayers(
            @Nonnull final Game.Identifier id) {
      return repository.findById(id);
   }

   @Override
   @Nonnull
   public final GameService getGameService() {
      return gameService;
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
   public final GamePlayersRepository getRepository() {
      return repository;
   }

}
