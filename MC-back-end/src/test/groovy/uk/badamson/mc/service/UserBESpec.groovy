package uk.badamson.mc.service

import com.fasterxml.jackson.core.type.TypeReference
import org.hamcrest.Matchers
import org.springframework.boot.test.context.SpringBootTest
import uk.badamson.mc.TestConfiguration
import uk.badamson.mc.User
import uk.badamson.mc.Authority
import uk.badamson.mc.rest.AuthorityValue
import uk.badamson.mc.rest.UserDetailsRequest
import uk.badamson.mc.rest.UserResponse
import uk.badamson.mc.spring.SpringUser
import uk.badamson.mc.spring.SpringUserDetails

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static spock.util.matcher.HamcrestSupport.expect
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
class UserBESpec extends BESpecification {

    def "List users"() {
        given: "user has the #role"
        def user = addUserWithAuthorities(EnumSet.of(role))

        when: "try to get the list of users"
        def response = requestGetUsers(user)

        then: "it provides the list of users"
        def users = expectEncodedResponse(response, new TypeReference<List<User>>() {})

        and: "the list of users has at least one user"
        expect(users, Matchers.not(Matchers.empty()))

        where:
        role << [Authority.ROLE_PLAYER, Authority.ROLE_MANAGE_USERS]
    }

    def "Examine user"() {
        given: "there is a user to examine"
        def userToExamine = addUserWithAuthorities(EnumSet.of(Authority.ROLE_MANAGE_USERS))

        and: "current user has the manage users role"
        def currentUser = addUserWithAuthorities(EnumSet.of(Authority.ROLE_MANAGE_USERS))

        when: "try to examine the user"
        def response = requestGetUser(userToExamine.id, currentUser)

        then: "it provides the user"
        def fetchedUser = expectEncodedResponse(response, UserResponse.class)

        and: "the user includes the user name"
        fetchedUser.username() == userToExamine.username

        and: "the user lists the roles of the user"
        fetchedUser.authorities() == AuthorityValue.convertToValue(userToExamine.authorities)
    }

    def "Add user"() {
        given: "current user has the manage users role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_MANAGE_USERS))

        when: "try to add a user named #userName with password #password"
        def requestBody = new UserDetailsRequest(userName, password, Set.of(AuthorityValue.ROLE_PLAYER),
                false, false, false, true)
        def response = requestAddUser(requestBody, user)

        then: "accepts the addition"
        def location = expectFound(response)
        parseUserPath(location) != null

        and:
        "the list of users includes a user named ${userName}"
        userService.getUsers().anyMatch(u -> u.username == userName)

        where:
        userName | password
        'John'   | 'secret'
        'Jeff'   | 'password123'
    }

    def "Only administrator may add user"() {
        given: "current user has the player role but not the manage users role"
        def user = addUserWithAuthorities(EnumSet.of(Authority.ROLE_PLAYER))

        when: "try to add a user"
        def requestBody = new UserDetailsRequest('Zoe', 'password',
                Set.of(AuthorityValue.ROLE_PLAYER),
                false, false, false, true)
        def response = requestAddUser(requestBody, user)

        then: "does not allow adding a user"
        response.andExpect(status().isForbidden())
    }

    def "Only administrator my examine user"() {
        given: "current user has the player role but not the manage users role"
        def currentUser = addUserWithAuthorities(EnumSet.of(Authority.ROLE_PLAYER))

        and: "there is a user to examine"
        def userToExamine = addUserWithAuthorities(EnumSet.of(Authority.ROLE_PLAYER))

        when: "try to examine the user"
        final def response = requestGetUser(userToExamine.id, currentUser)

        then: "does not allow examining any users"
        response.andExpect(status().isForbidden())
    }
}