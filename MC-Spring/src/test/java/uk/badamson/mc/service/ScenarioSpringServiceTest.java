package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2019-24.
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.badamson.mc.Fixtures;
import uk.badamson.mc.NamedUUID;
import uk.badamson.mc.Scenario;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Tag("Spring")
public class ScenarioSpringServiceTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(Fixtures.MONGO_DB_IMAGE);

    @Autowired
    ScenarioSpringService service;

    private Set<UUID> getIds() {
        return service.getScenarioIdentifiers().collect(toUnmodifiableSet());
    }

    private Optional<Scenario> getScenario(
            final UUID id) {
        final var result = service.getScenario(id);

        assertThat(result, notNullValue());
        return result;
    }

    @Test
    public void getScenarioIdentifiers() {
        final var scenarios = service.getScenarioIdentifiers();

        assertThat(scenarios, notNullValue());
        final var scenariosList = scenarios.toList();
        final Set<UUID> scenariosSet;
        try {
            scenariosSet = scenariosList.stream().collect(toUnmodifiableSet());
        } catch (final NullPointerException e) {
            throw new AssertionError(
                    "The returned stream will not include a null element", e);
        }
        assertThat("Does not contain duplicates.", scenariosSet, hasSize(scenariosList.size()));
    }


    @Test
    public void getNamedScenarioIdentifiersTest() {
        final var ids = getIds();

        final Set<UUID> expectedIdentifiers = service.getScenarioIdentifiers()
                .collect(toUnmodifiableSet());

        final var scenarios = service.getNamedScenarioIdentifiers();

        assertThat(scenarios, notNullValue());
        final var scenariosAsList = scenarios.toList();
        final var identifiersOfScenarios = scenariosAsList.stream()
                .map(NamedUUID::getId).collect(toUnmodifiableSet());
        assertThat(
                "Contains a named identifier corresponding to each scenario identifier",
                identifiersOfScenarios, is(expectedIdentifiers));

        final var namedIds = scenariosAsList.stream();

        final var idsOfNamedIds = namedIds.map(NamedUUID::getId)
                .collect(toUnmodifiableSet());
        assertThat("Contains a named identifier corresponding to each scenario identifier.",
                ids, is(idsOfNamedIds));
    }

    @Nested
    public class GetScenario {

        @Test
        public void absent() {
            final var ids = getIds();
            var id = UUID.randomUUID();
            while (ids.contains(id)) {
                id = UUID.randomUUID();
            }

            final var result = getScenario(id);

            assertThat(result.isEmpty(), is(true));
        }

        @Test
        public void present() {
            final Optional<UUID> idOptional = getIds().stream().findAny();
            assertThat("id", idOptional.isPresent());
            final var id = idOptional.get();

            final var result = getScenario(id);

            assertThat(result.isPresent(), is(true));
        }
    }
}
