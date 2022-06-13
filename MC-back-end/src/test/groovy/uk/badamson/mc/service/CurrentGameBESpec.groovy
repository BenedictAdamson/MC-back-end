package uk.badamson.mc.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import uk.badamson.mc.*

import java.time.Instant

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
class CurrentGameBESpec {

  @Autowired
  private BackEndWorld world;

  def "May examine current-game only if playing"() {
    given: "has a game"
    hasAGame()

    and: "user is not playing any games"
    world.backEnd.mockNoCurrentGame()

    when: "logged in as a user with the player role"
    def homePage = world.logInAsUserWithTheRole(Authority.ROLE_PLAYER)

    then: "does not indicate that the user has a current game"
    !homePage.doesIndicateUserHasCurrentGame()
  }

  def "Examine current game"() {
    given: "has a game"
    hasAGame()

    and: "user is playing the game"
    def user = world.createUserWithRole(Authority.ROLE_PLAYER)
    world.backEnd.mockCurrentGame(GAME_ID)
    world.backEnd.mockGetGamePlayers(new GamePlayers(GAME_ID, true, Map.of(CHARACTER_ID, user.id)))

    when: "logged in as a user with the player role"
    def homePage = world.logInAsUser(user)

    then: "indicates that the user has a current game"
    homePage.doesIndicateUserHasCurrentGame()

    when: "examine the current game"
    def gamePage = homePage.navigateToCurrentGamePage()

    then: "the game indicates which character the user is playing"
    gamePage.assertInvariants()
    gamePage.assertIndicatesWhichCharacterUserIsPlaying()
  }

  private void hasAGame() {
    world.backEnd.mockGetAllScenarios(Set.of(new NamedUUID(SCENARIO_ID, SCENARIO_TITLE)))
    world.backEnd.mockGetScenario(SCENARIO)
    world.backEnd.mockGetGameCreationTimes(SCENARIO_ID, Set.of(GAME_CREATION_TIME))
    world.backEnd.mockGetGame(GAME_WAITING_TO_START)
  }


}
