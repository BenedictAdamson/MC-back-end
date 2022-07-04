package uk.badamson.mc.repository;
/*
 * © Copyright Benedict Adamson 2022.
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

import uk.badamson.mc.Game;
import uk.badamson.mc.GamePlayers;
import uk.badamson.mc.User;
import uk.badamson.mc.UserGameAssociation;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MCSpringRepositoryAdapter extends MCRepository {

    private final CurrentUserGameSpringRepository currentUserGameRepository;
    private final GamePlayersSpringRepository gamePlayersRepository;
    private final GameSpringRepository gameRepository;
    private final UserSpringRepository userRepository;

    public MCSpringRepositoryAdapter(
            @Nonnull CurrentUserGameSpringRepository currentUserGameRepository,
            @Nonnull GamePlayersSpringRepository gamePlayersRepository,
            @Nonnull GameSpringRepository gameRepository,
            @Nonnull UserSpringRepository userRepository) {
        this.currentUserGameRepository = Objects.requireNonNull(currentUserGameRepository);
        this.gamePlayersRepository = Objects.requireNonNull(gamePlayersRepository);
        this.gameRepository = Objects.requireNonNull(gameRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    @Nonnull
    @Override
    public Context openContext() {
        return new AdapterContext();
    }

    public final class AdapterContext extends Context {

        @Override
        protected void addGameUncached(@Nonnull Game.Identifier id, @Nonnull Game game) {
            gameRepository.save(GameDTO.convertToDTO(id, game));
        }

        @Override
        protected void updateGameUncached(@Nonnull Game.Identifier id, @Nonnull Game game) {
            gameRepository.save(GameDTO.convertToDTO(id, game));
        }

        @Nonnull
        @Override
        protected Optional<Game> findGameUncached(@Nonnull Game.Identifier id) {
            return gameRepository.findById(GameIdentifierDTO.convertToDTO(id))
                    .map(GameDTO::convertFromDTO);
        }

        @Nonnull
        @Override
        protected Stream<Game.Identifier> findAllGameIdentifiersUncached() {
            return StreamSupport.stream(gameRepository.findAll().spliterator(), false)
                    .map(game -> GameIdentifierDTO.convertFromDTO(game.identifier()));
        }

        @Override
        protected void addGamePlayersUncached(@Nonnull Game.Identifier id, @Nonnull GamePlayers gamePlayers) {
            gamePlayersRepository.save(GamePlayersDTO.convertToDTO(id, gamePlayers));
        }

        @Override
        protected void updateGamePlayersUncached(@Nonnull Game.Identifier id, @Nonnull GamePlayers gamePlayers) {
            gamePlayersRepository.save(GamePlayersDTO.convertToDTO(id, gamePlayers));
        }

        @Nonnull
        @Override
        protected Optional<GamePlayers> findGamePlayersUncached(@Nonnull Game.Identifier id) {
            return gamePlayersRepository.findById(GameIdentifierDTO.convertToDTO(id)).map(GamePlayersDTO::convertFromDTO);
        }

        @Nonnull
        @Override
        protected Optional<UserGameAssociation> findCurrentUserGameUncached(@Nonnull UUID userId) {
            return currentUserGameRepository.findById(userId).map(UserGameAssociationDTO::convertFromDTO);
        }

        @Override
        protected void addCurrentUserGameUncached(@Nonnull UUID userId, @Nonnull UserGameAssociation association) {
            currentUserGameRepository.save(UserGameAssociationDTO.convertToDTO(userId, association));
        }

        @Override
        protected void updateCurrentUserGameUncached(@Nonnull UUID userId, @Nonnull UserGameAssociation association) {
            currentUserGameRepository.save(UserGameAssociationDTO.convertToDTO(userId, association));
        }

        @Nonnull
        @Override
        protected Optional<UUID> findUserIdForUsernameUncached(@Nonnull String username) {
            return userRepository.findByUsername(username).map(SpringUser::getId);
        }

        @Nonnull
        @Override
        protected Optional<User> findUserUncached(@Nonnull UUID id) {
            return userRepository.findById(id).map(SpringUser::convertFromSpring);
        }

        @Nonnull
        @Override
        protected Stream<Map.Entry<UUID,User>> findAllUsersUncached() {
            return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                    .map(u -> new AbstractMap.SimpleImmutableEntry<>(u.getId(), SpringUser.convertFromSpring(u)));
        }

        @Override
        protected void addUserUncached(@Nonnull UUID id, @Nonnull User user) {
            userRepository.save(SpringUser.convertToSpring(user));
        }

        @Override
        protected void updateUserUncached(@Nonnull UUID id, @Nonnull User user) {
            userRepository.save(SpringUser.convertToSpring(user));
        }
    }
}
