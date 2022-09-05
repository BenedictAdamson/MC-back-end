package uk.badamson.mc.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.util.UriTemplate
import spock.lang.Specification
import uk.badamson.mc.*
import uk.badamson.mc.presentation.GameController
import uk.badamson.mc.repository.UserSpringRepository
import uk.badamson.mc.rest.Paths
import uk.badamson.mc.rest.UserDetailsRequest
import uk.badamson.mc.spring.SpringUser

import javax.annotation.Nonnull
import javax.annotation.Nullable

import static org.hamcrest.MatcherAssert.assertThat
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
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

@SpringBootTest(classes = TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
abstract class BESpecification extends Specification {

    private static final UriTemplate GAME_PATH_URI_TEMPLATE = new UriTemplate(
            Paths.GAME_PATH_PATTERN)

    private static final UriTemplate USER_PATH_TEMPLATE = new UriTemplate('/api/user/{id}')

    private static int nUsers

    @Autowired
    protected ScenarioSpringService scenarioService

    @Autowired
    protected GameSpringService gameService

    @Autowired
    protected UserSpringService userService

    @Autowired
    protected UserSpringRepository userRepository

    @Autowired
    protected ObjectMapper objectMapper

    @Autowired
    protected WebApplicationContext context

    protected MockMvc mockMvc

    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity()).build()
        userRepository.deleteAll()
    }

    protected final UUID chooseScenario() {
        final def scenarioId = getAScenarioId()
        def scenarioOptional = scenarioService.getScenario(scenarioId)
        assertThat("scenario", scenarioOptional.isPresent())
        scenarioId
    }

    protected final UUID getAScenarioId() {
        def scenarioIdOptional = scenarioService.getScenarioIdentifiers().findAny()
        assertThat("scenarioId", scenarioIdOptional.isPresent())
        scenarioIdOptional.get()
    }

    protected final User addUserWithAuthorities(final Set<Authority> authorities) {
        ++nUsers
        userService.add(new BasicUserDetails("Zoe-${nUsers}", 'password1', authorities,
                true, true, true, true))
    }

    protected static UUID parseGamePath(final String path) {
        Objects.requireNonNull(path, "path")
        final var pathVariable = GAME_PATH_URI_TEMPLATE.match(path)
        try {
            return UUID.fromString(pathVariable.get("game"))
        } catch (final RuntimeException e) {
            throw new IllegalArgumentException("Path " + path, e)
        }
    }

    protected static UUID parseUserPath(final String path) {
        final var pathVariable = USER_PATH_TEMPLATE.match(path)
        UUID.fromString(pathVariable.get('id'))
    }

    protected final ResultActions requestGetCurrentGame(@Nullable User loggedInUser) {
        requestGetJson(Paths.CURRENT_GAME_PATH, loggedInUser)
    }


    protected final ResultActions requestGetGame(@Nonnull UUID gameId, @Nullable User loggedInUser) {
        requestGetJson(Paths.createPathForGame(gameId), loggedInUser)
    }

    protected final ResultActions requestGetMayJoinQuery(@Nonnull UUID gameId, @Nullable User loggedInUser) {
        requestGetJson(GameController.createPathForMayJoinQueryOf(gameId), loggedInUser)
    }

    protected final ResultActions requestGetScenarios(@Nullable User loggedInUser) {
        requestGetJson(Paths.SCENARIOS_PATH, loggedInUser)
    }

    protected final ResultActions requestGetScenario(@Nonnull UUID scenarioId, @Nullable User loggedInUser) {
        requestGetJson(Paths.createPathForScenario(scenarioId), loggedInUser)
    }

    protected final ResultActions requestGetGamesOfScenario(@Nonnull UUID scenarioId, @Nullable User loggedInUser) {
        requestGetJson(Paths.createPathForGamesOfScenario(scenarioId), loggedInUser)
    }

    protected final ResultActions requestGetUsers(@Nullable User loggedInUser) {
        requestGetJson('/api/user', loggedInUser)
    }

    protected final ResultActions requestGetUser(@Nonnull UUID userId, @Nullable User loggedInUser) {
        requestGetJson(Paths.createPathForUser(userId), loggedInUser)
    }

    private ResultActions requestGetJson(@Nonnull String path, @Nullable User loggedInUser) {
        var request = get(path).accept(MediaType.APPLICATION_JSON)
        if (loggedInUser != null) {
            request = request.with(user(SpringUser.convertToSpring(loggedInUser))).with(csrf())
        }
        mockMvc.perform(request)
    }

    protected final ResultActions requestJoinGame(@Nonnull UUID gameId, @Nullable User loggedInUser) {
        final def path = GameController.createPathForJoining(gameId)
        var request = post(path).accept(MediaType.APPLICATION_JSON)
        if (loggedInUser != null) {
            request = request.with(user(SpringUser.convertToSpring(loggedInUser))).with(csrf())
        }
        mockMvc.perform(request)
    }

    protected final ResultActions requestStartGame(@Nonnull UUID gameId, @Nullable User loggedInUser) {
        final def path = GameController.createPathForStarting(gameId)
        var request = post(path).accept(MediaType.APPLICATION_JSON)
        if (loggedInUser != null) {
            request = request.with(user(SpringUser.convertToSpring(loggedInUser))).with(csrf())
        }
        mockMvc.perform(request)
    }

    protected final ResultActions requestStopGame(@Nonnull UUID gameId, @Nullable User loggedInUser) {
        final def path = GameController.createPathForStopping(gameId)
        var request = post(path).accept(MediaType.APPLICATION_JSON)
        if (loggedInUser != null) {
            request = request.with(user(SpringUser.convertToSpring(loggedInUser))).with(csrf())
        }
        mockMvc.perform(request)
    }

    protected final ResultActions requestAddGame(@Nonnull UUID scenarioId, @Nullable User loggedInUser) {
        final def path = Paths.createPathForGamesOfScenario(scenarioId)
        def request = post(path).accept(MediaType.APPLICATION_JSON)
        if (loggedInUser != null) {
            request = request.with(user(SpringUser.convertToSpring(loggedInUser))).with(csrf())
        }
        mockMvc.perform(request)
    }

    protected final ResultActions requestEndRecruitment(@Nonnull UUID gameId, @Nullable User loggedInUser) {
        final def path = GameController.createPathForEndRecruitmentOf(gameId)
        def request = post(path).accept(MediaType.APPLICATION_JSON)
        if (loggedInUser != null) {
            request = request.with(user(SpringUser.convertToSpring(loggedInUser))).with(csrf())
        }
        mockMvc.perform(request)
    }

    protected final ResultActions requestLogin(@Nonnull String username, @Nonnull String password) {
        def request = post('/login')
                .param('username', username).param('password', password)
                .with(csrf())
        mockMvc.perform(request)
    }

    protected final ResultActions requestAddUser(@Nonnull UserDetailsRequest userDetails, @Nullable User loggedInUser) {
        final var encoded = objectMapper.writeValueAsString(userDetails)
        def request = post('/api/user')
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(encoded)
        if (loggedInUser != null) {
            request = request.with(user(SpringUser.convertToSpring(loggedInUser))).with(csrf())
        }
        mockMvc.perform(request)
    }

    protected final <TYPE> TYPE expectEncodedResponse(final ResultActions response,
                                                      final Class<? extends TYPE> clazz) {
        response.andExpect(status().isOk())
        final def responseText = response.andReturn().getResponse().getContentAsString()
        objectMapper.readValue(responseText, clazz)
    }

    protected final <TYPE> TYPE expectEncodedResponse(final ResultActions response,
                                                      final TypeReference<? extends TYPE> type) {
        response.andExpect(status().isOk())
        final def responseText = response.andReturn().getResponse().getContentAsString()
        objectMapper.readValue(responseText, type)
    }

    protected final String expectFound(ResultActions response) {
        response.andExpect(status().isFound())
        response.andReturn().getResponse().getHeader("Location")
    }

    protected final String expectTemporaryRedirect(ResultActions response) {
        response.andExpect(status().isTemporaryRedirect())
        response.andReturn().getResponse().getHeader("Location")
    }
}
