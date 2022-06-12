package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2019-20,22.
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.badamson.mc.Authority;
import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.GamePlayers;
import uk.badamson.mc.User;
import uk.badamson.mc.service.GamePlayersService;
import uk.badamson.mc.service.GamePlayersSpringService;
import uk.badamson.mc.service.IllegalGameStateException;
import uk.badamson.mc.service.UserAlreadyPlayingException;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import java.net.URI;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>
 * End-points for the game players HTTP resources.
 * </p>
 */
@RestController
public class GamePlayersController {

    public static final String CURRENT_GAME_PATH = "/api/self/current-game";

    /**
     * <p>
     * The format of URI paths for
     * {@linkplain #createPathForGamePlayersOf(Identifier) game player
     * resources}.
     * </p>
     */
    public static final String GAME_PLAYERS_PATH_PATTERN = "/api/scenario/{scenario}/game/{created}/players";

    public static final String END_RECRUITMENT_PARAM = "endRecruitment";

    public static final String MAY_JOIN_PARAM = "mayJoin";

    public static final String JOIN_PARAM = "join";
    private final GamePlayersSpringService gamePlayersService;

    @Autowired
    public GamePlayersController(
            @Nonnull final GamePlayersSpringService gamePlayersService) {
        this.gamePlayersService = Objects.requireNonNull(gamePlayersService);
    }

    /**
     * <p>
     * Create a valid path for {@linkplain GamePlayers#endRecruitment() ending
     * recruitment} for a game that has a given identifier.
     * </p>
     * <p>
     * The created value is consistent with the path used for
     * {@link #endRecruitment(UUID, Instant)}.
     * </p>
     *
     * @param id The identifier of the game
     * @return The path.
     * @throws NullPointerException If {@code id} is null.
     */
    public static String createPathForEndRecruitmentOf(
            final Game.Identifier id) {
        return createPathForGamePlayersOf(id) + "?" + END_RECRUITMENT_PARAM;
    }

    /**
     * <p>
     * Create a valid path for a game players resource for a game that has a
     * given identifier.
     * </p>
     * <p>
     * The created value is consistent with the path used for
     * {@link #getGamePlayers(User, UUID, Instant)}.
     * </p>
     *
     * @param id The identifier of the game
     * @return The path.
     * @throws NullPointerException If {@code id} is null.
     */
    public static String createPathForGamePlayersOf(final Game.Identifier id) {
        return GameController.createPathFor(id) + "/players";
    }

    /**
     * <p>
     * Create a valid path for querying whether the current user may join a game
     * that has a given identifier.
     * </p>
     * <p>
     * The created value is consistent with the path used for
     * {@link #joinGame(User, UUID, Instant)}.
     * </p>
     *
     * @param id The identifier of the game
     * @return The path.
     * @throws NullPointerException If {@code id} is null.
     */
    public static String createPathForJoining(final Game.Identifier id) {
        return createPathForGamePlayersOf(id) + "?" + JOIN_PARAM;
    }

    /**
     * <p>
     * Create a valid path for querying whether the current user may join a game
     * that has a given identifier.
     * </p>
     * <p>
     * The created value is consistent with the path used for
     * {@link #mayJoinGame(User, UUID, Instant)}.
     * </p>
     *
     * @param id The identifier of the game
     * @return The path.
     * @throws NullPointerException If {@code id} is null.
     */
    public static String createPathForMayJoinQueryOf(final Game.Identifier id) {
        return createPathForGamePlayersOf(id) + "?" + MAY_JOIN_PARAM;
    }

    /**
     * <p>
     * Behaviour of the GET verb for a game players resource.
     * </p>
     * <ul>
     * <li>Returns a (non null) game players container.</li>
     * <li>The {@linkplain Identifier#getScenario() scenario identifier} of the
     * {@linkplain GamePlayers#getGame() game identifier} of the game players
     * container {@linkplain UUID#equals(Object) is equivalent to} the given
     * scenario ID</li>
     * <li>The {@linkplain Identifier#getCreated() creation time} of the
     * {@linkplain GamePlayers#getGame() game identifier} of the game players
     * container {@linkplain Instant#equals(Object) is equivalent to} the given
     * creation time</li>
     * </ul>
     * <ul>
     * <li>{@linkplain GamePlayersService#endRecruitment(Identifier) ends
     * recruitment} for the game with the given ID.</li>
     * <li>Returns a redirect to the modified game players resource. That is, a
     * response with
     * <ul>
     * <li>A {@linkplain ResponseEntity#getStatusCode() status code} of
     * {@linkplain HttpStatus#FOUND 302 (Found)}</li>
     * <li>A {@linkplain HttpHeaders#getLocation()
     * Location}{@linkplain ResponseEntity#getHeaders() header} giving the
     * {@linkplain #createPathForGamePlayersOf(Identifier) path} of the
     * resource.</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param scenario The unique ID of the scenario of the game.
     * @param created  The creation time of the game.
     * @return The response.
     * @throws NullPointerException    <ul>
     *                                                                            <li>If {@code scenario} is null.</li>
     *                                                                            <li>If {@code created} is null.</li>
     *                                                                            </ul>
     * @throws ResponseStatusException With a {@linkplain ResponseStatusException#getStatus() status}
     *                                 of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if there
     *                                 is no game that has {@linkplain Game#getIdentifier()
     *                                 identification information} equivalent to the given
     *                                 {@code scenario} and {@code created}.
     */
    @PostMapping(path = GAME_PLAYERS_PATH_PATTERN,
            params = {END_RECRUITMENT_PARAM})
    @RolesAllowed("MANAGE_GAMES")
    @Nonnull
    public ResponseEntity<Void> endRecruitment(
            @Nonnull @PathVariable("scenario") final UUID scenario,
            @Nonnull @PathVariable("created") final Instant created) {
        final var id = new Game.Identifier(scenario, created);
        try {
            gamePlayersService.endRecruitment(id);
            final var location = URI.create(createPathForGamePlayersOf(id));
            final var headers = new HttpHeaders();
            headers.setLocation(location);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (final NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "unrecognized IDs", e);
        }
    }

    @GetMapping(CURRENT_GAME_PATH)
    @Nonnull
    public ResponseEntity<Void> getCurrentGame(
            @AuthenticationPrincipal final User user) {
        if (user == null) {
            /*
             * Must return Not Found rather than Unauthorized, because otherwise
             * web browsers will pop up an authentication dialogue
             */
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Not Found Because Unauthorized");

        }
        final Optional<Identifier> gameId = gamePlayersService.getCurrentGameOfUser(user.getId());
        if (gameId.isPresent()) {
            final var headers = new HttpHeaders();
            headers.setLocation(URI.create(GameController.createPathFor(gameId.get())));
            return new ResponseEntity<>(headers, HttpStatus.TEMPORARY_REDIRECT);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No current game");
        }
    }

    /**
     * <p>
     * Behaviour of the GET verb for a game players resource.
     * </p>
     * <ul>
     * <li>Returns a (non null) game players container.</li>
     * <li>The {@linkplain Identifier#getScenario() scenario identifier} of the
     * {@linkplain GamePlayers#getGame() game identifier} of the game players
     * container {@linkplain UUID#equals(Object) is equivalent to} the given
     * scenario ID</li>
     * <li>The {@linkplain Identifier#getCreated() creation time} of the
     * {@linkplain GamePlayers#getGame() game identifier} of the game players
     * container {@linkplain Instant#equals(Object) is equivalent to} the given
     * creation time</li>
     * </ul>
     *
     * @param user     The authenticated identity of the current user
     * @param scenario The unique ID of the scenario of the game.
     * @param created  The creation time of the game.
     * @return The response.
     * @throws NullPointerException     <ul>
     *                                                                              <li>If {@code user} is null.</li>
     *                                                                              <li>If {@code scenario} is null.</li>
     *                                                                              <li>If {@code created} is null.</li>
     *                                                                              </ul>
     * @throws IllegalArgumentException If the {@code user} does not have the
     *                                  {@link Authority#ROLE_MANAGE_GAMES} or
     *                                  {@link Authority#ROLE_PLAYER} roles.
     * @throws ResponseStatusException  With a {@linkplain ResponseStatusException#getStatus() status}
     *                                  of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if there
     *                                  is no game that has {@linkplain Game#getIdentifier()
     *                                  identification information} equivalent to the given
     *                                  {@code scenario} and {@code created}.
     */
    @GetMapping(GAME_PLAYERS_PATH_PATTERN)
    @RolesAllowed({"PLAYER", "MANAGE_GAMES"})
    @Nonnull
    public GamePlayers getGamePlayers(
            @Nonnull @AuthenticationPrincipal final User user,
            @Nonnull @PathVariable("scenario") final UUID scenario,
            @Nonnull @PathVariable("created") final Instant created) {
        Objects.requireNonNull(user, "user");
        final var id = new Game.Identifier(scenario, created);

        final Optional<GamePlayers> gamePlayers;
        if (user.getAuthorities().contains(Authority.ROLE_MANAGE_GAMES)) {
            gamePlayers = gamePlayersService.getGamePlayersAsGameManager(id);
        } else if (user.getAuthorities().contains(Authority.ROLE_PLAYER)) {
            gamePlayers = gamePlayersService.getGamePlayersAsNonGameManager(id,
                    user.getId());
        } else {
            throw new IllegalArgumentException("Request not permitted for role");
        }

        if (gamePlayers.isPresent()) {
            return gamePlayers.get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "unrecognized IDs");
        }
    }

    /**
     * <p>
     * Add the requesting user as a {@linkplain GamePlayers#getUsers() player} of
     * a given game.
     * </p>
     * <ul>
     * <li>Returns a redirect to the {@linkplain GamePlayers game players}
     * resource of the game. That is, a response with
     * <ul>
     * <li>A {@linkplain ResponseEntity#getStatusCode() status code} of
     * {@linkplain HttpStatus#FOUND 302 (Found)}</li>
     * <li>A {@linkplain HttpHeaders#getLocation()
     * Location}{@linkplain ResponseEntity#getHeaders() header} giving the
     * {@linkplain #createPathForGamePlayersOf(Identifier) path} of the game
     * players resource.</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param user     The authenticated identity of the current user
     * @param scenario The unique ID of the scenario of the game.
     * @param created  The creation time of the game.
     * @return The response.
     * @throws NullPointerException    <ul>
     *                                                                            <li>If {@code user} is null.</li>
     *                                                                            <li>If {@code scenario} is null.</li>
     *                                                                            <li>If {@code created} is null.</li>
     *                                                                            </ul>
     * @throws ResponseStatusException <ul>
     *                                                                            <li>With a {@linkplain ResponseStatusException#getStatus()
     *                                                                            status} of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if
     *                                                                            there is no game that has {@linkplain Game#getIdentifier()
     *                                                                            identification information} equivalent to the given
     *                                                                            {@code scenario} and {@code created}.</li>
     *                                                                            <li>With a {@linkplain ResponseStatusException#getStatus()
     *                                                                            status} of {@linkplain HttpStatus#CONFLICT 409 (Conflict)} if
     *                                                                            any of the following are true:
     *                                                                            <ul>
     *                                                                            <li>If the {@code user} is already playing a different
     *                                                                            game.</li>
     *                                                                            <li>If the game is not {@linkplain GamePlayers#isRecruiting()
     *                                                                            recruiting} players.
     *                                                                            </ul>
     *                                                                            </li>
     *                                                                            <li>With a {@linkplain ResponseStatusException#getStatus()
     *                                                                            status} of {@linkplain HttpStatus#FORBIDDEN 403 (Forbidden)} if
     *                                                                            the {@code user} does not {@linkplain User#getAuthorities()
     *                                                                            have} {@linkplain Authority#ROLE_PLAYER permission} to play
     *                                                                            games.</li>
     *                                                                            </ul>
     */
    @PostMapping(path = GAME_PLAYERS_PATH_PATTERN, params = {JOIN_PARAM})
    @RolesAllowed("PLAYER")
    @Nonnull
    public ResponseEntity<Void> joinGame(
            @Nonnull @AuthenticationPrincipal final User user,
            @Nonnull @PathVariable("scenario") final UUID scenario,
            @Nonnull @PathVariable("created") final Instant created) {
        Objects.requireNonNull(user, "user");
        final var game = new Game.Identifier(scenario, created);
        try {
            gamePlayersService.userJoinsGame(user.getId(), game);
            final var location = URI.create(createPathForGamePlayersOf(game));
            final var headers = new HttpHeaders();
            headers.setLocation(location);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (final NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "unrecognized IDs", e);
        } catch (final IllegalGameStateException
                       | UserAlreadyPlayingException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(),
                    e);
        }
    }

    /**
     * <p>
     * Whether the requesting user can become one of the
     * {@linkplain GamePlayers#getUsers() players} of a given game.
     * </p>
     * <p>
     * That is, whether all the following are true.
     * </p>
     * <ul>
     * <li>The{@code user} is the ID of a known user.</li>
     * <li>The {@code game} is the ID of a known game.</li>
     * <li>The {@code user} is not already playing a different game.</li>
     * <li>The {@code user} {@linkplain User#getAuthorities() has}
     * {@linkplain Authority#ROLE_PLAYER permission} to play games. Note that the
     * given user need not be the current user.</li>
     * <li>The game is {@linkplain GamePlayers#isRecruiting() recruiting}
     * players.</li>
     * </ul>
     *
     * @param user     The authenticated identity of the current user
     * @param scenario The unique ID of the scenario of the game.
     * @param created  The creation time of the game.
     * @throws NullPointerException    <ul>
     *                                                                            <li>If {@code user} is null.</li>
     *                                                                            <li>If {@code scenario} is null.</li>
     *                                                                            <li>If {@code created} is null.</li>
     *                                                                            </ul>
     * @throws ResponseStatusException With a {@linkplain ResponseStatusException#getStatus() status}
     *                                 of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if there
     *                                 is no game that has {@linkplain Game#getIdentifier()
     *                                 identification information} equivalent to the given
     *                                 {@code scenario} and {@code created}.
     */
    @GetMapping(path = GAME_PLAYERS_PATH_PATTERN, params = {MAY_JOIN_PARAM})
    @RolesAllowed("PLAYER")
    public boolean mayJoinGame(@Nonnull @AuthenticationPrincipal final User user,
                               @Nonnull @PathVariable("scenario") final UUID scenario,
                               @Nonnull @PathVariable("created") final Instant created) {
        Objects.requireNonNull(user, "user");
        final var game = new Game.Identifier(scenario, created);
        if (gamePlayersService.getGamePlayersAsGameManager(game).isPresent()) {
            return gamePlayersService.mayUserJoinGame(user.getId(), game);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "unrecognized IDs");
        }
    }
}
