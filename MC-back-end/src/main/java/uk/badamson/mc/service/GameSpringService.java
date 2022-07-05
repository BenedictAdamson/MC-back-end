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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.GamePlayers;
import uk.badamson.mc.repository.MCSpringRepositoryAdapter;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class GameSpringService {

    private final GameService delegate;

    @Autowired
    public GameSpringService(@Nonnull final Clock clock,
                             @Nonnull final ScenarioSpringService scenarioService,
                             @Nonnull final UserSpringService userService,
                             @Nonnull MCSpringRepositoryAdapter repository) {
        this.delegate = new GameService(clock, scenarioService.getDelegate(), userService.getDelegate(), repository);
    }

    final GameService getDelegate() {
        return delegate;
    }

    @Nonnull
    @Transactional
    public Game create(@Nonnull final UUID scenario) {
        return delegate.create(scenario);
    }

    @Nonnull
    @Transactional
    public Stream<Instant> getCreationTimesOfGamesOfScenario(@Nonnull final UUID scenario)
            throws NoSuchElementException {
        return delegate.getCreationTimesOfGamesOfScenario(scenario).stream();
    }

    @Transactional
    @Nonnull
    public Optional<Game> getGame(@Nonnull final Identifier id) {
        return delegate.getGame(id);
    }

    @Transactional
    @Nonnull
    public Iterable<Identifier> getGameIdentifiers() {
        return delegate.getGameIdentifiers();
    }

    @Transactional
    @Nonnull
    public Game startGame(@Nonnull final Identifier id)
            throws NoSuchElementException, IllegalGameStateException {
        return delegate.startGame(id);
    }

    @Transactional
    public void stopGame(@Nonnull final Identifier id)
            throws NoSuchElementException {
        delegate.stopGame(id);
    }

    @Transactional
    public void endRecruitment(@Nonnull final Identifier id)
            throws NoSuchElementException {
        delegate.endRecruitment(id);
    }

    @Transactional
    @Nonnull
    public Optional<Game.Identifier> getCurrentGameOfUser(
            @Nonnull final UUID userId) {
        return delegate.getCurrentGameOfUser(userId);
    }

    @Transactional
    @Nonnull
    public Optional<GamePlayers> getGamePlayersAsGameManager(
            @Nonnull final Game.Identifier id) {
        return delegate.getGamePlayersAsGameManager(id);
    }

    @Transactional
    @Nonnull
    public Optional<GamePlayers> getGamePlayersAsNonGameManager(
            @Nonnull final Game.Identifier id, @Nonnull final UUID user) {
        return delegate.getGamePlayersAsNonGameManager(id, user);
    }

    @Transactional
    public boolean mayUserJoinGame(@Nonnull final UUID user, @Nonnull final Identifier game) {
        return delegate.mayUserJoinGame(user, game);
    }

    @Transactional
    public void userJoinsGame(@Nonnull final UUID userId,
                              @Nonnull final Game.Identifier gameId)
            throws NoSuchElementException, UserAlreadyPlayingException,
            IllegalGameStateException, SecurityException {
        delegate.userJoinsGame(userId, gameId);
    }

}
