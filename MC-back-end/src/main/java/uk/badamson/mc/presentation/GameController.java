package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2019-22.
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

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.Scenario;
import uk.badamson.mc.service.GameSpringService;
import uk.badamson.mc.service.IllegalGameStateException;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * <p>
 * End-points for the game HTTP resources.
 * </p>
 */
@RestController
public class GameController {

    /**
     * <p>
     * The format of URI paths for {@linkplain #createPathFor(Identifier) game
     * resources}.
     * </p>
     */
    public static final String GAME_PATH_PATTERN = "/api/scenario/{scenario}/game/{created:.+}";

    /**
     * <p>
     * The format of URI paths for {@linkplain #createPathForGames(UUID) games
     * collection resources}.
     * </p>
     */
    public static final String GAMES_PATH_PATTERN = "/api/scenario/{scenario}/game";

    /**
     * <p>
     * The format of time-stamps when used as parts of paths of URIs used by this
     * controller.
     * </p>
     */
    public static final DateTimeFormatter URI_DATETIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public static final String START_PARAM = "start";

    public static final String STOP_PARAM = "stop";
    @Nonnull
    private final GameSpringService gameService;

    GameController(@Nonnull final GameSpringService gameService) {
        this.gameService = Objects.requireNonNull(gameService, "gameService");
    }

    /**
     * <p>
     * Create a valid path for a game resource for a game that has a given
     * identifier.
     * </p>
     *
     * @param id The identifier of the game
     * @throws NullPointerException If {@code id} is null.
     * @see #getGame(UUID, Instant)
     */
    @Nonnull
    public static String createPathFor(@Nonnull final Game.Identifier id) {
        Objects.requireNonNull(id, "id");
        return createPathForGames(id.getScenario())
                + URI_DATETIME_FORMATTER.format(id.getCreated());
    }

    /**
     * <p>
     * Create a valid path for the games collection resource for a scenario with
     * a given identifier.
     * </p>
     * <p>
     * The created value is consistent with the path used for
     * {@link #createGameForScenario(UUID)} and {@link #getCreationTimes(UUID)}.
     * </p>
     *
     * @param scenario The identifier of the scenario
     * @return The path.
     * @throws NullPointerException If {@code scenario} is null.
     */
    @Nonnull
    public static String createPathForGames(@Nonnull final UUID scenario) {
        return ScenarioController.createPathFor(scenario) + "/game/";
    }

    @Nonnull
    public static String createPathForStarting(@Nonnull final Game.Identifier id) {
        return createPathFor(id) + "?" + START_PARAM;
    }

    @Nonnull
    public static String createPathForStopping(@Nonnull final Game.Identifier id) {
        return createPathFor(id) + "?" + STOP_PARAM;
    }

    /**
     * <p>
     * Create a game for a given scenario.
     * </p>
     * <ul>
     * <li>Creates a new game for the given scenario.</li>
     * <li>Returns a redirect to the newly created game. That is, a response with
     * <ul>
     * <li>A {@linkplain ResponseEntity#getStatusCode() status code} of
     * {@linkplain HttpStatus#FOUND 302 (Found)}</li>
     * <li>A {@linkplain HttpHeaders#getLocation()
     * Location}{@linkplain ResponseEntity#getHeaders() header} giving the
     * {@linkplain #createPathFor(Identifier) path} of the new game.</li>
     * </ul>
     * </li>
     * <li>The scenario ID part of the identifier of the newly created game is
     * equal to the given scenario identifier.</li>
     * </ul>
     *
     * @param scenario The unique ID of the scenario for which to create a game.
     * @return The response.
     * @throws NullPointerException    If {@code scenario} is null.
     * @throws ResponseStatusException <ul>
     *                                                                            <li>With a {@linkplain ResponseStatusException#getStatus()
     *                                                                            status} of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if
     *                                                                            there is no scenario with the given {@code scenario} ID
     *                                                                            {@linkplain UUID#equals(Object) equivalent to} its
     *                                                                            {@linkplain Scenario#getIdentifier() identifier}.</li>
     *                                                                            <li>With a {@linkplain ResponseStatusException#getStatus()
     *                                                                            status} of {@linkplain HttpStatus#INTERNAL_SERVER_ERROR 500
     *                                                                            (Internal Server Error)} if there is data access error.</li>
     *                                                                            </ul>
     */
    @PostMapping(GAMES_PATH_PATTERN)
    @Nonnull
    @RolesAllowed("MANAGE_GAMES")
    public ResponseEntity<Void> createGameForScenario(
            @Nonnull @PathVariable("scenario") final UUID scenario) {
        try {
            final var game = gameService.create(scenario);

            final var location = URI.create(createPathFor(game.getIdentifier()));
            final var headers = new HttpHeaders();
            headers.setLocation(location);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (final NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "unrecognized ID", e);
        } catch (final DataAccessException e) {
            // Hard to test
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage(), e);
        }
    }

    /**
     * <p>
     * Get the creation times of the games for a given scenario, formatted in a
     * manner suitable for use in URIs used by this controller.
     * </p>
     *
     * @param scenario The unique ID of the scenario for which to create a game.
     * @throws NullPointerException    If {@code scenario} is null.
     * @throws ResponseStatusException With a {@linkplain ResponseStatusException#getStatus() status}
     *                                 of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if there
     *                                 is no scenario with the given {@code scenario} ID
     *                                 {@linkplain UUID#equals(Object) equivalent to} its
     *                                 {@linkplain Scenario#getIdentifier() identifier}.
     */
    @GetMapping(GAMES_PATH_PATTERN)
    @Nonnull
    public Stream<String> getCreationTimes(
            @Nonnull @PathVariable("scenario") final UUID scenario) {
        try {
            return gameService.getCreationTimesOfGamesOfScenario(scenario)
                    .map(URI_DATETIME_FORMATTER::format);
        } catch (final NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "unrecognized ID", e);
        }
    }

    /**
     * @param scenario The unique ID of the scenario of the wanted game.
     * @param created  The creation time of the wanted game.
     * @throws NullPointerException    <ul>
     *                                 <li>If {@code scenario} is null.</li>
     *                                 <li>If {@code created} is null.</li>
     *                                 </ul>
     * @throws ResponseStatusException With a {@linkplain ResponseStatusException#getStatus() status}
     *                                 of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if there
     *                                 is no game that has {@linkplain Game#getIdentifier()
     *                                 identification information} equivalent to the given
     *                                 {@code scenario} and {@code created}.
     */
    @GetMapping(GAME_PATH_PATTERN)
    @RolesAllowed({"MANAGE_GAMES", "PLAYER"})
    @Nonnull
    public Game getGame(@Nonnull @PathVariable("scenario") final UUID scenario,
                        @Nonnull @PathVariable("created") final Instant created) {
        final var id = new Game.Identifier(scenario, created);
        final Optional<Game> game = gameService.getGame(id);
        if (game.isPresent()) {
            return game.get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "unrecognized IDs");
        }
    }

    @PostMapping(path = GAME_PATH_PATTERN, params = {START_PARAM})
    @RolesAllowed("MANAGE_GAMES")
    @Nonnull
    public ResponseEntity<Void> startGame(
            @Nonnull @AuthenticationPrincipal final SpringUser user,
            @Nonnull @PathVariable("scenario") final UUID scenario,
            @Nonnull @PathVariable("created") final Instant created) {
        Objects.requireNonNull(user, "user");
        final var gameId = new Game.Identifier(scenario, created);
        try {
            gameService.startGame(gameId);
            final var location = URI.create(createPathFor(gameId));
            final var headers = new HttpHeaders();
            headers.setLocation(location);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (final NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "unrecognized game ID", e);
        } catch (final IllegalGameStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(),
                    e);
        }
    }

    @PostMapping(path = GAME_PATH_PATTERN, params = {STOP_PARAM})
    @RolesAllowed("MANAGE_GAMES")
    @Nonnull
    public ResponseEntity<Void> stopGame(
            @Nonnull @AuthenticationPrincipal final SpringUser user,
            @Nonnull @PathVariable("scenario") final UUID scenario,
            @Nonnull @PathVariable("created") final Instant created) {
        Objects.requireNonNull(user, "user");
        final var gameId = new Game.Identifier(scenario, created);
        try {
            gameService.stopGame(gameId);
            final var location = URI.create(createPathFor(gameId));
            final var headers = new HttpHeaders();
            headers.setLocation(location);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (final NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "unrecognized game ID", e);
        } catch (final IllegalGameStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(),
                    e);
        }
    }
}
