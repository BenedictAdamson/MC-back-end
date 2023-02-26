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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.badamson.mc.NamedUUID;
import uk.badamson.mc.Scenario;
import uk.badamson.mc.rest.Paths;
import uk.badamson.mc.rest.Reasons;
import uk.badamson.mc.rest.ScenarioResponse;
import uk.badamson.mc.service.ScenarioSpringService;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@RestController
public class ScenarioController {

    private final ScenarioSpringService service;

    @Autowired
    public ScenarioController(@Nonnull final ScenarioSpringService service) {
        this.service = Objects.requireNonNull(service);
    }

    /**
     * @return a list of all the scenarios.
     */
    @GetMapping(Paths.SCENARIOS_PATH)
    @Nonnull
    public Stream<NamedUUID> getAll() {
        return service.getNamedScenarioIdentifiers();
    }

    /**
     * @throws ResponseStatusException With a {@linkplain ResponseStatusException#getStatus() status}
     *                                 of {@linkplain HttpStatus#NOT_FOUND 404 (Not Found)} if there
     *                                 is no scenario with the given {@code id} ID.
     */
    @GetMapping(Paths.SCENARIO_PATH_PATTERN)
    @Nonnull
    public ScenarioResponse getScenario(@Nonnull @PathVariable("id") final UUID scenarioId) {
        final Optional<Scenario> scenario = service.getScenario(scenarioId);
        if (scenario.isPresent()) {
            return scenario.map(s -> ScenarioResponse.convertToResponse(scenarioId, s)).get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Reasons.SCENARIO_NOT_FOUND);
        }
    }
}
