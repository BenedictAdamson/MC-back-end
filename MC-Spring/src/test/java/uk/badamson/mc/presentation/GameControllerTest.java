package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2024.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.badamson.mc.Authority;
import uk.badamson.mc.FindGameResult;
import uk.badamson.mc.Game;
import uk.badamson.mc.NamedUUID;
import uk.badamson.mc.rest.GameResponse;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GameControllerTest extends ControllerTest {
    private final GameController gameController = new GameController(gameService);

    @Nested
    public class CreateGameForScenario {

        @Test
        public void unknownScenario() {
            final var scenario = UUID.randomUUID();

            final var exception = assertThrows(ResponseStatusException.class, () -> createGameForScenario(scenario));

            assertThat(exception.getStatus(), is(HttpStatus.NOT_FOUND));
        }

        @Test
        public void valid() {
            final var scenario = getValidScenarioId();

            final var response = createGameForScenario(scenario);

            assertThat(response.getStatusCode(), is(HttpStatus.FOUND));
            final var gameId = getGameFromLocationHeader(response);
            Optional<FindGameResult> gameOptional = gameService.getGameAsGameManager(gameId);
            assertThat("Game exists", gameOptional.isPresent());
            final FindGameResult game = gameOptional.get();
            assertThat("Game is for the given scenario", game.scenarioId(), is(scenario));
        }

        private ResponseEntity<Void> createGameForScenario(UUID scenario) {
            final var result = gameController.createGameForScenario(scenario);
            assertThat(result, notNullValue());
            return result;
        }
    }

    @Nested
    public class GetGameIdentifiersOfScenario {

        @Test
        public void unknownScenario() {
            final var scenario = UUID.randomUUID();

            final var exception = assertThrows(ResponseStatusException.class, () -> getGameIdentifiersOfScenario(scenario));

            assertThat(exception.getStatus(), is(HttpStatus.NOT_FOUND));
        }

        @Test
        public void noGames() {
            final var scenario = getValidScenarioId();

            final Set<NamedUUID> result = getGameIdentifiersOfScenario(scenario);

            assertThat(result, empty());
        }

        private Set<NamedUUID> getGameIdentifiersOfScenario(
                final UUID scenario) {
            Set<NamedUUID> result = gameController.getGameIdentifiersOfScenario(scenario);
            assertThat(result, notNullValue());
            assertThat(result, not(hasItem((NamedUUID) null)));
            return result;
        }
    }

    @Nested
    public class GetGame {

        @Test
        public void unknownGame() {
            final var user = createSpringUser(Authority.ALL);// tough test
            final var game = UUID.randomUUID();

            final var exception = assertThrows(ResponseStatusException.class, () -> getGame(user, game));

            assertThat(exception.getStatus(), is(HttpStatus.NOT_FOUND));
        }

        @Test
        public void withManageGamesRole() {
            final var user = createSpringUser(EnumSet.of(Authority.ROLE_MANAGE_GAMES));
            final var playerId = createSpringUser(EnumSet.of(Authority.ROLE_PLAYER)).getId();
            final var gameId = createGame();
            gameService.userJoinsGame(playerId, gameId);

            final GameResponse response = getGame(user, gameId);

            assertThat("Report game player", response.users().values(), hasItem(playerId));
        }

        @Test
        public void withPlayerRole() {
            final var user = createSpringUser(EnumSet.of(Authority.ROLE_PLAYER));
            final var playerId = createSpringUser(EnumSet.of(Authority.ROLE_PLAYER)).getId();
            final var gameId = createGame();
            gameService.userJoinsGame(playerId, gameId);

            final GameResponse response = getGame(user, gameId);

            assertThat("Does not report game players", response.users(), anEmptyMap());
        }

        @Test
        public void withoutAuthority() {
            final var user = createSpringUser(Set.of());
            final var gameId = createGame();

            assertThrows(IllegalArgumentException.class, () -> getGame(user, gameId));
        }

        @Nonnull
        private GameResponse getGame(
                final SpringUser requestingUser,
                final UUID game) {
            GameResponse response = gameController.getGame(requestingUser, game);
            assertThat(response, notNullValue());
            assertThat(response.identifier(), is(game));
            return response;
        }
    }

    @Nested
    public class StartGame {
        @Test
        public void unknownGame() {
            final var user = createSpringUser(Authority.ALL);// tough test
            final var game = UUID.randomUUID();

            final var exception = assertThrows(ResponseStatusException.class, () -> startGame(user, game));

            assertThat(exception.getStatus(), is(HttpStatus.NOT_FOUND));
        }

        @Test
        public void waitingToStart() {
            final var user = createSpringUser(Authority.ALL);
            final var game = createGame();

            ResponseEntity<Void> response = startGame(user, game);

            final var responseGame = getGameFromLocationHeader(response);
            assertThat(responseGame, is(game));
            final var runState = gameService.getGameAsGameManager(game).orElseThrow().game().getRunState();
            assertThat(runState, is(Game.RunState.RUNNING));
        }

        @Test
        public void stopped() {
            final var user = createSpringUser(Authority.ALL);
            final var game = createGame();
            gameService.startGame(game);
            gameService.stopGame(game);

            final var exception = assertThrows(ResponseStatusException.class, () -> startGame(user, game));

            assertThat(exception.getStatus(), is(HttpStatus.CONFLICT));
        }

        private ResponseEntity<Void> startGame(
                final SpringUser requestingUser,
                final UUID game
        ) {
            ResponseEntity<Void> voidResponseEntity = gameController.startGame(requestingUser, game);
            assertThat(voidResponseEntity, notNullValue());
            return voidResponseEntity;
        }
    }

    @Nested
    public class StopGame {
        @Test
        public void unknownGame() {
            final var user = createSpringUser(Authority.ALL);// tough test
            final var game = UUID.randomUUID();

            final var exception = assertThrows(ResponseStatusException.class, () -> stopGame(user, game));

            assertThat(exception.getStatus(), is(HttpStatus.NOT_FOUND));
        }

        @Test
        public void waitingToStart() {
            final var user = createSpringUser(Authority.ALL);
            final var game = createGame();

            ResponseEntity<Void> response = stopGame(user, game);

            final var responseGame = getGameFromLocationHeader(response);
            assertThat(responseGame, is(game));
            final var runState = gameService.getGameAsGameManager(game).orElseThrow().game().getRunState();
            assertThat(runState, is(Game.RunState.STOPPED));
        }


        private ResponseEntity<Void> stopGame(
                final SpringUser requestingUser,
                final UUID game
        ) {
            ResponseEntity<Void> voidResponseEntity = gameController.stopGame(requestingUser, game);
            assertThat(voidResponseEntity, notNullValue());
            return voidResponseEntity;
        }
    }

    @Nested
    public class EndRecruitment {
        @Test
        public void unknownGame() {
            final var game = UUID.randomUUID();

            final var exception = assertThrows(ResponseStatusException.class, () -> endRecruitment(game));

            assertThat(exception.getStatus(), is(HttpStatus.NOT_FOUND));
        }

        @Test
        public void recruiting() {
            final var gameId = createGame();

            ResponseEntity<Void> response = endRecruitment(gameId);

            final var responseGame = getGameFromLocationHeader(response);
            assertThat(responseGame, is(gameId));
            final var game = gameService.getGameAsGameManager(gameId).orElseThrow().game();
            assertThat(game.isRecruiting(), is(false));
        }

        private ResponseEntity<Void> endRecruitment(
                final UUID game
        ) {
            ResponseEntity<Void> voidResponseEntity = gameController.endRecruitment(game);
            assertThat(voidResponseEntity, notNullValue());
            return voidResponseEntity;
        }
    }

    @Nested
    public class GetCurrentGame {
        @Test
        public void noAuthenticatedUser() {
            final var exception = assertThrows(ResponseStatusException.class, () -> getCurrentGame(null));

            assertThat(exception.getStatus(), is(HttpStatus.NOT_FOUND));
        }

        @Test
        public void noCurrentGame() {
            final var user = createSpringUser(Authority.ALL);

            final var exception = assertThrows(ResponseStatusException.class, () -> getCurrentGame(user));

            assertThat(exception.getStatus(), is(HttpStatus.NOT_FOUND));
        }

        @Test
        public void hasCurrentGame() {
            final var user = createSpringUser(Authority.ALL);
            final var game = createGame();
            gameService.userJoinsGame(user.getId(), game);

            final var response = getCurrentGame(user);

            assertThat(response.getStatusCode(), is(HttpStatus.TEMPORARY_REDIRECT));
            final var responseGame = getGameFromLocationHeader(response);
            assertThat(responseGame, is(game));
        }

        private ResponseEntity<Void> getCurrentGame(final SpringUser user) {
            final var response = gameController.getCurrentGame(user);
            assertThat(response, notNullValue());
            return response;
        }
    }

    @Nested
    public class JoinGame {
        @Test
        public void unknownGame() {
            final var user = createSpringUser(Authority.ALL);// tough test
            final var game = UUID.randomUUID();

            final var exception = assertThrows(ResponseStatusException.class, () -> joinGame(user, game));

            assertThat(exception.getStatus(), is(HttpStatus.NOT_FOUND));

        }

        @Test
        public void recruitmentEnded() {
            final var user = createSpringUser(Authority.ALL);
            final var game = createGame();
            gameService.endRecruitment(game);

            final var exception = assertThrows(ResponseStatusException.class, () -> joinGame(user, game));

            assertThat(exception.getStatus(), is(HttpStatus.CONFLICT));
        }

        @Test
        public void alreadyPlaying() {
            final var user = createSpringUser(Authority.ALL);
            final var gameA = createGame();
            final var gameB = createGame();
            gameService.userJoinsGame(user.getId(), gameB);

            final var exception = assertThrows(ResponseStatusException.class, () -> joinGame(user, gameA));

            assertThat(exception.getStatus(), is(HttpStatus.CONFLICT));

        }

        @Test
        public void valid() {
            final var user = createSpringUser(Authority.ALL);
            final var gameId = createGame();

            final var response = joinGame(user, gameId);

            assertThat(response.getStatusCode(), is(HttpStatus.FOUND));
            final var responseGameId = getGameFromLocationHeader(response);
            assertThat(responseGameId, is(gameId));
            final var game = gameService.getGameAsGameManager(gameId).orElseThrow().game();
            assertThat(game.getUsers().values(), hasItem(user.getId()));
        }

        private ResponseEntity<Void> joinGame(
                final SpringUser user,
                final UUID game) {
            final var response = gameController.joinGame(user, game);
            assertThat(response, notNullValue());
            return response;
        }
    }

    @Nested
    public class MayJoinGame {

        @Test
        public void unknownGame() {
            final var user = createSpringUser(Authority.ALL);// tough test
            final var game = UUID.randomUUID();

            final var exception = assertThrows(ResponseStatusException.class, () -> mayJoinGame(user, game));

            assertThat(exception.getStatus(), is(HttpStatus.NOT_FOUND));
        }

        @Test
        public void notPlaying() {
            final var user = createSpringUser(Authority.ALL);
            final var game = createGame();

            final boolean response = mayJoinGame(user, game);

            assertThat(response, is(true));
        }

        @Test
        public void notRecuiting() {
            final var user = createSpringUser(Authority.ALL);
            final var game = createGame();
            gameService.endRecruitment(game);

            final boolean response = mayJoinGame(user, game);

            assertThat(response, is(false));
        }

        private boolean mayJoinGame(final SpringUser user,
                                    final UUID game) {
            return gameController.mayJoinGame(user, game);
        }
    }

}
