package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2024.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;
import uk.badamson.mc.NamedUUID;
import uk.badamson.mc.rest.ScenarioResponse;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ScenarioControllerTest extends ControllerTest {
    private final ScenarioController scenarioController = new ScenarioController(scenarioService);

    @Test
    public void getAll() {
        Stream<NamedUUID> result = scenarioController.getAll();

        assertThat(result, notNullValue());
        Collection<NamedUUID> all = result.toList();
        assertThat(all, not(empty()));
    }

    @Nested
    public class GetScenario {

        @Test
        public void unknownScenario() {
            final var scenario = UUID.randomUUID();

            final var exception = assertThrows(ResponseStatusException.class, () -> getScenario(scenario));

            assertThat(exception.getStatusCode(), is(HttpStatus.NOT_FOUND));
        }

        @Test
        public void knownScenario() {
            final var scenario = getValidScenarioId();

            final var response = getScenario(scenario);

            assertThat(response.identifier(), is(scenario));
        }

        private ScenarioResponse getScenario(final UUID scenarioId) {
            final var response = scenarioController.getScenario(scenarioId);
            assertThat(response, notNullValue());
            return response;
        }
    }

}
