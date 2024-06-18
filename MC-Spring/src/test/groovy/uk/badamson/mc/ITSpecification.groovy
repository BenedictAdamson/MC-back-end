package uk.badamson.mc

import org.openqa.selenium.Capabilities
import org.openqa.selenium.OutputType
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.springframework.http.HttpCookie
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.testcontainers.Testcontainers
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.lifecycle.TestDescription
import spock.lang.Shared
import spock.lang.Specification
import uk.badamson.mc.presentation.page.HomePage

import javax.annotation.Nonnegative
import javax.annotation.Nonnull
import javax.annotation.Nullable
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
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

abstract class ITSpecification extends Specification {

    private static final String MONGO_DB_PASSWORD = "LetMeIn1"
    private static final String ADMINISTRATOR_PASSWORD = ProcessFixtures.ADMINISTRATOR.getPassword()
    private static final Path FAILURE_RECORDING_DIRECTORY = Path.of('build', 'test-results', 'integrationTest', 'failure-records')
    private static final Capabilities CAPABILITIES = new FirefoxOptions()
            .addPreference("security.insecure_field_warning.contextual.enabled", false)

    @Shared
    private static MongoDBContainer mongoDBContainer
    @Shared
    private static McBackEndProcess mcBackEndProcess
    @Shared
    private static McBackEndClient mcBackEndClient
    @Shared
    private static BrowserWebDriverContainer browser
    @Shared
    private static RemoteWebDriver webDriver

    @Shared
    protected static String specificationName = getClass().simpleName
    @Shared
    private static int nUsers
    @Shared
    private static int nTests

    private TestDescription description = new TestDescription() {
        @Override
        String getTestId() {
            specificationName + '-' + nTests
        }

        @Override
        String getFilesystemFriendlyName() {
            getTestId()
        }
    }

    private static <TYPE> boolean intersects(final Set<TYPE> set1,
                                             final Set<TYPE> set2) {
        /* The sets intersect if we can find any element in both. */
        return set1.stream().anyMatch(set2::contains)
    }


    void setupSpec() {
        Files.createDirectories(FAILURE_RECORDING_DIRECTORY)
        mongoDBContainer = new MongoDBContainer(ProcessFixtures.MONGO_DB_IMAGE)
        mongoDBContainer.start()
        final def mongoDBPath = mongoDBContainer.getReplicaSetUrl()
        mcBackEndProcess = new McBackEndProcess(mongoDBPath, MONGO_DB_PASSWORD, ADMINISTRATOR_PASSWORD)
        def backEndPort = mcBackEndProcess.getServerPort()
        mcBackEndClient = new McBackEndClient("localhost", backEndPort)
        Testcontainers.exposeHostPorts(backEndPort)
        browser = createBrowserContainer()
        browser.start()
        webDriver = new RemoteWebDriver(browser.getSeleniumAddress(), CAPABILITIES)
    }

    void setup() {
        ++nTests
        webDriver.manage().deleteAllCookies()
        browser.beforeTest(description)
    }

    @Nonnull
    private static BrowserWebDriverContainer createBrowserContainer() {
        final var browser = new BrowserWebDriverContainer<>()
        browser.withCreateContainerCmdModifier(cmd -> Objects.requireNonNull(Objects.requireNonNull(cmd).getHostConfig())
                .withCpuCount(2L))
        browser.withCapabilities(CAPABILITIES)
        browser.withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL, FAILURE_RECORDING_DIRECTORY.toFile())
        browser
    }

    void cleanup() {
        retainScreenshot()
        browser.afterTest(description, Optional.empty())
    }


    void cleanupSpec() {
        if (webDriver != null) {
            webDriver.close()
            webDriver = null
        }
        if (browser != null) {
            browser.close()
            browser = null
        }
        if (mcBackEndProcess != null) {
            mcBackEndProcess.close()
            retainLog()
            mcBackEndProcess = null
        }
        if (mongoDBContainer != null) {
            mongoDBContainer.close()
            mongoDBContainer = null
        }
    }

    @Nonnull
    protected final UUID createGame(@Nonnull final UUID scenario) {
        Objects.requireNonNull(scenario, "scenario")

        final def cookies = login(ProcessFixtures.ADMINISTRATOR)
        WebTestClient.ResponseSpec response = null
        try {
            response = mcBackEndClient.createGameForScenario(
                    scenario,
                    ProcessFixtures.ADMINISTRATOR, cookies,
                    true, true
            )
        } finally {
            logout(ProcessFixtures.ADMINISTRATOR, cookies)
        }
        assert response != null
        response.expectStatus().isFound()
        return McBackEndClient.parseCreateGameResponse(response)
    }

    @Nonnull
    protected final Stream<uk.badamson.mc.rest.NamedUUID> getScenarios() {
        return mcBackEndClient.getAllScenarios().returnResult(uk.badamson.mc.rest.NamedUUID.class)
                .getResponseBody().toStream().map(ni -> new uk.badamson.mc.rest.NamedUUID(ni.getId(), ni.getTitle()))
    }

    User createUserWithRoles(final Set<Authority> included,
                             final Set<Authority> excluded) {
        Objects.requireNonNull(included, "included")
        Objects.requireNonNull(excluded, "excluded")
        if (intersects(included, excluded)) {
            throw new IllegalArgumentException("Contradictory role constraints")
        }

        return createUser(included)
    }

    @Nonnull
    protected final UUID addUser(@Nonnull final BasicUserDetails userDetails) {
        Objects.requireNonNull(userDetails, "userDetails")

        final var cookies = login(ProcessFixtures.ADMINISTRATOR)
        try {
            final var response = mcBackEndClient.addUser(
                    ProcessFixtures.ADMINISTRATOR, userDetails,
                    cookies, true, true)
            response.expectStatus().isFound()
            final var location = response.returnResult(Void.class)
                    .getResponseHeaders().getLocation()
            if (location == null) {
                throw new IllegalStateException("response has Location header")
            }
            return UUID.fromString(
                    McBackEndClient.USER_URI_TEMPLATE.match(location.toString()).get("id"))
        } finally {
            logout(ProcessFixtures.ADMINISTRATOR, cookies)
        }
    }

    @Nonnull
    protected final MultiValueMap<String, HttpCookie> login(
            @Nonnull final BasicUserDetails user
    ) {
        final var response = mcBackEndClient.getSelf(user)

        response.expectStatus().isOk()
        final var cookies = response.returnResult(String.class)
                .getResponseCookies()
        if (!cookies.containsKey(McBackEndClient.SESSION_COOKIE_NAME)
                || !cookies.containsKey(McBackEndClient.XSRF_TOKEN_COOKIE_NAME)) {
            throw new IllegalStateException(
                    "Cookies missing from response " + cookies)
        }
        final MultiValueMap<String, HttpCookie> result = new LinkedMultiValueMap<>()
        cookies.forEach(result::addAll)
        return result
    }

    protected final void logout(
            @Nullable final BasicUserDetails user,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies
    ) {
        final var response = mcBackEndClient.logout(user, cookies, true, true)
        response.expectStatus().is2xxSuccessful()
    }

    User currentUserIsUnknownUser() {
        return new User(UUID.randomUUID(), generateBasicUserDetails(Authority.ALL))
    }

    @Nonnull
    protected final User createUser(@Nonnull final Set<Authority> authorities) {
        final var userDetails = generateBasicUserDetails(authorities)
        final var id = addUser(userDetails)
        return new User(id, userDetails)
    }

    private static void retainScreenshot() {
        final String leafName = specificationName + "-" + nTests + ".png"
        final Path path = FAILURE_RECORDING_DIRECTORY.resolve(leafName)
        try {
            assert webDriver != null
            final var bytes = webDriver.getScreenshotAs(OutputType.BYTES)
            Files.write(path, bytes)
        } catch (final IOException e) {
            throw new RuntimeException(e)
        }
    }

    private static void retainLog() {
        final String leafName = specificationName + ".log"
        final Path path = FAILURE_RECORDING_DIRECTORY.resolve(leafName)
        try {
            Files.write(path, mcBackEndProcess.getLog().getBytes(StandardCharsets.UTF_8))
        } catch (final IOException e) {
            throw new RuntimeException(e)
        }
    }

    protected final BasicUserDetails generateBasicUserDetails(final Set<Authority> authorities) {
        final var sequenceId = ++nUsers
        final var username = "User " + sequenceId
        final var password = "password" + sequenceId
        return new BasicUserDetails(username, password, authorities,
                true, true, true, true)
    }

    final HomePage navigateToHomePage() {
        final var homePage = new HomePage(createBrowserBaseUri(mcBackEndProcess.getServerPort()), webDriver)
        homePage.get()
        homePage.awaitIsReady()
        return homePage
    }

    @Nonnull
    private static URI createBrowserBaseUri(@Nonnegative final int serverPort) {
        try {
            return new URI(
                    "http", null, "host.testcontainers.internal", serverPort,
                    null, null, null
            )
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e)
        }
    }

}