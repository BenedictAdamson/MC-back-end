package uk.badamson.mc.rest;
/*
 * © Copyright Benedict Adamson 2020-24.
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
import org.springframework.core.ParameterizedTypeReference;
import uk.badamson.mc.presentation.ScenarioController;
import uk.badamson.mc.presentation.SecurityConfiguration;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

/**
 * Tests {@link SecurityConfiguration}.
 * Tests Spring annotations on {@link ScenarioController}.
 */
public class ScenarioRestIT extends RestIT {

    /**
     * Tests Spring annotations on {@link ScenarioController#getAll()}
     */
    @Test
    public void getAll() {
        final var response = getMcBackEndClient().getAllScenarios();

        response.expectStatus().isOk();
        response.expectBody(new ParameterizedTypeReference<List<NamedUUID>>() {})
                .value(notNullValue())
                .value(not(empty()));
    }

    /**
     * Tests Spring annotations on {@link ScenarioController#getScenario(UUID)}
     */
    @Nested
    public class GetScenario {

        @Test
        public void absent() {
            final var id = UUID.randomUUID();

            final var response = getMcBackEndClient().getScenario(id);

            response.expectStatus().isNotFound();
        }

        @Test
        public void present() {
            final var id = getKnownScenarioId();

            final var response = getMcBackEndClient().getScenario(id);

            response.expectStatus().isOk();
            response.expectBody(ScenarioResponse.class)
                    .value(ScenarioResponse::identifier, is(id));
        }

        private UUID getKnownScenarioId() {
            return getScenarios().findAny().orElseThrow().getId();
        }

    }

}
