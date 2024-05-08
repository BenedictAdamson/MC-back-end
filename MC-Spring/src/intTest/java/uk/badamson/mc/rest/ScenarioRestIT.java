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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.testcontainers.containers.MongoDBContainer;
import uk.badamson.mc.McBackEndClient;
import uk.badamson.mc.McBackEndProcess;
import uk.badamson.mc.ProcessFixtures;
import uk.badamson.mc.presentation.ScenarioController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.*;

public class ScenarioRestIT {
    private static final String MONGO_DB_PASSWORD = "LetMeIn1";
    private static final String ADMINISTRATOR_PASSWORD = ProcessFixtures.ADMINISTRATOR.getPassword();

    private static MongoDBContainer mongoDBContainer;
    private static McBackEndProcess mcBackEndProcess;
    private static McBackEndClient mcBackEndClient;

    @BeforeAll
    public static void setUp() throws TimeoutException {
        mongoDBContainer = new MongoDBContainer(ProcessFixtures.MONGO_DB_IMAGE);
        mongoDBContainer.start();
        final var mongoDBPath = mongoDBContainer.getReplicaSetUrl();
        mcBackEndProcess = new McBackEndProcess(mongoDBPath, MONGO_DB_PASSWORD, ADMINISTRATOR_PASSWORD);
        mcBackEndClient = new McBackEndClient(
                "localhost", mcBackEndProcess.getServerPort(), ADMINISTRATOR_PASSWORD
        );
    }

    @AfterAll
    public static void tearDown() {
        mcBackEndProcess.close();
        mongoDBContainer.close();
    }


    /**
     * Tests {@link ScenarioController#getAll()}
     */
    @Test
    public void getAll() {
        final var response = mcBackEndClient.getAllScenarios();

        response.expectStatus().isOk();
        response.expectBody(new ParameterizedTypeReference<List<NamedUUID>>() {})
                .value(notNullValue())
                .value(not(empty()));
    }

    /**
     * Tests {@link ScenarioController#getScenario(UUID)}
     */
    @Nested
    public class GetScenario {

        @Test
        public void absent() {
            final var id = UUID.randomUUID();

            final var response = mcBackEndClient.getScenario(id);

            response.expectStatus().isNotFound();
        }

        @Test
        public void present() {
            final var id = getKnownScenarioId();

            final var response = mcBackEndClient.getScenario(id);

            response.expectStatus().isOk();
            response.expectBody(ScenarioResponse.class)
                    .value(ScenarioResponse::identifier, is(id));
        }

        private UUID getKnownScenarioId() {
            return mcBackEndClient.getScenarios().findAny().orElseThrow().getId();
        }

    }

}
