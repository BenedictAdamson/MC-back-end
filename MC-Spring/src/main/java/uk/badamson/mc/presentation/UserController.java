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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uk.badamson.mc.rest.Paths;
import uk.badamson.mc.rest.Reasons;
import uk.badamson.mc.rest.UserDetailsRequest;
import uk.badamson.mc.rest.UserResponse;
import uk.badamson.mc.service.UserExistsException;
import uk.badamson.mc.service.UserSpringService;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@RestController
public class UserController {

    @Nonnull
    private final UserSpringService service;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "reference semantics")
    @Autowired
    public UserController(@Nonnull final UserSpringService service) {
        this.service = Objects.requireNonNull(service);
    }

    @PostMapping(Paths.USERS_PATH)
    @ResponseStatus(HttpStatus.CREATED)
    @RolesAllowed("MANAGE_USERS")
    public ResponseEntity<Void> addUser(
            @RequestBody final UserDetailsRequest detailsOfUserToAdd
    ) {
        try {
            final var userAdded = service.add(UserDetailsRequest.convertFromRequest(detailsOfUserToAdd));

            final var location = URI.create(Paths.createPathForUser(userAdded.getId()));
            final var headers = new HttpHeaders();
            headers.setLocation(location);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (final UserExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, Reasons.USER_EXISTS_CONFLICT, e);
        } catch (final IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), e);
        }
    }

    @GetMapping(Paths.USERS_PATH)
    public Stream<UserResponse> getAll() {
        return service.getUsers();
    }

    @GetMapping(Paths.SELF_PATH)
    @PreAuthorize("isAuthenticated()")
    @Nonnull
    public UserResponse getSelf(@Nonnull @AuthenticationPrincipal final SpringUser requestingUser) {
        return UserResponse.convertToResponse(SpringUser.convertFromSpring(requestingUser));
    }

    @GetMapping(Paths.USER_PATH_PATTERN)
    @RolesAllowed("MANAGE_USERS")
    @Nonnull
    public UserResponse getUser(@Nonnull @PathVariable("id") final UUID id) {
        return service.getUser(id)
                .map(UserResponse::convertToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Reasons.USER_NOT_FOUND));
    }
}
