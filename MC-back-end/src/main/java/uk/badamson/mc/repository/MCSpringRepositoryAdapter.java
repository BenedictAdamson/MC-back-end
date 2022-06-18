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

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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

    @Override
    public void saveGame(@Nonnull Game.Identifier id, @Nonnull Game game) {
        gameRepository.save(game);
    }

    @Nonnull
    @Override
    public Optional<Game> findGame(@Nonnull Game.Identifier id) {
        return gameRepository.findById(id);
    }

    @Nonnull
    @Override
    public Stream<Game> findAllGames() {
        return StreamSupport.stream(gameRepository.findAll().spliterator(), false);
    }

    @Override
    public void saveGamePlayers(@Nonnull Game.Identifier id, @Nonnull GamePlayers gamePlayers) {
        gamePlayersRepository.save(gamePlayers);
    }

    @Nonnull
    @Override
    public Optional<GamePlayers> findGamePlayers(@Nonnull Game.Identifier id) {
        return gamePlayersRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<UserGameAssociation> findCurrentUserGame(@Nonnull UUID userId) {
        return currentUserGameRepository.findById(userId);
    }

    @Override
    public void saveCurrentUserGame(@Nonnull UUID userId, @Nonnull UserGameAssociation association) {
        currentUserGameRepository.save(association);
    }

    @Nonnull
    @Override
    public Optional<User> findUserByUsername(@Nonnull String username) {
        return userRepository.findByUsername(username);
    }

    @Nonnull
    @Override
    public Optional<User> findUser(@Nonnull UUID id) {
        return userRepository.findById(id);
    }

    @Nonnull
    @Override
    public Stream<User> findAllUsers() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false);
    }

    @Override
    public void saveUser(@Nonnull UUID id, @Nonnull User user) {
        userRepository.save(user);
    }
}
