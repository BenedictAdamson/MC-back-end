package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2019-23.
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
import uk.badamson.mc.FindGameResult;
import uk.badamson.mc.NamedUUID;
import uk.badamson.mc.rest.GameResponse;
import uk.badamson.mc.rest.Paths;
import uk.badamson.mc.rest.Reasons;
import uk.badamson.mc.service.GameSpringService;
import uk.badamson.mc.service.IllegalGameStateException;
import uk.badamson.mc.service.UserAlreadyPlayingException;
import uk.badamson.mc.spring.SpringAuthority;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import java.net.URI;
import java.util.*;

@RestController
public class GameController {

    @Nonnull
    private final GameSpringService gameService;

    @Autowired
    GameController(@Nonnull final GameSpringService gameService) {
        this.gameService = Objects.requireNonNull(gameService, "gameService");
    }

    @Nonnull
    private static ResponseEntity<Void> createRedirectResponseForGame(@Nonnull UUID game) {
        URI location = URI.create(Paths.createPathForGame(game));
        final var headers = new HttpHeaders();
        headers.setLocation(location);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping(Paths.GAMES_PATH_PATTERN)
    @Nonnull
    @RolesAllowed("MANAGE_GAMES")
    public ResponseEntity<Void> createGameForScenario(
            @Nonnull @PathVariable("scenario") final UUID scenario) {
        try {
            final var game = gameService.create(scenario).getIdentifier();
            return createRedirectResponseForGame(game);
        } catch (final NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Reasons.SCENARIO_NOT_FOUND, e);
        } catch (final DataAccessException e) {
            // Hard to test
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), e
            );
        }
    }

    @GetMapping(Paths.GAMES_PATH_PATTERN)
    @RolesAllowed({"MANAGE_GAMES", "PLAYER"})
    @Nonnull
    public Set<NamedUUID> getGameIdentifiersOfScenario(
            @Nonnull @PathVariable("scenario") final UUID scenario) {
        try {
            return gameService.getGameIdentifiersOfScenario(scenario);
        } catch (final NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Reasons.SCENARIO_NOT_FOUND, e);
        }
    }

    @GetMapping(Paths.GAME_PATH_PATTERN)
    @RolesAllowed({"MANAGE_GAMES", "PLAYER"})
    @Nonnull
    public GameResponse getGame(
            @Nonnull @AuthenticationPrincipal final SpringUser requestingUser,
            @Nonnull @PathVariable("game") final UUID game) {
        final Optional<FindGameResult> findResult;
        if (requestingUser.getAuthorities().contains(SpringAuthority.ROLE_MANAGE_GAMES)) {
            findResult = gameService.getGameAsGameManager(game);
        } else if (requestingUser.getAuthorities().contains(SpringAuthority.ROLE_PLAYER)) {
            findResult = gameService.getGameAsNonGameManager(game, requestingUser.getId());
        } else {
            throw new IllegalArgumentException("Request not permitted for role");
        }

        if (findResult.isPresent()) {
            return findResult.map(fr -> GameResponse.convertToResponse(game, fr.scenarioId(), fr.game())).get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Reasons.GAME_NOT_FOUND);
        }
    }

    @PostMapping(path = Paths.GAME_PATH_PATTERN, params = {Paths.GAME_START_PARAM})
    @RolesAllowed("MANAGE_GAMES")
    @Nonnull
    public ResponseEntity<Void> startGame(
            @Nonnull @AuthenticationPrincipal final SpringUser requestingUser,
            @Nonnull @PathVariable("game") final UUID game) {
        Objects.requireNonNull(requestingUser, "requestingUser");
        try {
            gameService.startGame(game);
            return createRedirectResponseForGame(game);
        } catch (final NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Reasons.GAME_NOT_FOUND, e);
        } catch (final IllegalGameStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, Reasons.GAME_STATE_CONFLICT, e);
        }
    }

    @PostMapping(path = Paths.GAME_PATH_PATTERN, params = {Paths.GAME_STOP_PARAM})
    @RolesAllowed("MANAGE_GAMES")
    @Nonnull
    public ResponseEntity<Void> stopGame(
            @Nonnull @AuthenticationPrincipal final SpringUser requestingUser,
            @Nonnull @PathVariable("game") final UUID game) {
        Objects.requireNonNull(requestingUser, "requestingUser");
        try {
            gameService.stopGame(game);
            return createRedirectResponseForGame(game);
        } catch (final NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Reasons.GAME_NOT_FOUND, e);
        }
    }

    @PostMapping(path = Paths.GAME_PATH_PATTERN, params = {Paths.END_GAME_RECRUITMENT_PARAM})
    @RolesAllowed("MANAGE_GAMES")
    @Nonnull
    public ResponseEntity<Void> endRecruitment(
            @Nonnull @PathVariable("game") final UUID game) {
        try {
            gameService.endRecruitment(game);
            return createRedirectResponseForGame(game);
        } catch (final NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Reasons.GAME_NOT_FOUND, e);
        }
    }

    @GetMapping(Paths.CURRENT_GAME_PATH)
    @Nonnull
    public ResponseEntity<Void> getCurrentGame(
            @AuthenticationPrincipal final SpringUser user) {
        if (user == null) {
            /*
             * Must return Not Found rather than Unauthorized, because otherwise
             * web browsers will pop up an authentication dialogue
             */
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, HttpStatus.UNAUTHORIZED.getReasonPhrase());

        }
        final Optional<UUID> game = gameService.getCurrentGameOfUser(user.getId());
        if (game.isPresent()) {
            final var headers = new HttpHeaders();
            headers.setLocation(URI.create(Paths.createPathForGame(game.get())));
            return new ResponseEntity<>(headers, HttpStatus.TEMPORARY_REDIRECT);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Reasons.GAME_NOT_FOUND);
        }
    }

    @PostMapping(path = Paths.GAME_PATH_PATTERN, params = {Paths.JOIN_GAME_PARAM})
    @RolesAllowed("PLAYER")
    @Nonnull
    public ResponseEntity<Void> joinGame(
            @Nonnull @AuthenticationPrincipal final SpringUser user,
            @Nonnull @PathVariable("game") final UUID game) {
        Objects.requireNonNull(user, "user");
        try {
            gameService.userJoinsGame(user.getId(), game);
            return createRedirectResponseForGame(game);
        } catch (final NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Reasons.GAME_NOT_FOUND, e);
        } catch (final IllegalGameStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, Reasons.GAME_STATE_CONFLICT, e);
        } catch (final UserAlreadyPlayingException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, Reasons.USER_STATE_CONFLICT, e);
        }
    }

    @GetMapping(path = Paths.GAME_PATH_PATTERN, params = {Paths.MAY_JOIN_GAME_PARAM})
    @RolesAllowed("PLAYER")
    public boolean mayJoinGame(@Nonnull @AuthenticationPrincipal final SpringUser user,
                               @Nonnull @PathVariable("game") final UUID game) {
        Objects.requireNonNull(user, "user");
        if (gameService.getGameAsGameManager(game).isPresent()) {
            return gameService.mayUserJoinGame(user.getId(), game);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Reasons.GAME_NOT_FOUND);
        }
    }
}
