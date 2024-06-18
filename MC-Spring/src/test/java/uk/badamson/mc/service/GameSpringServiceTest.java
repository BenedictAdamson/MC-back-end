package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2019-24.
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.badamson.mc.*;

import javax.annotation.Nonnull;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Tag("Spring")
public class GameSpringServiceTest {

    private static final String PASSWORD = "password";

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(Fixtures.MONGO_DB_IMAGE);
    private static int nUsers;
    @Autowired
    private GameSpringService gameService;
    @Autowired
    private ScenarioSpringService scenarioService;
    @Autowired
    private UserSpringService userService;

    private static BasicUserDetails createPlayerUserDetails(@Nonnull String userName) {
        return new BasicUserDetails(
                userName,
                PASSWORD,
                EnumSet.of(Authority.ROLE_PLAYER),
                false, false, false, false
        );
    }

    private static String createUserName() {
        return "User " + (++nUsers);
    }

    @Nonnull
    private IdentifiedValue<UUID, Game> create(
            @Nonnull final UUID scenario)
            throws NoSuchElementException {
        final IdentifiedValue<UUID, Game> result = gameService.create(scenario);

        assertThat(result, notNullValue());
        assertThat(result.getValue().getScenario(), notNullValue());

        return result;
    }

    @Nonnull
    private Set<NamedUUID> getGameIdentifiersOfScenario(
            final UUID scenario)
            throws NoSuchElementException {
        final Set<NamedUUID> identifiers = gameService.getGameIdentifiersOfScenario(scenario);

        assertThat(identifiers, notNullValue());

        return identifiers;
    }

    @Nonnull
    private Iterable<UUID> getGameIdentifiers() {
        final var games = gameService.getGameIdentifiers();

        assertThat(games, notNullValue());
        return games;
    }

    private void endRecruitment(
            final UUID id)
            throws NoSuchElementException {
        gameService.endRecruitment(id);

        final Optional<FindGameResult> gameOptional = getGameAsGameManager(id);
        assertThat(gameOptional.isPresent(), is(true));
        assertFalse(gameOptional.get().game().isRecruiting(),
                "Subsequent retrieval of game players using an identifier equivalent to the given ID returns "
                        + "a value that is also not recruiting.");
    }

    @Nonnull
    private Optional<UUID> getCurrentGameOfUser(
            final UUID user) {
        final var result = gameService.getCurrentGameOfUser(user);
        assertThat(result, notNullValue());
        return result;
    }

    @Nonnull
    private Optional<FindGameResult> getGameAsGameManager(
            final UUID id) {
        final var result = gameService.getGameAsGameManager(id);
        assertThat(result, notNullValue());
        return result;
    }

    @Nonnull
    private Optional<FindGameResult> getGameAsNonGameManager(
            final UUID id,
            final UUID user) {
        final var result = gameService.getGameAsNonGameManager(id, user);

        assertThat(result, notNullValue());
        if (result.isPresent()) {
            final var game = result.get().game();
            assertAll(
                    () -> assertThat(
                            "The collection of players is either empty or contains the requesting user.",
                            Set.copyOf(game.getUsers().values()),
                            either(empty()).or(is(Set.of(user)))),
                    () -> assertThat("game.scenario", game.getScenario(), notNullValue())
            );
        }
        return result;
    }

    private boolean mayUserJoinGame(
            final UUID user,
            final UUID game) {
        return gameService.mayUserJoinGame(user, game);
    }

    private void userJoinsGame(
            final UUID user,
            final UUID gameIdentifier)
            throws NoSuchElementException, UserAlreadyPlayingException,
            IllegalGameStateException, SecurityException {
        final var gameOptional0 = getGameAsGameManager(gameIdentifier).map(FindGameResult::game);
        final Map<UUID, UUID> users0 = gameOptional0.map(Game::getUsers).orElseGet(Map::of);
        final var alreadyPlaying = users0.containsValue(user);

        gameService.userJoinsGame(user, gameIdentifier);

        final var currentGame = getCurrentGameOfUser(user);
        final Optional<FindGameResult> gameOptional = getGameAsGameManager(gameIdentifier);
        assertThat("game", gameOptional.isPresent());
        final var game = gameOptional.map(FindGameResult::game).get();
        final var users = game.getUsers();
        final var characterPlayed = users.entrySet().stream()
                .filter(entry -> user.equals(entry.getValue()))
                .map(Map.Entry::getKey).findAny();
        assertThat("The players of the game includes the user.",
                characterPlayed.isPresent());// guard
        assertAll(
                () -> assertThat("The current game of the user is the given game.", currentGame,
                        is(Optional.of(gameIdentifier))),
                () -> assertThat(
                        "The user is already a player, or the character played by the player did not previously have a player.",
                        alreadyPlaying || !users0.containsKey(characterPlayed.get()))
        );
    }

    private UUID getAScenarioId() {
        final Optional<UUID> scenarioOptional = scenarioService.getScenarioIdentifiers()
                .findAny();
        assert scenarioOptional.isPresent();
        return scenarioOptional.get();
    }

    @Nested
    public class Create {

        @Test
        public void unknownScenario() {
            final var scenario = UUID.randomUUID();

            assertThrows(NoSuchElementException.class,
                    () -> create(scenario));
        }

        @Test
        public void knownScenario() {
            final var scenarioId = getAScenarioId();
            final var scenarioOptional = scenarioService.getScenario(scenarioId);
            assert scenarioOptional.isPresent();
            final var scenario = scenarioOptional.get();

            final var identifiedValue = create(scenarioId);

            final var identifier = identifiedValue.getIdentifier();
            final var retrievedGameOptional = getGameAsGameManager(identifier);
            assertThat(retrievedGameOptional, notNullValue());
            assertThat(retrievedGameOptional.isPresent(), is(true));
            final var retrievedGame = retrievedGameOptional.get().game();
            assertThat(retrievedGame.getScenario(), is(scenario));
        }
    }

    @Nested
    public class GetCreationTimesOfGamesOfScenario {

        @Test
        public void knownScenarioWithGame() {
            final var scenario = getAScenarioId();
            final var existingGameId = create(scenario).getIdentifier();

            final Set<NamedUUID> result = getGameIdentifiersOfScenario(scenario);

            final Collection<UUID> ids = result.stream().map(NamedUUID::getId).toList();
            assertThat(ids, hasItem(existingGameId));
        }

        @Test
        public void unknownScenario() {
            final var scenario = UUID.randomUUID();

            assertThrows(NoSuchElementException.class,
                    () -> getGameIdentifiersOfScenario(scenario));
        }
    }

    @Nested
    public class GetGameIdentifiers {

        @Test
        public void hasAGame() {
            final UUID existingGameId = create(getAScenarioId()).getIdentifier();

            final Iterable<UUID> result = getGameIdentifiers();

            assertThat(result, hasItem(existingGameId));
        }
    }

    @Nested
    public class EndRecruitment {

        @Test
        public void gameExists() {
            final var scenario = getAScenarioId();
            final var id = create(scenario).getIdentifier();

            endRecruitment(id);
        }

        @Test
        public void unknownGame() {
            final UUID id = UUID.randomUUID();

            assertThrows(NoSuchElementException.class,
                    () -> endRecruitment(id));
        }

    }

    @Nested
    public class GetCurrentGameOfUser {

        @Test
        public void unknownUser() {
            final var user = UUID.randomUUID();

            final var result = getCurrentGameOfUser(user);

            assertTrue(result.isEmpty(), "empty");
        }
    }

    @Nested
    public class GetGameAsGameManager {

        @Test
        public void present() {
            final var scenarioId = getAScenarioId();
            final var scenarioOptional = scenarioService.getScenario(scenarioId);
            assert scenarioOptional.isPresent();
            final var scenario = scenarioOptional.get();
            final var gameId = create(scenarioId).getIdentifier();

            final var result = getGameAsGameManager(gameId);

            assertThat("present", result.isPresent());
            final var game = result.get().game();
            assertAll(
                    () -> assertThat("recruiting", game.isRecruiting(), is(true)),
                    () -> assertThat("users", game.getUsers(), anEmptyMap()),
                    () -> assertThat("scenario", game.getScenario(), is(scenario))
            );
        }

        @Test
        public void absent() {
            final UUID id = UUID.randomUUID();

            final var result = getGameAsGameManager(id);

            assertThat(result.isEmpty(), is(true));
        }

    }

    @Nested
    public class GetGameAsNonGameManager {

        @Test
        public void solePlayer() {
            final var scenarioId = getAScenarioId();
            final var scenarioOptional = scenarioService.getScenario(scenarioId);
            assert scenarioOptional.isPresent();
            final var scenario = scenarioOptional.get();
            final var gameId = create(scenarioId).getIdentifier();
            final var userId = userService.add(createPlayerUserDetails(createUserName())).getId();
            userJoinsGame(userId, gameId);

            final var result = getGameAsNonGameManager(gameId, userId);

            assertTrue(result.isPresent(), "present");// guard
            final var game = result.get().game();
            assertAll(
                    () -> assertThat("recruiting", game.isRecruiting(), is(true)),
                    () -> assertThat("users", game.getUsers().values(), contains(userId)),
                    () -> assertThat("scenario", game.getScenario(), is(scenario))
            );
        }


        @Test
        public void noSuchGame() {
            final var gameId = UUID.randomUUID();
            final var userId = userService.add(createPlayerUserDetails(createUserName())).getId();
            final var result = getGameAsNonGameManager(gameId,
                    userId);

            assertThat("result is empty", result.isEmpty(), is(true));
        }

    }

    @Nested
    public class MayUserJoinGame {

        @Test
        public void mayNot() {
            final var scenario = getAScenarioId();
            final var game = create(scenario).getIdentifier();
            // Tough test: user exists and is permitted
            final var user = userService.add(new BasicUserDetails(createUserName(),
                    PASSWORD, Authority.ALL, true, true, true, true)).getId();

            endRecruitment(game);

            assertThat(mayUserJoinGame(user, game), is(false));
        }

        @Test
        public void may() {
            final var scenario = getAScenarioId();
            final var game = create(scenario).getIdentifier();
            // Tough test: user has minimum permission
            final var authorities = Set.of(Authority.ROLE_PLAYER);
            final var user = userService.add(new BasicUserDetails(createUserName(),
                    PASSWORD, authorities, true, true, true,
                    true)).getId();

            assertThat(mayUserJoinGame(user, game), is(true));
        }

    }

    @Nested
    public class UserJoinsGame {

        @Test
        public void gameNotRecruiting() {
            final var scenario = getAScenarioId();
            final var gameId = create(scenario).getIdentifier();
            // Tough test: user exists and is permitted
            final var user = userService.add(new BasicUserDetails(createUserName(),
                    PASSWORD, Authority.ALL, true, true, true, true)).getId();
            endRecruitment(gameId);

            assertThrows(IllegalGameStateException.class,
                    () -> userJoinsGame(user, gameId));
        }

        @Test
        public void unknownGame() {
            final var gameId = UUID.randomUUID();
            // Tough test: user exists and is permitted
            final var user = userService.add(new BasicUserDetails(createUserName(),
                    PASSWORD, Authority.ALL, true, true, true, true)).getId();

            assertThrows(NoSuchElementException.class, () -> userJoinsGame(
                    user, gameId));
        }

        @Test
        public void unknownUser() {
            final var userId = UUID.randomUUID();
            // Tough test: game exists and is recruiting
            final var scenario = getAScenarioId();
            final var game = create(scenario).getIdentifier();

            assertThrows(NoSuchElementException.class,
                    () -> userJoinsGame(userId, game));
        }

        @Test
        public void userAlreadyPlayingDifferentGame() {
            final var scenario = getAScenarioId();
            final var gameA = create(scenario).getIdentifier();
            final var gameB = create(scenario).getIdentifier();
            assert !gameA.equals(gameB);
            final var user = userService.add(new BasicUserDetails(createUserName(),
                    PASSWORD, Authority.ALL, true, true, true, true)).getId();
            userJoinsGame(user, gameA);

            assertThrows(UserAlreadyPlayingException.class,
                    () -> userJoinsGame(user, gameB));
        }

        @Test
        public void userNotPermitted() {
            // Tough test: game exists and is recruiting
            final var scenario = getAScenarioId();
            final var game = create(scenario).getIdentifier();
            // Tough test: user has all the other permissions
            final Set<Authority> authorities = EnumSet
                    .complementOf(EnumSet.of(Authority.ROLE_PLAYER));
            final var user = userService.add(new BasicUserDetails(createUserName(),
                    PASSWORD, authorities, true, true, true, true)).getId();

            assertThrows(SecurityException.class,
                    () -> userJoinsGame(user, game));
        }

        @Test
        public void mayJoin() {
            final var scenarioId = getAScenarioId();
            final var game = create(scenarioId).getIdentifier();
            // Tough test: user has minimum permission
            final var user = userService.add(new BasicUserDetails(createUserName(),
                    PASSWORD, Set.of(Authority.ROLE_PLAYER), true, true,
                    true, true)).getId();

            final Optional<Scenario> scenarioOptional = scenarioService.getScenario(scenarioId);
            assert scenarioOptional.isPresent();
            final var scenario = scenarioOptional.get();
            final var characterIds = scenario.getCharacters().stream().map(NamedUUID::getId).toList();
            final Optional<Game> game0Optional = getGameAsGameManager(game).map(FindGameResult::game);
            assertThat("game", game0Optional.isPresent());
            final var game0 = game0Optional.get();
            final var playedCharacters0 = game0.getUsers().keySet();
            final Optional<UUID> firstUnPlayedCharacter0Optional = characterIds.stream()
                    .filter(c -> !playedCharacters0.contains(c))
                    .findFirst();
            assertThat("firstUnPlayedCharacter", firstUnPlayedCharacter0Optional.isPresent());

            userJoinsGame(user, game);

            final Optional<UUID> currentGameOptional = getCurrentGameOfUser(user);
            assertThat("currentGame", currentGameOptional.isPresent());
            final var currentGame = currentGameOptional.get();
            assertThat("The current game of the user becomes the given game.", currentGame, is(game));
        }

    }

}
