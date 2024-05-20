package uk.badamson.mc
/**
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

/**
 * Mission Command is a multi-player game. To conserve resources, play on a server is restricted to only known (and presumably trusted) users.
 */
class UserSpec extends ITSpecification {

  void setupSpec() {
    specificationName = 'UserSpec'
  }

  def "Login"() {
    given: "user with any role"
    def user = createUserWithRoles(Set.of(Authority.ROLE_PLAYER), Set.of())

    and: "not logged in"
    def homePage = navigateToHomePage()

    when: "log in using correct password"
    final var loginPage = homePage.navigateToLoginPage()
    loginPage.submitLoginForm(user.getUsername(), user.getPassword())
    homePage.awaitIsReady()

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
    def user = createUserWithRoles(Set.of(Authority.ROLE_PLAYER), Set.of())
    def homePage = navigateToHomePage()
    def loginPage = homePage.navigateToLoginPage()
    loginPage.submitLoginForm(user.getUsername(), user.getPassword())
    homePage.awaitIsReady()

    when: "request logout"
    homePage.logout()

    then: "accepts the logout"
    homePage.assertInvariants()
    homePage.assertNoErrorMessages()
    homePage.assertReportsThatNotLoggedIn()
  }

  def "Login denied"() {
    given: "unknown user"
    def user = currentUserIsUnknownUser()

    when: "try to login"
    final var loginPage = navigateToHomePage().navigateToLoginPage()
    loginPage.submitLoginForm(user.getUsername(), user.getPassword())
    loginPage.awaitIsReadyAndErrorMessage()

    then: "rejects the login"
    loginPage.assertRejectedLogin()
  }
}