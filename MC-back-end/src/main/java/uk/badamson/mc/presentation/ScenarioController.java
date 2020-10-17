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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
   public ScenarioController(final ScenarioService service) {
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
   public Stream<Scenario.Identifier> getAll() {
      return service.getScenarioIdentifiers();
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
   public final ScenarioService getService() {
      return service;
   }
}
