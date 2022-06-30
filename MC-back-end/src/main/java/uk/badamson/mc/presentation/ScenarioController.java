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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.badamson.mc.NamedUUID;
import uk.badamson.mc.Scenario;
import uk.badamson.mc.rest.ScenarioResponse;
import uk.badamson.mc.service.ScenarioSpringService;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * <p>
 * End-points for the scenario and scenarios HTTP resources.
 * </p>
 */
@RestController
public class ScenarioController {

    private final ScenarioSpringService service;

    @Autowired
    public ScenarioController(@Nonnull final ScenarioSpringService service) {
        this.service = Objects.requireNonNull(service);
    }

    /**
     * <p>
     * Create a valid path for a scenario resource for a scenario that has a given
     * identifier.
     * </p>
     * <p>
     * The created value is consistent with the path used for
     * {@link #getScenario(UUID)}.
     * </p>
     *
     * @param id The identifier of the scenario
     * @return The path.
     * @throws NullPointerException If {@code id} is null.
     */
    public static String createPathFor(final UUID id) {
        Objects.requireNonNull(id, "id");
        return "/api/scenario/" + id;
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
        return service.getNamedScenarioIdentifiers();
    }

    /**
     * <p>
     * Behaviour of the GET verb for a scenario resource.
     * </p>
     * <ul>
     * <li>Returns a (non null) scenario.</li>
     * <li>The {@linkplain Scenario#getIdentifier() identifier} of the returned
     * scenario {@linkplain UUID#equals(Object) is equivalent to} the given
     * ID</li>
     * </ul>
     *
     * @param id The unique ID of the wanted scenario.
     * @return The response.
     * @throws NullPointerException    If {@code id} is null.
     * @throws ResponseStatusException With a {@linkplain ResponseStatusException#getStatus() status}
     *                                 of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if there
     *                                 is no scenario with the given {@code id}
     *                                 {@linkplain UUID#equals(Object) equivalent to} its
     *                                 {@linkplain Scenario#getIdentifier() identifier}.
     */
    @GetMapping("/api/scenario/{id}")
    @Nonnull
    public ScenarioResponse getScenario(@Nonnull @PathVariable final UUID id) {
        final Optional<Scenario> scenario = service.getScenario(id);
        if (scenario.isPresent()) {
            return scenario.map(ScenarioResponse::convertToResponse).get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "unrecognized ID");
        }
    }
}
