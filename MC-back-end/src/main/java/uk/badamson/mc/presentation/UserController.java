package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2019-20.
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

import java.net.URI;
import java.security.Principal;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.User;
import uk.badamson.mc.service.UserExistsException;
import uk.badamson.mc.service.UserService;

/**
 * <p>
 * End-points for the user and users HTTP resources.
 * </p>
 */
@RestController
public class UserController {

   /**
    * <p>
    * Create a valid path for a user resource for a user that has a given unique
    * ID.
    * </p>
    * <p>
    * The created path is consistent with the path used with
    * {@link #getUser(UUID)}.
    * </p>
    *
    * @param id
    *           The identifier of the user
    * @return The path.
    * @throws NullPointerException
    *            If {@code id} is null.
    */
   @Nonnull
   public static String createPathForUser(@Nonnull final UUID id) {
      Objects.requireNonNull(id, "id");
      return "/api/user/" + id;
   }

   private final UserService service;

   /**
    * <p>
    * Construct a controller that associates with a given service layer
    * instance.
    * </p>
    * <ul>
    * <li>The {@linkplain #getService() service layer} of this controller is the
    * given service layer.</li>
    * </ul>
    *
    * @param service
    *           The service layer instance that this uses.
    * @throws NullPointerException
    *            If {@code service} is null
    */
   @Autowired
   public UserController(final UserService service) {
      this.service = Objects.requireNonNull(service, "service");
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
    * {@linkplain #createPathForUser(UUID) path} of the new user.</li>
    * </ul>
    *
    * @param userDetails
    *           The body of the request
    * @throws NullPointerException
    *            If {@code userDetails} is null
    * @throws ResponseStatusException
    *            <ul>
    *            <li>With a {@linkplain ResponseStatusException#getStatus()
    *            status} of {@linkplain HttpStatus#BAD_REQUEST 400 (Bad
    *            Request)} If the {@linkplain BasicUserDetails#getUsername()
    *            username} of {@code user} indicates it is the
    *            {@linkplain User#ADMINISTRATOR_USERNAME administrator}.</li>
    *            <li>With a {@linkplain ResponseStatusException#getStatus()
    *            status} of {@linkplain HttpStatus#CONFLICT 409 (Conflict)} If
    *            the {@linkplain BasicUserDetails#getUsername() username} of
    *            {@code userDetails} is already the username of a user.</li>
    *            </ul>
    * @return The response.
    */
   @PostMapping("/api/user")
   @ResponseStatus(HttpStatus.CREATED)
   @RolesAllowed("MANAGE_USERS")
   public ResponseEntity<Void> add(
            @RequestBody final BasicUserDetails userDetails) {
      try {
         final var user = service.add(userDetails);

         final var location = URI.create(createPathForUser(user.getId()));
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
   @GetMapping("/api/user")
   public Stream<User> getAll() {
      return service.getUsers();
   }

   /**
    * <p>
    * Behaviour of the GET verb for the self resource.
    * </p>
    *
    * @param id
    *           The authenticated identity of the current user
    * @return The user object for the current user.
    * @throws NullPointerException
    *            If {@code id} is null
    */
   @GetMapping("/api/self")
   @PreAuthorize("isAuthenticated()")
   @Nonnull
   public User getSelf(final Principal id) {
      Objects.requireNonNull(id, "id");
      return service.getUsers()
               .filter(u -> u.getUsername().equals(id.getName())).findAny()
               .get();
   }

   /**
    * <p>
    * The service layer instance that this uses.
    * </p>
    * <ul>
    * <li>Always associates with a (non null) service.</li>
    * </ul>
    *
    * @return the service
    */
   public final UserService getService() {
      return service;
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
    * @param id
    *           The unique ID of the wanted user.
    * @return The response.
    * @throws NullPointerException
    *            If {@code id} is null.
    * @throws ResponseStatusException
    *            With a {@linkplain ResponseStatusException#getStatus() status}
    *            of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if there
    *            is no user with the given {@code id}
    *            {@linkplain String#equals(Object) equivalent to} its
    *            {@linkplain User#getUsername() username}.
    */
   @GetMapping("/api/user/{id}")
   @Nonnull
   public User getUser(@Nonnull @PathVariable final UUID id) {
      Objects.requireNonNull(id, "id");
      try {
         return service.getUsers().filter(u -> u.getId().equals(id)).findAny()
                  .get();
      } catch (final NoSuchElementException e) {
         throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                  "unrecognized ID", e);
      }
   }
}
