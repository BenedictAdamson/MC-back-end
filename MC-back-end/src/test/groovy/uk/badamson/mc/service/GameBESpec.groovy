package uk.badamson.mc.service

import org.mockserver.matchers.Times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import uk.badamson.mc.*

import java.time.Instant

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
class GameBESpec {

  @Autowired
  private BackEndWorld world;

  def "Examine game as player"() {
    given: "has a game"
    hasAGameWaitingToStart()
    world.backEnd.mockMayJoinGame(GAME_ID, false)

    and: "logged in as a user with the player role"
    def homePage = world.logInAsUserWithTheRole(Authority.ROLE_PLAYER)

    when: "examine the game"
    def gamePage = examineGame(homePage)

    then: "the game includes the scenario title"
    gamePage.assertInvariants()
    gamePage.assertIncludesScenarioTitle()

    and: "the game includes the scenario description"
    // TODO

    and: "the game includes the date and time that the game was set up"
    gamePage.assertIncludesCreationTime()

    and: "the game indicates whether it has players"
    gamePage.assertIndicatesWhetherGameHasPlayers()

    and: "the game indicates whether it is recruiting players"
    gamePage.assertIndicatesWhetherRecruitingPlayers()

    and: "the game indicates whether the user may join the game"
    gamePage.assertIndicatesWhetherUserMayJoinGame()

    and: "the game indicates whether the user is playing the game"
    gamePage.assertIndicatesWhetherUserIsPlayingGame()

    and: "the game indicates whether it is running"
    gamePage.assertIndicatesWhetherRunning()

    and: "the game indicates which character (if any) the user is playing"
    gamePage.assertIndicatesWhichCharacterIfAnyUserIsPlaying()

    and: "the game does not indicate which characters are played by which (other) users"
    gamePage.assertDoesNotIndicateWhichCharactersPlayedByOtherUsers()
  }

  def "Examine game as game manager"() {
    given: "has a game"
    hasAGameWaitingToStart()
    world.backEnd.mockMayJoinGame(GAME_ID, false)

    and: "logged in as a user with the manage games role"
    def homePage = world.logInAsUserWithTheRole(Authority.ROLE_MANAGE_GAMES)

    when: "examine the game"
    def gamePage = examineGame(homePage)

    then: "the game includes the scenario title"
    gamePage.assertInvariants()
    gamePage.assertIncludesScenarioTitle()

    and: "the game includes the scenario description"
    // TODO

    and: "the game includes the date and time that the game was set up"
    gamePage.assertIncludesCreationTime()

    and: "the game indicates whether it has players"
    gamePage.assertIndicatesWhetherGameHasPlayers()

    and: "the game indicates whether it is recruiting players"
    gamePage.assertIndicatesWhetherRecruitingPlayers()

    and: "the game indicates whether the user may join the game"
    gamePage.assertIndicatesWhetherUserMayJoinGame()

    and: "the game indicates whether the user is playing the game"
    gamePage.assertIndicatesWhetherUserIsPlayingGame()

    and: "the game indicates whether it is running"
    gamePage.assertIndicatesWhetherRunning()

    and: "the game indicates which character (if any) the user is playing"
    gamePage.assertIndicatesWhichCharacterIfAnyUserIsPlaying()

    and: "the game indicates which characters are played by which users"
    gamePage.assertIndicatesWhichCharactersPlayedByWhichUsers()
  }

  def "Add game"() {
    given: "has a scenario without any games"
    hasAScenario()
    world.backEnd.mockGetGameCreationTimes(SCENARIO_ID, Set.of(), Times.once())

    and: "logged in as a user with the manage games role"
    def user = world.createUserWithRole(Authority.ROLE_MANAGE_GAMES)
    def homePage = world.logInAsUser(user)

    and: "selected the scenario"
    def scenarioPage0 = homePage.navigateToScenariosPage()
            .navigateToScenario(0)

    when: "creating a game for the scenario"
    world.backEnd.mockCreateGameForScenario(GAME_ID)
    world.backEnd.mockGetGameCreationTimes(SCENARIO_ID, Set.of(GAME_CREATION_TIME), Times.unlimited())
    world.backEnd.mockGetGame(GAME_WAITING_TO_START)
    world.backEnd.mockMayJoinGame(GAME_ID, true)
    world.backEnd.mockGetGamePlayers(new GamePlayers(GAME_ID, true, NO_USERS))
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

    and: "the list of games includes the new game"
    scenarioPage1.getNumberOfGamesListed() == 1
  }

  def "Only a game manager may add a game"() {
    given: "has a scenario without any games"
    hasAScenario()
    world.backEnd.mockGetGameCreationTimes(SCENARIO_ID, Set.of(), Times.once())

    and: "logged in as a user without the manage games role"
    def homePage = world.logInAsUserWithoutTheRole(Authority.ROLE_MANAGE_GAMES)

    when: "examine scenario"
    def scenarioPage = homePage.navigateToScenariosPage()
            .navigateToScenario(0)

    then: "the scenario does not allow creating a game"
    !scenarioPage.isCreateGameButtonEnabled()
  }

  def "End game recruitment"() {
    given: "a game is initially recruiting players"
    hasAGameWaitingToStart()
    world.backEnd.mockMayJoinGame(GAME_ID, false)
    world.backEnd.mockGetGamePlayers(new GamePlayers(GAME_ID, true, NO_USERS), Times.once())

    and: "logged in as a user with the manage games role"
    def homePage = world.logInAsUserWithTheRole(Authority.ROLE_MANAGE_GAMES)

    when: "user ends recruitment for the game"
    def gamePage = examineGame(homePage)
    world.backEnd.mockEndRecruitment(GAME_ID)
    world.backEnd.mockGetGamePlayers(new GamePlayers(GAME_ID, false, NO_USERS), Times.unlimited())
    gamePage.endRecruitement()

    then: "the game accepts ending recruitment"
    gamePage.assertInvariants()
    gamePage.assertNoErrorMessages()

    and: "the game indicates that it is not recruiting players"
    gamePage.assertIndicatesIsNotRecruitingPlayers()
  }

  def "Only a game manager may end recruitment for a game"() {
    given: "a game is recruiting players"
    hasAGameRecruitingPlayers()
    world.backEnd.mockMayJoinGame(GAME_ID, false)

    and: "logged in as a user has the player role but not the manage games role"
    def homePage = world.logInAsUserWithTheRole(Authority.ROLE_PLAYER)

    when: "examine the game"
    def gamePage = examineGame(homePage)

    then: "the game does not allow ending recruitment"
    !gamePage.isEndRecruitmentEnabled()
  }

  def "Players may join a game"() {
    given: "a game is recruiting players"
    hasAGameRecruitingPlayers()

    and: "user is not playing any games"
    world.backEnd.mockNoCurrentGame()
    world.backEnd.mockMayJoinGame(GAME_ID, true)

    and: "logged in as a user with the player role"
    def homePage = world.logInAsUserWithTheRole(Authority.ROLE_PLAYER)

    when: "examine the game"
    def gamePage = examineGame(homePage)

    then: "the game indicates that the user may join the game"
    gamePage.assertIndicatesUserMayJoinGame()

    and: "the game indicates that the user is not playing the game"
    gamePage.assertIndicatesUserIsNotPlayingGame()
  }

  def "Join a game"() {
    given: "a game is recruiting players"
    hasAGameWaitingToStart()
    world.backEnd.mockGetGamePlayers(new GamePlayers(GAME_ID, true, NO_USERS), Times.once())
    world.backEnd.mockMayJoinGame(GAME_ID, true)

    and: "logged in as a user with the player role"
    def user = world.createUserWithRole(Authority.ROLE_PLAYER)
    def homePage = world.logInAsUser(user)

    and: "user is not playing any games"
    world.backEnd.mockNoCurrentGame()
    world.backEnd.mockMayJoinGame(GAME_ID, true)

    and: "examining the game"
    def gamePage = examineGame(homePage)

    when: "the user joins the game"
    world.backEnd.mockJoinGame(GAME_ID)
    world.backEnd.mockGetGamePlayers(new GamePlayers(GAME_ID, true, Map.of(CHARACTER_ID, user.id)), Times.unlimited())
    gamePage.joinGame()

    then: "the game accepts joining"
    gamePage.assertInvariants()
    gamePage.assertNoErrorMessages()

    and: "the game indicates that the user is playing the game"
    gamePage.assertIndicatesUserIsPlayingGame()

    and: "the game indicates which character the user is playing"
    gamePage.assertIndicatesWhichCharacterUserIsPlaying()
  }

  def "Only a player may join a game"() {
    given: "a game is recruiting players"
    hasAGameRecruitingPlayers()
    world.backEnd.mockMayJoinGame(GAME_ID, false)

    and: "user is not playing any games"
    world.backEnd.mockNoCurrentGame()

    and: "logged in as a user with the manage games role but not the player role"
    def homePage = world.logInAsUserWithTheRole(Authority.ROLE_MANAGE_GAMES)

    when: "examine the game"
    def gamePage = examineGame(homePage)

    then: "the game indicates that the user may not join the game"
    gamePage.assertIndicatesUserMayNotJoinGame()
  }

  def "Start game"() {
    given: "a game is waiting to start"
    hasAScenario()
    world.backEnd.mockGetGameCreationTimes(SCENARIO_ID, Set.of(GAME_CREATION_TIME))
    world.backEnd.mockGetGame(new Game(GAME_ID, Game.RunState.WAITING_TO_START), Times.once())
    world.backEnd.mockGetGamePlayers(new GamePlayers(GAME_ID, true, NO_USERS))

    and: "logged in as a user with the manage games role"
    def homePage = world.logInAsUserWithTheRole(Authority.ROLE_MANAGE_GAMES)

    and: "examining the game"
    def gamePage = examineGame(homePage)

    when: "user starts the game"
    world.backEnd.mockStartGame(GAME_ID)
    world.backEnd.mockGetGame(new Game(GAME_ID, Game.RunState.RUNNING))
    gamePage.startGame()

    then: "the game accepts starting"
    gamePage.assertInvariants()
    gamePage.assertNoErrorMessages()

    and: "the game indicates that it is running"
    gamePage.assertIndicatesRunning()
  }

  def "Only a game manager may start a game"() {
    given: "a game is waiting to start"
    hasAGameWaitingToStart()

    and: "logged in as a user has the player role but not the manage games role"
    def homePage = world.logInAsUserWithTheRole(Authority.ROLE_PLAYER)

    when: "examine the game"
    def gamePage = examineGame(homePage)

    then: "the game does not allow starting"
    !gamePage.isStartingEnabled()
  }

  def "Stop game"() {
    given: "a game is running"
    hasAScenario()
    world.backEnd.mockGetGameCreationTimes(SCENARIO_ID, Set.of(GAME_CREATION_TIME))
    world.backEnd.mockGetGame(new Game(GAME_ID, Game.RunState.RUNNING), Times.once())
    world.backEnd.mockGetGamePlayers(new GamePlayers(GAME_ID, true, NO_USERS))

    and: "logged in as a user with the manage games role"
    def homePage = world.logInAsUserWithTheRole(Authority.ROLE_MANAGE_GAMES)

    and: "examine the game"
    def gamePage = examineGame(homePage)

    when: "user stops the game"
    world.backEnd.mockStopGame(GAME_ID)
    world.backEnd.mockGetGame(new Game(GAME_ID, Game.RunState.STOPPED))
    gamePage.stopGame()

    then: "the game accepts stopping"
    gamePage.assertInvariants()
    gamePage.assertNoErrorMessages()

    and: "the game indicates that it is not running"
    gamePage.assertIndicatesNotRunning()
  }

  def "Only a game manager may stop a game"() {
    given: "a game is running"
    hasAScenario()
    world.backEnd.mockGetGameCreationTimes(SCENARIO_ID, Set.of(GAME_CREATION_TIME))
    world.backEnd.mockGetGame(new Game(GAME_ID, Game.RunState.RUNNING))
    world.backEnd.mockGetGamePlayers(new GamePlayers(GAME_ID, true, NO_USERS))

    and: "user has the player role but not the manage games role"
    def homePage = world.logInAsUserWithTheRole(Authority.ROLE_PLAYER)

    when: "examine the game"
    def gamePage = examineGame(homePage)

    then: "the game does not allow stopping"
    !gamePage.isStoppingEnabled()
  }

  private void hasAGameRecruitingPlayers() {
    hasAGameWaitingToStart()
    world.backEnd.mockGetGamePlayers(new GamePlayers(GAME_ID, true, NO_USERS))
  }

  private void hasAGameWaitingToStart() {
    hasAScenario()
    world.backEnd.mockGetGameCreationTimes(SCENARIO_ID, Set.of(GAME_CREATION_TIME))
    world.backEnd.mockGetGame(GAME_WAITING_TO_START)
  }


  private void hasAScenario() {
    world.backEnd.mockGetAllScenarios(Set.of(new NamedUUID(SCENARIO_ID, SCENARIO_TITLE)))
    world.backEnd.mockGetScenario(SCENARIO)
  }

  private GamePage examineGame(final HomePage homePage) {
    def scenarioPage = homePage.navigateToScenariosPage()
            .navigateToScenario(0)
    scenarioPage.awaitIsReadyOrErrorMessage()
    scenarioPage.requireIsReady()
    def gamePage = scenarioPage.navigateToGamePage(0)
    gamePage.awaitIsReadyOrErrorMessage()
    gamePage
  }
}