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

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import uk.badamson.mc.NamedUUID;
import uk.badamson.mc.Scenario;
import uk.badamson.mc.service.ScenarioService;

/**
 * <p>
 * End-points for the scenario and scenarios HTTP resources.
 * </p>
 */
@RestController
public class ScenarioController {

   private final ScenarioService service;

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
   public ScenarioController(@Nonnull final ScenarioService service) {
      this.service = Objects.requireNonNull(service, "service");
   }

   /**
    * <p>
    * Behaviour of the GET verb for the scenarios list.
    * </p>
    * <p>
    * Returns a list of all the scenarios.
    * </p>
    *
    * @return The response.
    */
   @GetMapping("/api/scenario")
   @Nonnull
   public Stream<NamedUUID> getAll() {
      return service.getScenarioIdentifiers();
   }

   /**
    * <p>
    * Behaviour of the GET verb for a scenario resource.
    * </p>
    * <ul>
    * <li>Returns a (non null) scenario.</li>
    * <li>The {@linkplain NamedUUID#getId() unique identifier} of the
    * {@linkplain Scenario#getIdentifier() identification information} of the
    * given scenario {@linkplain UUID#equals(Object) is equivalent to} the given
    * ID</li>
    * </ul>
    *
    * @param id
    *           The unique ID of the wanted scenario.
    * @return The response.
    * @throws NullPointerException
    *            If {@code id} is null.
    * @throws ResponseStatusException
    *            <ul>
    *            <li>With a {@linkplain ResponseStatusException#getStatus()
    *            status} of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if
    *            there is no scenario with the given {@code id}
    *            {@linkplain UUID#equals(Object) is equivalent to} the
    *            {@linkplain NamedUUID#getId() unique identifier} of the
    *            {@linkplain Scenario#getIdentifier() identification
    *            information} of the scenario.</li>
    *            </ul>
    */
   @GetMapping("/api/scenario/{id}")
   @Nonnull
   public Scenario getScenario(@Nonnull @PathVariable final UUID id) {
      try {
         return service.getScenario(id).get();
      } catch (final NoSuchElementException e) {
         throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                  "unrecognized ID", e);
      }
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
   @Nonnull
   public final ScenarioService getService() {
      return service;
   }
}
