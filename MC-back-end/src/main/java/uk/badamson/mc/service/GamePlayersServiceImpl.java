package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2020-22.
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

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.badamson.mc.*;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.repository.CurrentUserGameSpringRepository;
import uk.badamson.mc.repository.GamePlayersSpringRepository;

/**
 * <p>
 * Implementation of the part of the service layer pertaining to players of
 * games of Mission Command.
 * </p>
 */
@Service
public class GamePlayersServiceImpl implements GamePlayersService {

   @Immutable
   private static final class UserJoinsGameState {
      final GamePlayers gamePlayers;
      final UUID character;
      final boolean alreadyJoined;
      final boolean endRecruitment;

      UserJoinsGameState(final GamePlayers gamePlayers,
               final UUID firstUnplayedCharacter, final boolean alreadyJoined,
               final boolean endRecruitment) {
         this.gamePlayers = gamePlayers;
         this.character = firstUnplayedCharacter;
         this.alreadyJoined = alreadyJoined;
         this.endRecruitment = endRecruitment;
      }

   }

   private static final Map<UUID, UUID> NO_USERS = Map.of();

   private static GamePlayers createDefault(final Game.Identifier id) {
      return new GamePlayers(id, true, NO_USERS);
   }

   private final GamePlayersSpringRepository gamePlayersRepository;

   private final CurrentUserGameSpringRepository currentUserGameRepository;

   private final GameService gameService;

   private final UserService userService;

   /**
    * <p>
    * Construct a service with given associations.
    * </p>
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
            @Nonnull final GamePlayersSpringRepository gamePlayersRepository,
            @Nonnull final CurrentUserGameSpringRepository currentUserGameRepository,
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
      final var players = get(id);
      if (players.isEmpty()) {
         throw new NoSuchElementException();
      }
      players.get().endRecruitment();
      return gamePlayersRepository.save(players.get());
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

   private Optional<Game.Identifier> getCurrent(final UUID user) {
      Objects.requireNonNull(user, "user");
      final var association = currentUserGameRepository.findById(user);
      if (association.isEmpty()) {
         return Optional.empty();
      } else {
         return Optional.of(association.get().getGame());
      }
   }

   @Override
   @Nonnull
   public Optional<Game.Identifier> getCurrentGameOfUser(
            @Nonnull final UUID userId) {
      final var user = getUser(userId);
      if (user.isPresent()) {
         return getCurrent(userId);
      } else {
         return Optional.empty();
      }
   }

   @Nonnull
   public final CurrentUserGameSpringRepository getCurrentUserGameRepository() {
      return currentUserGameRepository;
   }

   @Override
   @Nonnull
   public Optional<GamePlayers> getGamePlayersAsGameManager(
            @Nonnull final Game.Identifier id) {
      return get(id);
   }

   @Override
   @Nonnull
   public Optional<GamePlayers> getGamePlayersAsNonGameManager(
            @Nonnull final Game.Identifier id, @Nonnull final UUID user) {
      Objects.requireNonNull(user, "user");

      final var fullInformation = get(id);
      if (fullInformation.isEmpty()) {
         return fullInformation;
      } else {
         final var allUsers = fullInformation.get().getUsers();
         final Map<UUID, UUID> filteredUsers = allUsers.entrySet().stream()
                  .filter(entry -> user.equals(entry.getValue()))
                  .collect(toUnmodifiableMap(Map.Entry::getKey,
                          Map.Entry::getValue));
         if (allUsers.size() == filteredUsers.size()) {
            return fullInformation;
         } else {
            return Optional.of(new GamePlayers(id, false, filteredUsers));
         }
      }
   }

   @Nonnull
   public final GamePlayersSpringRepository getGamePlayersRepository() {
      return gamePlayersRepository;
   }

   @Override
   @Nonnull
   public final GameService getGameService() {
      return gameService;
   }

   private Optional<User> getUser(final UUID userId) {
      return getUserService().getUser(userId);
   }

   private UserJoinsGameState getUserJoinsGameState(final UUID userId,
            final Game.Identifier gameId)
            throws NoSuchElementException, UserAlreadyPlayingException,
            IllegalGameStateException, SecurityException {
      final var userOptional = getUser(userId);
      if (userOptional.isEmpty()) {
         throw new NoSuchElementException("user");
      }
      final var user = userOptional.get();
      final var gamePlayersOptional = get(gameId);
      if (gamePlayersOptional.isEmpty()) {
         throw new NoSuchElementException("game");
      }
      final var gamePlayers = gamePlayersOptional.get();
      final var current = getCurrent(userId);

      if (!user.getAuthorities().contains(Authority.ROLE_PLAYER)) {
         throw new SecurityException("User does not have the player role");
      }

      final boolean alreadyJoined;
      final UUID character;
      final boolean endRecruitment;
      if (current.isPresent() && !gameId.equals(current.get())) {
         throw new UserAlreadyPlayingException();
      } else if (current.isPresent()) {// && gameId.equals(current.get())
         alreadyJoined = true;
         final var characterEntryOptional = gamePlayers.getUsers().entrySet().stream()
                 .filter(entry -> userId.equals(entry.getValue())).findAny();
         if (characterEntryOptional.isEmpty()) {
            throw new NoSuchElementException("character");
         }
         character = characterEntryOptional.get().getKey();
         endRecruitment = false;
      } else {
         if (!gamePlayers.isRecruiting()) {
            throw new IllegalGameStateException("Game is not recruiting");
         }
         alreadyJoined = false;
         final var scenarioId = gamePlayers.getGame().getScenario();
         final var scenarioOptional = gameService.getScenarioService()
                 .getScenario(scenarioId);
         if (scenarioOptional.isEmpty()) {
            throw new NoSuchElementException("scenario");
         }
         final var scenario = scenarioOptional.get();
         final var characters = scenario.getCharacters();
         final var playedCharacters = gamePlayers.getUsers().keySet();
         final var characterOptional = characters.stream().sequential()
                 .map(NamedUUID::getId)
                 .filter(c -> !playedCharacters.contains(c)).findFirst();
         if (characterOptional.isEmpty()) {
            throw new NoSuchElementException("character");
         }
         character = characterOptional.get();
         endRecruitment = characters.size() - 1 <= playedCharacters.size();
      }

      return new UserJoinsGameState(gamePlayers, character, alreadyJoined,
               endRecruitment);
   }

   @SuppressFBWarnings(value="EI_EXPOSE_REP", justification="reference semantics")
   @Override
   @Nonnull
   public UserService getUserService() {
      return userService;
   }

   @Override
   @Transactional
   public boolean mayUserJoinGame(@Nonnull final UUID user, @Nonnull final Identifier game) {
      try {
         getUserJoinsGameState(user, game);
      } catch (UserAlreadyPlayingException | IllegalGameStateException
               | SecurityException | NoSuchElementException e) {
         return false;
      }
      return true;
   }

   @Override
   @Transactional
   public void userJoinsGame(@Nonnull final UUID userId,
            @Nonnull final Game.Identifier gameId)
            throws NoSuchElementException, UserAlreadyPlayingException,
            IllegalGameStateException, SecurityException {
      // read and check:
      final var state = getUserJoinsGameState(userId, gameId);
      if (state.alreadyJoined) {
         // optimisation
         return;
      }

      // modify:
      final var association = new UserGameAssociation(userId, gameId);
      state.gamePlayers.addUser(state.character, userId);
      if (state.endRecruitment) {
         state.gamePlayers.endRecruitment();
      }

      // write:
      currentUserGameRepository.save(association);
      gamePlayersRepository.save(state.gamePlayers);
   }

}
