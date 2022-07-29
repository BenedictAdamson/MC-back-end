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

import uk.badamson.mc.*;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import java.util.*;

public class MCSpringRepositoryAdapter extends MCRepository {

    private final CurrentUserGameSpringRepository currentUserGameRepository;
    private final GameSpringRepository gameRepository;
    private final UserSpringRepository userRepository;

    public MCSpringRepositoryAdapter(
            @Nonnull CurrentUserGameSpringRepository currentUserGameRepository,
            @Nonnull GameSpringRepository gameRepository,
            @Nonnull UserSpringRepository userRepository) {
        this.currentUserGameRepository = Objects.requireNonNull(currentUserGameRepository);
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
        protected void addGameUncached(@Nonnull GameIdentifier id, @Nonnull Game game) {
            gameRepository.save(GameDTO.convertToDTO(id, game));
        }

        @Override
        protected void updateGameUncached(@Nonnull GameIdentifier id, @Nonnull Game game) {
            gameRepository.save(GameDTO.convertToDTO(id, game));
        }

        @Nonnull
        @Override
        protected Optional<FindGameResult> findGameUncached(@Nonnull GameIdentifier id) {
            final var gameDtoOptional = gameRepository.findById(GameIdentifierDTO.convertToDTO(id));
            if (gameDtoOptional.isEmpty()) {
                return Optional.empty();
            }
            final var gameDto = gameDtoOptional.get();
            final var scenarioId = gameDto.identifier().scenario();
            return gameDtoOptional.map(dto -> new FindGameResult(GameDTO.convertFromDTO(dto), scenarioId));
        }

        @Nonnull
        @Override
        protected Iterable<Map.Entry<GameIdentifier, FindGameResult>> findAllGamesUncached() {
            List<Map.Entry<GameIdentifier, FindGameResult>> result = new ArrayList<>();
            for (var gameDTO: gameRepository.findAll()) {
                result.add(new AbstractMap.SimpleImmutableEntry<>(
                        GameIdentifierDTO.convertFromDTO(gameDTO.identifier()),
                        new FindGameResult(GameDTO.convertFromDTO(gameDTO), gameDTO.identifier().scenario())
                ));
            }
            return result;
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
        protected Iterable<Map.Entry<UUID,User>> findAllUsersUncached() {
            final List<Map.Entry<UUID,User>> result = new ArrayList<>();
            for (var u: userRepository.findAll()) {
                result.add(new AbstractMap.SimpleImmutableEntry<>(u.getId(), SpringUser.convertFromSpring(u)));
            }
            return result;
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
