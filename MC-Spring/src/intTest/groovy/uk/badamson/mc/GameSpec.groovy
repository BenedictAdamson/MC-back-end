package uk.badamson.mc

import uk.badamson.mc.presentation.GamePage
import uk.badamson.mc.presentation.HomePage

import javax.annotation.Nonnull

/**
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

/**
 * The Mission Command game can have multiple games (plays) for each scenario
 */
class GameSpec extends ITSpecification {

    void setupSpec() {
        specificationName = 'GameSpec'
    }

    def "Add game"() {
        given: "logged in as a user with the manage games role"
        def homePage = loginAsUserWithRole(Authority.ROLE_MANAGE_GAMES)

        and: "selected the scenario"
        def scenarioPage0 = homePage.navigateToScenariosPage()
                .navigateToScenario(0)
        final int nGames0 = scenarioPage0.numberOfGamesListed

        when: "creating a game for the scenario"
        def gamePage = scenarioPage0.createGame()

        then: "accepts the creation of the game"
        gamePage.assertInvariants()
        gamePage.assertNoErrorMessages()

        and: "the game indicates that it is recruiting players"
        gamePage.assertIndicatesIsRecruitingPlayers()

        and: "the game indicates that it has no players"
        gamePage.assertIndicatesGameHasNoPlayedCharacters()

        and: "the game indicates that it is not running"
        gamePage.assertIndicatesNotRunning()

        and: "can get the list of games"
        def scenarioPage1 = gamePage.navigateToScenarioPage()
        scenarioPage1.assertHasListOfGames()

        and: "the list of games includes a new game"
        scenarioPage1.getNumberOfGamesListed() == nGames0 + 1
    }

    def "End game recruitment"() {
        given: "logged in as a user with the manage games role"
        def homePage = loginAsUserWithRole(Authority.ROLE_MANAGE_GAMES)

        and: "a game is initially recruiting players"
        def gamePage = createGame(homePage)
        gamePage.assertIndicatesIsRecruitingPlayers()

        when: "user ends recruitment for the game"
        gamePage.endRecruitement()

        then: "the game accepts ending recruitment"
        gamePage.assertInvariants()
        gamePage.assertNoErrorMessages()

        and: "the game indicates that it is not recruiting players"
        gamePage.assertIndicatesIsNotRecruitingPlayers()
    }

    def "Join a game"() {
        given: "logged in as a user with the player role not playing any games"
        def homePage = loginAsUserWithRole(Authority.ROLE_PLAYER)

        and: "a game is recruiting players"
        def gamePage = createGame(homePage)

        when: "the user joins the game"
        gamePage.joinGame()

        then: "the game accepts joining"
        gamePage.assertInvariants()
        gamePage.assertNoErrorMessages()

        and: "the game indicates that the user is playing the game"
        gamePage.assertIndicatesUserIsPlayingGame()

        and: "the game indicates which character the user is playing"
        gamePage.assertIndicatesWhichCharacterUserIsPlaying()
    }

    def "Start game"() {
        given: "logged in as a user with the manage games role"
        def homePage = loginAsUserWithRole(Authority.ROLE_MANAGE_GAMES)

        and: "examining a game is waiting to start"
        def gamePage = createGame(homePage)
        gamePage.assertIndicatesNotRunning()

        when: "user starts the game"
        gamePage.startGame()

        then: "the game accepts starting"
        gamePage.assertInvariants()
        gamePage.assertNoErrorMessages()

        and: "the game indicates that it is running"
        gamePage.assertIndicatesRunning()
    }

    private HomePage loginAsUserWithRole(@Nonnull final Authority role) {
        def user = createUserWithRoles(Set.of(role), Set.of())
        def homePage = navigateToHomePage()
        final var loginPage = homePage.navigateToLoginPage()
        loginPage.submitLoginForm(user.getUsername(), user.getPassword())
        homePage.awaitIsReady()
        homePage
    }

    private GamePage createGame(@Nonnull HomePage homePage) {
        final var scenario = getScenarios().findFirst().get().getId()
        final var scenarioIndex = 0
        final var gameId = createGame(scenario)
        final def scenarioPage = homePage.navigateToScenariosPage()
                .navigateToScenario(scenarioIndex)
        scenarioPage.navigateToGamePage(gameId)
    }
}