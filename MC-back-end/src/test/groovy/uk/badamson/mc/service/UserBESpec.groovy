package uk.badamson.mc.service

import org.mockserver.matchers.Times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import uk.badamson.mc.Authority
import uk.badamson.mc.BackEndWorld
import uk.badamson.mc.BasicUserDetails
import uk.badamson.mc.TestConfiguration
import uk.badamson.mc.User

/**
 * © Copyright Benedict Adamson 2019-20,22.
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
 * Mission Command is a multi-player game. To conserve resources, play on a server is restricted to only known (and presumably trusted) users.
 */
@SpringBootTest(classes = TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class UserBESpec {

  @Autowired
  private BackEndWorld world;

  def "List users"() {
    given: "logged in as a user with the ${role} role"
    def user = world.createUserWithRole(role)
    def homePage = world.logInAsUser(user)

    when: "getting the users"
    world.backEnd.mockGetAllUsers(Set.of(user))
    def usersPage = homePage.navigateToUsersPage()

    then: "it serves the list of users"
    usersPage.assertInvariants()
    usersPage.assertHasListOfUsers()

    and: "the list of users has at least one user"
    usersPage.assertListOfUsersNotEmpty()

    where:
    role << [Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_USERS]
  }

  def "Examine user"() {
    given: "logged in as a user with the manage users role"
    def currentUser = world.createUserWithRole(Authority.ROLE_MANAGE_USERS)
    def otherUser = world.createUserWithRole(Authority.ROLE_PLAYER)
    def homePage = world.logInAsUser(currentUser)

    and: "viewing the list of users"
    world.backEnd.mockGetAllUsers(Set.of(currentUser, otherUser))
    def usersPage = homePage.navigateToUsersPage()

    when: "navigate to one user"
    def userPage = usersPage.navigateToUserPage(0)

    then: "MC serves the user"
    userPage.assertInvariants()

    and: "the user includes the user name"
    userPage.assertIncludesUserName()

    and: "the user lists the roles of the user"
    userPage.assertListsRolesOfUser()
  }

  def "Login"() {
    given: "user with any role"
    def user = world.createUserWithRole(Authority.ROLE_PLAYER)

    and: "not logged in"
    world.backEnd.mockGetSelfUnauthenticated(Times.once())
    world.backEnd.mockNoCurrentGame()
    def homePage = world.getHomePage()

    when: "log in using correct password"
    final var loginPage = homePage.navigateToLoginPage()
    world.backEnd.mockLogin(user, UUID.randomUUID().toString(), UUID.randomUUID().toString())
    world.backEnd.mockGetSelf(user)
    loginPage.submitLoginForm(user.getUsername(), user.getPassword())
    homePage.awaitIsReadyOrErrorMessage()

    then: "redirected to home-page"
    homePage.requireIsReady()
    homePage.assertInvariants()

    then: "accepts the login"
    homePage.assertNoErrorMessages()
    homePage.assertReportsThatLoggedIn()

    and: "allows logout"
    homePage.isLogoutEnabled()

    and: "allows examining the current user"
    homePage.hasExamineCurrentUserLink()
  }

  def "Logout"() {
    given: "logged in as user with any role"
    def homePage = world.logInAsUserWithTheRole(Authority.ROLE_PLAYER)

    when: "request logout"
    homePage.logout()

    then: "accepts the logout"
    homePage.assertInvariants()
    homePage.assertNoErrorMessages()
    homePage.assertReportsThatNotLoggedIn()
  }

  def "Login denied"() {
    given: "not logged in"
    world.backEnd.mockGetSelfUnauthenticated()

    and: "unknown user"
    def user = world.createUserWithRole(Authority.ROLE_PLAYER)

    when: "try to login"
    final var loginPage = world.getHomePage().navigateToLoginPage()
    loginPage.submitLoginForm(user.getUsername(), user.getPassword())
    loginPage.awaitIsReadyAndErrorMessage()

    then: "rejects the login"
    loginPage.assertRejectedLogin()
  }

  def "Add user"() {
    given: "logged in as a user with the manage users role"
    world.backEnd.mockNoCurrentGame()
    def user = world.createUserWithRole(Authority.ROLE_MANAGE_USERS)
    def homePage = world.logInAsUser(user)

    and: 'on the users page'
    world.backEnd.mockGetAllUsers(Set.of(user), Times.once())
    def usersPage = homePage.navigateToUsersPage()

    when: "adding a user named ${userName} with password ${password}"
    def newBasicUserDetails = new BasicUserDetails(userName, password, Set.of(Authority.ROLE_PLAYER),
            false, false, false, true)
    def newUserId = UUID.randomUUID()
    def newUser = new User(newUserId, newBasicUserDetails)
    world.backEnd.mockAddUser(newBasicUserDetails, newUserId)
    world.backEnd.mockGetAllUsers(Set.of(user, newUser))
    world.backEnd.mockGetUser(newUser)
    usersPage.navigateToAddUserPage().submitForm(userName, password)

    then: "accepts the addition"
    usersPage.awaitIsReady()

    and: "can get the list of users"
    usersPage.assertInvariants()

    and: "the list of users includes a user named ${userName}"
    usersPage.assertListOfUsersIncludes(userName)

    where:
    userName | password
    'John' | 'secret'
    'Jeff' | 'password123'
  }

  def "Only administrator may add user"() {
    given: "logged in as a user with the player role but not the manage users role"
    world.backEnd.mockNoCurrentGame()
    def user = world.createUserWithRole(Authority.ROLE_PLAYER)
    world.backEnd.mockGetAllUsers(Set.of(user))
    def homePage = world.logInAsUser(user)

    when: "examining the users page"
    final var usersPage = homePage.navigateToUsersPage()

    then: "does not allow adding a user"
    !usersPage.hasAddUserLink()
  }

  def "Only administrator my examine user"() {
    given: "logged in as user with the player role but not the manage users role"
    world.backEnd.mockNoCurrentGame()
    def user = world.createUserWithRole(Authority.ROLE_PLAYER)
    world.backEnd.mockGetAllUsers(Set.of(user))
    def homePage = world.logInAsUser(user)

    when: "viewing the list of users"
    final var usersPage = homePage.navigateToUsersPage()

    then: "does not allow examining any users"
    usersPage.getNumberOfUserLinks() == 0
  }
}