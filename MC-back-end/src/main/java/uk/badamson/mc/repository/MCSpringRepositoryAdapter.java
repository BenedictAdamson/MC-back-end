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
import uk.badamson.mc.spring.GrantedMCAuthority;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MCSpringRepositoryAdapter extends MCRepository {

    private final CurrentUserGameSpringRepository currentUserGameRepository;
    private final GamePlayersSpringRepository gamePlayersRepository;
    private final GameSpringRepository gameRepository;
    private final UserSpringRepository userRepository;

    @Nonnull
    private static GameIdentifierDTO convertToDTO(@Nonnull Game.Identifier id) {
        return new GameIdentifierDTO(id.getScenario(), id.getCreated());
    }

    @Nonnull
    private static RunStateDTO convertToDTO(@Nonnull Game.RunState runState) {
        return RunStateDTO.valueOf(runState.toString());
    }

    @Nonnull
    private static GameDTO convertToDTO(@Nonnull Game.Identifier id, @Nonnull Game game) {
        return new GameDTO(convertToDTO(id), convertToDTO(game.getRunState()));
    }

    @Nonnull
    private static GamePlayersDTO convertToDTO(@Nonnull Game.Identifier id, @Nonnull GamePlayers gamePlayers) {
        return new GamePlayersDTO(
                convertToDTO(id),
                gamePlayers.isRecruiting(),
                Map.copyOf(gamePlayers.getUsers())
        );
    }

    @Nonnull
    private static GrantedMCAuthority convertToDTO(@Nonnull Authority authority) {
        return GrantedMCAuthority.valueOf(authority.toString());
    }

    @Nonnull
    private static Set<GrantedMCAuthority> convertToDTO(@Nonnull Set<Authority> authorities) {
        return authorities.stream()
                .map(MCSpringRepositoryAdapter::convertToDTO)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    private static UserDTO convertToDTO(@Nonnull UUID userId, @Nonnull User user) {
        return new UserDTO(
                userId,
                user.getUsername(), user.getPassword(),
                convertToDTO(user.getAuthorities()),
                user.isAccountNonExpired(),
                user.isAccountNonLocked(),
                user.isCredentialsNonExpired(),
                user.isEnabled()
                );
    }

    @Nonnull
    private static UserGameAssociationDTO convertToDTO(@Nonnull UUID userId,@Nonnull UserGameAssociation association) {
        return new UserGameAssociationDTO(userId, convertToDTO(association.getGame()));
    }

    @Nonnull
    private static Game.Identifier convertFromDTO(@Nonnull GameIdentifierDTO dto) {
        return new Game.Identifier(dto.scenario(), dto.created());
    }

    @Nonnull
    private static Game.RunState convertFromDTO(@Nonnull RunStateDTO dto) {
        return Game.RunState.valueOf(dto.toString());
    }

    @Nonnull
    private static Game convertFromDTO(@Nonnull GameDTO dto) {
        return new Game(convertFromDTO(dto.identifier()), convertFromDTO(dto.runState()));
    }

    @Nonnull
    private static GamePlayers convertFromDTO(@Nonnull GamePlayersDTO dto) {
        return new GamePlayers(convertFromDTO(dto.game()), dto.recruiting(), Map.copyOf(dto.users()));
    }

    @Nonnull
    private static Authority convertFromDTO(@Nonnull GrantedMCAuthority dto) {
        return Authority.valueOf(dto.toString());
    }

    @Nonnull
    private static Set<Authority> convertFromDTO(@Nonnull Set<GrantedMCAuthority> dto) {
        return dto.stream().map(MCSpringRepositoryAdapter::convertFromDTO).collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    private static User convertFromDTO(@Nonnull UserDTO dto) {
        return new User(dto.getId(),
        dto.getUsername(),
        dto.getPassword(),
        convertFromDTO(dto.getAuthorities()),
        dto.isAccountNonExpired(),
        dto.isAccountNonLocked(),
        dto.isCredentialsNonExpired(),
        dto.isEnabled());
    }

    @Nonnull
    private static UserGameAssociation convertFromDTO(@Nonnull UserGameAssociationDTO dto) {
        return new UserGameAssociation(dto.user(), convertFromDTO(dto.game()));
    }

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

    public final class AdapterContext extends Context {

        @Override
        public void saveGame(@Nonnull Game.Identifier id, @Nonnull Game game) {
            gameRepository.save(convertToDTO(id, game));
        }

        @Nonnull
        @Override
        public Optional<Game> findGame(@Nonnull Game.Identifier id) {
            return gameRepository.findById(convertToDTO(id))
                    .map(MCSpringRepositoryAdapter::convertFromDTO);
        }

        @Nonnull
        @Override
        public Stream<Game> findAllGames() {
            return StreamSupport.stream(gameRepository.findAll().spliterator(), false)
                    .map(MCSpringRepositoryAdapter::convertFromDTO);
        }

        @Override
        public void saveGamePlayers(@Nonnull Game.Identifier id, @Nonnull GamePlayers gamePlayers) {
            gamePlayersRepository.save(convertToDTO(id, gamePlayers));
        }

        @Nonnull
        @Override
        public Optional<GamePlayers> findGamePlayers(@Nonnull Game.Identifier id) {
            return gamePlayersRepository.findById(convertToDTO(id)).map(MCSpringRepositoryAdapter::convertFromDTO);
        }

        @Nonnull
        @Override
        public Optional<UserGameAssociation> findCurrentUserGame(@Nonnull UUID userId) {
            return currentUserGameRepository.findById(userId).map(MCSpringRepositoryAdapter::convertFromDTO);
        }

        @Override
        public void saveCurrentUserGame(@Nonnull UUID userId, @Nonnull UserGameAssociation association) {
            currentUserGameRepository.save(convertToDTO(userId, association));
        }

        @Nonnull
        @Override
        public Optional<User> findUserByUsername(@Nonnull String username) {
            return userRepository.findByUsername(username);
        }

        @Nonnull
        @Override
        public Optional<User> findUser(@Nonnull UUID id) {
            return userRepository.findById(id).map(MCSpringRepositoryAdapter::convertFromDTO);
        }

        @Nonnull
        @Override
        public Stream<User> findAllUsers() {
            return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                    .map(MCSpringRepositoryAdapter::convertFromDTO);
        }

        @Override
        public void saveUser(@Nonnull UUID id, @Nonnull User user) {
            userRepository.save(convertToDTO(id, user));
        }

        @Override
        public void close() {
            // Do nothing
        }
    }

    @Nonnull
    @Override
    public Context openContext() {
        return new AdapterContext();
    }
}
