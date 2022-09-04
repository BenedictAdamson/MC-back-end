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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uk.badamson.mc.User;
import uk.badamson.mc.rest.AuthorityValue;
import uk.badamson.mc.rest.Paths;
import uk.badamson.mc.rest.UserDetailsRequest;
import uk.badamson.mc.rest.UserResponse;
import uk.badamson.mc.service.UserExistsException;
import uk.badamson.mc.service.UserSpringService;
import uk.badamson.mc.spring.SpringAuthority;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * <p>
 * End-points for the user and users HTTP resources.
 * </p>
 */
@RestController
public class UserController {

    private final UserSpringService service;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "reference semantics")
    @Autowired
    public UserController(@Nonnull final UserSpringService service) {
        this.service = Objects.requireNonNull(service);
    }

    /**
     * <p>
     * Behaviour of the POST verb for the user list.
     * </p>
     * <ul>
     * <li>Creates a new user having given user details.</li>
     * <li>Returns a redirect to the newly created user. That is, a response with
     * <ul>
     * <li>A {@linkplain ResponseEntity#getStatusCode() status code} of
     * {@linkplain HttpStatus#FOUND 302 (Found)}</li>
     * <li>A {@linkplain HttpHeaders#getLocation()
     * Location}{@linkplain ResponseEntity#getHeaders() header} giving the
     * {@linkplain Paths#createPathForUser(UUID) path} of the new user.</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param userDetails The body of the request
     * @return The response.
     * @throws NullPointerException    If {@code userDetails} is null
     * @throws ResponseStatusException <ul>
     *                                                                            <li>With a {@linkplain ResponseStatusException#getStatus()
     *                                                                            status} of {@linkplain HttpStatus#BAD_REQUEST 400 (Bad
     *                                                                            Request)} If the {@linkplain UserDetailsRequest#username()
     *                                                                            username} of {@code user} indicates it is the administrator.</li>
     *                                                                            <li>With a {@linkplain ResponseStatusException#getStatus()
     *                                                                            status} of {@linkplain HttpStatus#CONFLICT 409 (Conflict)} If
     *                                                                            the {@linkplain UserDetailsRequest#username() username} of
     *                                                                            {@code userDetails} is already the username of a user.</li>
     *                                                                            </ul>
     */
    @PostMapping(Paths.USERS_PATH)
    @ResponseStatus(HttpStatus.CREATED)
    @RolesAllowed("MANAGE_USERS")
    public ResponseEntity<Void> add(
            @RequestBody final UserDetailsRequest userDetails) {
        try {
            final var user = service.add(UserDetailsRequest.convertFromRequest(userDetails));

            final var location = URI.create(Paths.createPathForUser(user.getId()));
            final var headers = new HttpHeaders();
            headers.setLocation(location);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (final UserExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(),
                    e);
        } catch (final IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage(), e);
        }
    }

    /**
     * <p>
     * Behaviour of the GET verb for the users list.
     * </p>
     * <p>
     * Returns a list of all the users.
     * </p>
     *
     * @return The response.
     */
    @GetMapping(Paths.USERS_PATH)
    public Stream<UserResponse> getAll() {
        return service.getUsers();
    }

    /**
     * <p>
     * Behaviour of the GET verb for the self resource.
     * </p>
     *
     * @param user The authenticated identity of the current user
     * @return The user object for the current user.
     * @throws NullPointerException If {@code user} is null
     */
    @GetMapping(Paths.SELF_PATH)
    @PreAuthorize("isAuthenticated()")
    @Nonnull
    public UserResponse getSelf(@Nonnull @AuthenticationPrincipal final SpringUser user) {
        Objects.requireNonNull(user, "user");
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                AuthorityValue.convertToValue(SpringAuthority.convertFromSpring(user.getAuthorities())),
                user.isAccountNonExpired(),
                user.isAccountNonLocked(),
                user.isCredentialsNonExpired(),
                user.isEnabled()
        );
    }

    /**
     * <p>
     * Behaviour of the GET verb for a user resource.
     * </p>
     * <ul>
     * <li>Returns a (non null) user.</li>
     * <li>The {@linkplain User#getUsername() username} of the returned user
     * {@linkplain String#equals(Object) is equivalent to} the given ID</li>
     * </ul>
     *
     * @param id The unique ID of the wanted user.
     * @return The response.
     * @throws NullPointerException    If {@code id} is null.
     * @throws ResponseStatusException With a {@linkplain ResponseStatusException#getStatus() status}
     *                                 of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if there
     *                                 is no user with the given {@code id}
     *                                 {@linkplain String#equals(Object) equivalent to} its
     *                                 {@linkplain User#getUsername() username}.
     */
    @GetMapping(Paths.USER_PATH_PATTERN)
    @RolesAllowed("MANAGE_USERS")
    @Nonnull
    public UserResponse getUser(@Nonnull @PathVariable final UUID id) {
        final Optional<User> user = service.getUser(id);
        if (user.isPresent()) {
            return user.map(UserResponse::convertToResponse).get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "unrecognized ID");
        }
    }
}
