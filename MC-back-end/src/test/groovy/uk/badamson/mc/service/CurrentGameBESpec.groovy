package uk.badamson.mc.service


import org.hamcrest.Matchers
import org.springframework.boot.test.context.SpringBootTest
import uk.badamson.mc.Authority
import uk.badamson.mc.GamePlayers
import uk.badamson.mc.TestConfiguration

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static spock.util.matcher.HamcrestSupport.expect

/**
 * © Copyright Benedict Adamson 2021-22.
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
 * The system provides information about the game that a player is currently playing
 */
@SpringBootTest(classes = TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CurrentGameBESpec extends BESpecification {

    def "May examine current-game only if playing"() {
        given: "has a game"
        def scenario = chooseScenario()
        gameService.create(scenario.identifier).identifier

        and: "user has the player role but is not playing"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_PLAYER))

        when: "examine the current game of the user while logged in"
        final def response = requestGetCurrentGame(user)

        then: "it does not indicate that the user has a current game"
        response.andExpect(status().isNotFound())
    }

    def "Examine current game"() {
        given: "has a game"
        def scenario = chooseScenario()
        def gameId = gameService.create(scenario.identifier).identifier

        and: "user has the player role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_PLAYER))

        and: "user is playing the game"
        gamePlayersService.userJoinsGame(user.id, gameId)

        when: "examine whether have a current game while logged in"
        def response = requestGetCurrentGame(user)

        then: "indicates that the user has a current game"
        response.andExpect(status().isTemporaryRedirect())
        final def location = expectTemporaryRedirect(response)
        location != null

        when: "examine the current game"
        def currentGameId = parseGamePath(location)
        response = requestGetGamePlayers(currentGameId, user)

        then: "the game indicates which character the user is playing"
        def gamePlayers = expectEncodedResponse(response, GamePlayers.class)
        expect(gamePlayers.users.values(), Matchers.hasItem(user.id))
    }
}
