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

import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import uk.badamson.mc.User;
import uk.badamson.mc.service.Service;

/**
 * <p>
 * End-points for the user and users HTTP resources.
 * </p>
 */
@RestController
public class UserController {

   private final Service service;

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
   public UserController(final Service service) {
      this.service = Objects.requireNonNull(service, "service");
   }

   /**
    * <p>
    * Behaviour of the POST verb for the user list.
    * </p>
    * <p>
    * Adds the given user to the list of users.
    * </p>
    *
    * @param user
    *           The body of the request
    * @throws NullPointerException
    *            If {@code user} is null
    * @throws ResponseStatusException
    *            With a {@linkplain ResponseStatusException#getStatus() status}
    *            of {@linkplain HttpStatus#BAD_REQUEST 400 (Bad Request)} If the
    *            {@linkplain User#getUsername() username} of {@code user}
    *            indicates it is the {@linkplain User#ADMINISTRATOR_USERNAME
    *            administrator}.
    */
   @PostMapping("/api/user")
   @ResponseStatus(HttpStatus.CREATED)
   @RolesAllowed("MANAGE_USERS")
   public void add(@RequestBody final User user) {
      try {
         service.add(user);
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
    * The service layer instance that this uses.
    * </p>
    * <ul>
    * <li>Always associates with a (non null) service.</li>
    * </ul>
    *
    * @return the service
    */
   public final Service getService() {
      return service;
   }
}
