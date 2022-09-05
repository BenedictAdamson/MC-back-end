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
import uk.badamson.mc.FindGameResult;
import uk.badamson.mc.Game;
import uk.badamson.mc.IdentifiedValue;
import uk.badamson.mc.NamedUUID;
import uk.badamson.mc.repository.MCSpringRepositoryAdapter;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

    @Nonnull
    @Transactional
    public IdentifiedValue<UUID, Game> create(@Nonnull final UUID scenario) {
        return delegate.create(scenario);
    }

    @Nonnull
    @Transactional
    public Set<NamedUUID> getGameIdentifiersOfScenario(@Nonnull final UUID scenario)
            throws NoSuchElementException {
        return delegate.getGameIdentifiersOfScenario(scenario);
    }

    @Transactional
    @Nonnull
    public Iterable<UUID> getGameIdentifiers() {
        return delegate.getGameIdentifiers();
    }

    @Transactional
    @Nonnull
    public Game startGame(@Nonnull final UUID id)
            throws NoSuchElementException, IllegalGameStateException {
        return delegate.startGame(id);
    }

    @Transactional
    public void stopGame(@Nonnull final UUID id)
            throws NoSuchElementException {
        delegate.stopGame(id);
    }

    @Transactional
    public void endRecruitment(@Nonnull final UUID id)
            throws NoSuchElementException {
        delegate.endRecruitment(id);
    }

    @Transactional
    @Nonnull
    public Optional<UUID> getCurrentGameOfUser(
            @Nonnull final UUID userId) {
        return delegate.getCurrentGameOfUser(userId);
    }

    @Transactional
    @Nonnull
    public Optional<FindGameResult> getGameAsGameManager(
            @Nonnull final UUID id) {
        return delegate.getGameAsGameManager(id);
    }

    @Transactional
    @Nonnull
    public Optional<FindGameResult> getGameAsNonGameManager(
            @Nonnull final UUID id, @Nonnull final UUID user) {
        return delegate.getGameAsNonGameManager(id, user);
    }

    @Transactional
    public boolean mayUserJoinGame(@Nonnull final UUID user, @Nonnull final UUID game) {
        return delegate.mayUserJoinGame(user, game);
    }

    @Transactional
    public void userJoinsGame(@Nonnull final UUID userId,
                              @Nonnull final UUID gameId)
            throws NoSuchElementException, UserAlreadyPlayingException,
            IllegalGameStateException, SecurityException {
        delegate.userJoinsGame(userId, gameId);
    }

}
