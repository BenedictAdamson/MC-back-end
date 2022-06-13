package uk.badamson.mc.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import uk.badamson.mc.BackEndWorld
import uk.badamson.mc.NamedUUID
import uk.badamson.mc.TestConfiguration

/**
 * © Copyright Benedict Adamson 2019-22.
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
 * It should be easy for users to access the home-page of an MC server, and know they have got the right page.
 */
@SpringBootTest(classes = TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class HomePageBESpec {

    @Autowired
    private BackEndWorld world;

    def "Potential user accesses an MC server using a simple URL with the root path"() {
        given: "the DNS name, example.com, of an MC server"
        // do nothing
        and: "not logged in"
        world.notLoggedIn()
        and: "not resuming a session"
        // do nothing
        when: "the potential user gives the obvious URL https://example.com/ to a web browser"
        world.backEnd.mockGetAllScenarios(Set.of(new NamedUUID(UUID.randomUUID(), 'Squad assault')))
        def homePage = world.getHomePage()
        then: "MC serves the home page"
        homePage.assertInvariants()
        and: "the home page title includes the name of the game"
        homePage.assertTitleIncludesNameOfGame()
        and: "the home page header includes the name of the game"
        homePage.assertHeadingIncludesNameOfGame()
        and: "MC allows logging in"
        homePage.isLoginEnabled()
        and: "MC does not allow logout"
        !homePage.isLogoutButtonEnabled()
        and: "MC does not allow examining the current user"
        !homePage.hasExamineCurrentUserLink()
        and: "MC does not allow listing users"
        !homePage.hasUsersLink()
        and: "it does not indicate that the user has a current game"
        !homePage.doesIndicateUserHasCurrentGame()
    }
}