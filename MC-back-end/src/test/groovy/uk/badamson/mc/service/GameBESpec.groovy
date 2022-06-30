package uk.badamson.mc.service

import org.hamcrest.Matchers
import org.springframework.boot.test.context.SpringBootTest
import uk.badamson.mc.Game
import uk.badamson.mc.GamePlayers
import uk.badamson.mc.TestConfiguration
import uk.badamson.mc.Authority

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static spock.util.matcher.HamcrestSupport.expect
/**
 * © Copyright Benedict Adamson 2020-22.
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
 * The Mission Command game can have multiple games (plays) for each scenario
 */
@SpringBootTest(classes = TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class GameBESpec extends BESpecification {

    def "Examine game as player"() {
        given: "has a game"
        def scenarioId = chooseScenario().identifier
        def gameId = gameService.create(scenarioId).identifier

        and: "user has the player role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_PLAYER))

        when: "try to examine the game"
        def gameResponse = requestGetGame(gameId, user)
        def gamePlayersResponse = requestGetGamePlayers(gameId, user)
        def mayJoinResponse = requestGetMayJoinQuery(gameId, user)

        then: "provides the game"
        gameResponse.andExpect(status().isOk())
        gamePlayersResponse.andExpect(status().isOk())
        mayJoinResponse.andExpect(status().isOk())
        def game = expectEncodedResponse(gameResponse, Game.class)
        def gamePlayers = expectEncodedResponse(gamePlayersResponse, GamePlayers.class)
        def mayJoin = expectEncodedResponse(mayJoinResponse, Boolean.class)

        and: "the game indicates its scenario"
        game.identifier.scenario == scenarioId

        and: "the game indicates the date and time that the game was set up"
        game.identifier.created != null

        and: "the game indicates whether it is recruiting players"
        expect(gamePlayers.recruiting, Matchers.instanceOf(Boolean.class))

        and: "the game indicates whether the user may join the game"
        mayJoin != null

        and: "the game indicates whether it has players"
        gamePlayers.users != null

        and: "the game indicates whether it is running"
        game.runState != null

        and: "the game indicates which character (if any) the user is playing"
        expect(gamePlayers.users, Matchers.instanceOf(Map.class))

        and: "the game does not indicate which characters are played by which (other) users"
        gamePlayers.users.values().stream().map(userId -> !(userId == user.id)).count() == 0
    }

    def "Examine game as game manager"() {
        given: "has a game"
        def scenarioId = chooseScenario().identifier
        def gameId = gameService.create(scenarioId).identifier

        and: "user has the manage games role but not the player role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_MANAGE_GAMES))

        when: "try to examine the game"
        def mayJoinResponse = requestGetMayJoinQuery(gameId, user)
        def gameResponse = requestGetGame(gameId, user)
        def gamePlayersResponse = requestGetGamePlayers(gameId, user)

        then: "provides the game"
        gameResponse.andExpect(status().isOk())
        gamePlayersResponse.andExpect(status().isOk())
        def game = expectEncodedResponse(gameResponse, Game.class)
        def gamePlayers = expectEncodedResponse(gamePlayersResponse, GamePlayers.class)

        and: "the game indicates its scenario"
        game.identifier.scenario == scenarioId

        and: "the game indicates the date and time that the game was set up"
        game.identifier.created != null

        and: "the game indicates whether it is recruiting players"
        expect(gamePlayers.recruiting, Matchers.instanceOf(Boolean.class))

        and: "the game indicates that the user may not join the game"
        mayJoinResponse.andExpect(status().isForbidden())

        and: "the game indicates whether it has players"
        gamePlayers.users != null

        and: "the game indicates whether it is running"
        game.runState != null

        and: "the game indicates which character (if any) the user is playing"
        expect(gamePlayers.users, Matchers.instanceOf(Map.class))

        and: "the game indicates which characters are played by which users"
        expect(gamePlayers.users, Matchers.instanceOf(Map.class))
    }

    def "Add game"() {
        given: "has a scenario without any games"
        def scenarioId = chooseScenario().identifier

        and: "user has the manage games role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_MANAGE_GAMES))

        when: "try to create a game for the scenario"
        def response = requestAddGame(scenarioId, user)

        then: "accepts the creation of the game"
        final def location = expectFound(response)
        location != null
        final def gameId = parseGamePath(location)
        final def gameOptional = gameService.getGame(gameId)
        final def gamePlayersOptional = gamePlayersService.getGamePlayersAsGameManager(gameId)
        gameOptional.isPresent()
        gamePlayersOptional.isPresent()
        final def game = gameOptional.get()
        final def gamePlayers = gamePlayersOptional.get()

        and: "the game indicates that it is recruiting players"
        expect(gamePlayers.recruiting, Matchers.instanceOf(Boolean.class))

        and: "the game indicates that it has no players"
        expect(gamePlayers.users, Matchers.anEmptyMap())

        and: "the game indicates that it is not running"
        game.runState == Game.RunState.WAITING_TO_START

        and: "the list of games includes the new game"
        def creationTimes = gameService.getCreationTimesOfGamesOfScenario(scenarioId).toList()
        expect(creationTimes, Matchers.hasItem(gameId.created))
    }

    def "Only a game manager may add a game"() {
        given: "has a scenario without any games"
        def scenarioId = chooseScenario().identifier

        and: "user without the manage games role"
        def user = addUserWithAuthorities(
                EnumSet.complementOf(EnumSet.of(Authority.ROLE_MANAGE_GAMES))
        )

        when: "try to create a game for the scenario"
        def response = requestAddGame(scenarioId, user)

        then: "does not allow creating th game"
        response.andExpect(status().isForbidden())
    }

    def "End game recruitment"() {
        given: "a game is initially recruiting players"
        def scenarioId = chooseScenario().identifier
        def gameId = gameService.create(scenarioId).identifier

        and: "user has the manage games role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_MANAGE_GAMES))

        when: "try to end recruitment for the game"
        def response = requestEndRecruitment(gameId, user)

        then: "the game accepts ending recruitment"
        final def location = expectFound(response)
        location != null
        parseGamePlayersPath(location) == gameId

        and: "the game indicates that it is not recruiting players"
        final def gamePlayersOptional = gamePlayersService.getGamePlayersAsGameManager(gameId)
        gamePlayersOptional.isPresent()
        final def gamePlayers = gamePlayersOptional.get()
        !gamePlayers.recruiting
    }

    def "Only a game manager may end recruitment for a game"() {
        given: "a game is recruiting players"
        def scenarioId = chooseScenario().identifier
        def gameId = gameService.create(scenarioId).identifier

        and: "user has the player role but not the manage games role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_PLAYER))

        when: "try to end recruitment of the game"
        def response = requestEndRecruitment(gameId, user)

        then: "the game does not allow ending recruitment"
        response.andExpect(status().isForbidden())
    }

    def "Players may join a game"() {
        given: "a game is recruiting players"
        def scenarioId = chooseScenario().identifier
        def gameId = gameService.create(scenarioId).identifier

        and: "user has the player role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_PLAYER))

        when: "try to examine the game"
        def gamePlayersResponse = requestGetGamePlayers(gameId, user)
        def mayJoinResponse = requestGetMayJoinQuery(gameId, user)

        then: "provides the game"
        gamePlayersResponse.andExpect(status().isOk())
        mayJoinResponse.andExpect(status().isOk())
        def gamePlayers = expectEncodedResponse(gamePlayersResponse, GamePlayers.class)
        def mayJoin = expectEncodedResponse(mayJoinResponse, Boolean.class)

        then: "the game indicates that the user may join the game"
        mayJoin

        and: "the game indicates that the user is not playing the game"
        expect(gamePlayers.users.values(), Matchers.not(Matchers.hasItem(user.id)))
    }

    def "Join a game"() {
        given: "a game is recruiting players"
        def scenarioId = chooseScenario().identifier
        def gameId = gameService.create(scenarioId).identifier

        and: "user has the player role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_PLAYER))

        when: "the user trys to join the game"
        def response = requestJoinGame(gameId, user)

        then: "the game accepts joining"
        final def location = expectFound(response)
        location != null
        parseGamePlayersPath(location) == gameId
        def gamePlayersOptional = gamePlayersService.getGamePlayersAsGameManager(gameId)
        gamePlayersOptional.isPresent()
        def gamePlayers = gamePlayersOptional.get()

        and: "the game indicates which character the user is playing"
        expect(gamePlayers.users.values(), Matchers.hasItem(user.id))
    }

    def "Only a player may join a game"() {
        given: "a game is recruiting players"
        def scenarioId = chooseScenario().identifier
        def gameId = gameService.create(scenarioId).identifier

        and: "user has the manage games role but not the player role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_MANAGE_GAMES))

        when: "examine the game"
        def response = requestJoinGame(gameId, user)

        then: "the game indicates that the user may not join the game"
        response.andExpect(status().isForbidden())
    }

    def "Start game"() {
        given: "a game is waiting to start"
        def scenarioId = chooseScenario().identifier
        def gameId = gameService.create(scenarioId).identifier

        and: "user has the manage games role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_MANAGE_GAMES))

        when: "user tries to start the game"
        def response = requestStartGame(gameId, user)

        then: "the game accepts starting"
        final def location = expectFound(response)
        location != null
        parseGamePath(location) == gameId

        and: "the game indicates that it is running"
        def gameOptional = gameService.getGame(gameId)
        gameOptional.isPresent()
        def game = gameOptional.get()
        game.runState == Game.RunState.RUNNING
    }

    def "Only a game manager may start a game"() {
        given: "a game is waiting to start"
        def scenarioId = chooseScenario().identifier
        def gameId = gameService.create(scenarioId).identifier

        and: "logged in as a user has the player role but not the manage games role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_PLAYER))

        when: "user tries to start the game"
        def response = requestStartGame(gameId, user)

        then: "the game does not allow starting"
        response.andExpect(status().isForbidden())
    }

    def "Stop game"() {
        given: "a game is running"
        def scenarioId = chooseScenario().identifier
        def gameId = gameService.create(scenarioId).identifier
        gameService.startGame(gameId)

        and: "user has the manage games role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_MANAGE_GAMES))

        when: "user tries to stop the game"
        def response = requestStopGame(gameId, user)

        then: "the game accepts stopping"
        final def location = expectFound(response)
        location != null
        parseGamePath(location) == gameId

        and: "the game indicates that it is not running"
        def gameOptional = gameService.getGame(gameId)
        gameOptional.isPresent()
        def game = gameOptional.get()
        game.runState == Game.RunState.STOPPED
    }

    def "Only a game manager may stop a game"() {
        given: "a game is running"
        def scenarioId = chooseScenario().identifier
        def gameId = gameService.create(scenarioId).identifier
        gameService.startGame(gameId)

        and: "user has the player role but not the manage games role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_PLAYER))

        when: "user tries to stop the game"
        def response = requestStopGame(gameId, user)

        then: "the game does not allow stopping"
        response.andExpect(status().isForbidden())
    }
}