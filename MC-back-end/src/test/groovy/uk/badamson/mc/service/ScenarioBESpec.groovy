package uk.badamson.mc.service

import com.fasterxml.jackson.core.type.TypeReference
import org.hamcrest.Matchers
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Unroll
import uk.badamson.mc.*

import java.time.Instant

import static spock.util.matcher.HamcrestSupport.expect

/** © Copyright Benedict Adamson 2019,20,22.
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

/**
 * The Mission Command game provides multiple scenarios that can be played.
 */
@SpringBootTest(classes = TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ScenarioBESpec extends BESpecification {

    def "List scenarios"() {
        given: "not logged in"
        final User user = null

        when: "examine scenarios"
        def response = requestGetScenarios(user)

        then: "the response is the list of scenarios"
        def scenarios = expectEncodedResponse(response, new TypeReference<List<NamedUUID>>() {})
        expect(scenarios, Matchers.not(Matchers.empty()))
    }

    def "Examine scenario anonymously"() {
        given: "a scenario that has a game"
        def scenarioId = chooseScenario().identifier
        gameService.create(scenarioId).identifier

        and: "not logged in"
        final User user = null

        when: "try to examine the scenario"
        final def scenarioResponse = requestGetScenario(scenarioId, user)
        final def gameCreationTimesResponse = requestGetGameCreationTimes(scenarioId, user)

        then: "provides the scenario"
        def scenario = expectEncodedResponse(scenarioResponse, Scenario.class)
        def gameCreationTimes = expectEncodedResponse(gameCreationTimesResponse, new TypeReference<Set<Instant>>() {})

        and: "the scenario includes the scenario description"
        expect(scenario.description, Matchers.not(Matchers.emptyOrNullString()))

        and: "the scenario includes the list of playable characters of that scenario"
        expect(scenario.characters, Matchers.not(Matchers.empty()))

        and: "the scenario has a list of games of that scenario"
        expect(gameCreationTimes, Matchers.hasSize(1))
    }

    @Unroll
    def "Examine scenario with authorization"() {
        given: "a scenario that has a game"
        def scenarioId = chooseScenario().identifier
        gameService.create(scenarioId).identifier

        and: "logged in as a user with the #role"
        def user = addUserWithAuthorities(EnumSet.of(role))

        when: "try to examine the scenario"
        final def scenarioResponse = requestGetScenario(scenarioId, user)
        final def gameCreationTimesResponse = requestGetGameCreationTimes(scenarioId, user)

        then: "provides the scenario"
        def scenario = expectEncodedResponse(scenarioResponse, Scenario.class)
        def gameCreationTimes = expectEncodedResponse(gameCreationTimesResponse, new TypeReference<Set<Instant>>() {})

        then: "the scenario includes the scenario description"
        expect(scenario.description, Matchers.not(Matchers.emptyOrNullString()))

        and: "the scenario includes the list of playable characters of that scenario"
        expect(scenario.characters, Matchers.not(Matchers.empty()))

        and: "the scenario includes the list of games of that scenario"

        and: "the scenario has a list of games of that scenario"
        expect(gameCreationTimes, Matchers.hasSize(1))

        where:
        role << [Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_GAMES]
    }
}