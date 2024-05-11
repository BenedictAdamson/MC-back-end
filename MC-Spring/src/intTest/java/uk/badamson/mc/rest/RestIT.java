package uk.badamson.mc.rest;
/*
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.http.HttpCookie;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.MongoDBContainer;
import uk.badamson.mc.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

public abstract class RestIT {
    private static final String MONGO_DB_PASSWORD = "LetMeIn1";
    private static final String ADMINISTRATOR_PASSWORD = ProcessFixtures.ADMINISTRATOR.getPassword();

    private static MongoDBContainer mongoDBContainer;
    private static McBackEndProcess mcBackEndProcess;
    private static McBackEndClient mcBackEndClient;

    @BeforeAll
    public static void setUp() throws TimeoutException {
        mongoDBContainer = new MongoDBContainer(ProcessFixtures.MONGO_DB_IMAGE);
        mongoDBContainer.start();
        final var mongoDBPath = mongoDBContainer.getReplicaSetUrl();
        mcBackEndProcess = new McBackEndProcess(mongoDBPath, MONGO_DB_PASSWORD, ADMINISTRATOR_PASSWORD);
        mcBackEndClient = new McBackEndClient(
                "localhost", mcBackEndProcess.getServerPort()
        );
    }

    @AfterAll
    public static void tearDown() {
        mcBackEndProcess.close();
        mongoDBContainer.close();
    }

    protected final McBackEndClient getMcBackEndClient() {
        return mcBackEndClient;
    }

    @Nonnull
    protected final UUID createGame() {
        return createGame(getAScenarioId());
    }

    @Nonnull
    protected final UUID createGame(@Nonnull final UUID scenario) {
        Objects.requireNonNull(scenario, "scenario");

        final var cookies = login(ProcessFixtures.ADMINISTRATOR);
        final WebTestClient.ResponseSpec response;
        try {
            response = mcBackEndClient.createGameForScenario(
                    scenario,
                    ProcessFixtures.ADMINISTRATOR, cookies,
                    true, true
            );
        } finally {
            logout(ProcessFixtures.ADMINISTRATOR, cookies);
        }
        response.expectStatus().isFound();
        return McBackEndClient.parseCreateGameResponse(response);
    }


    @Nonnull
    protected final UUID getAScenarioId() {
        return getScenarios().findAny().orElseThrow().getId();
    }

    @Nonnull
    protected final Stream<NamedUUID> getScenarios() {
        return mcBackEndClient.getAllScenarios().returnResult(NamedUUID.class)
                .getResponseBody().toStream().map(ni -> new NamedUUID(ni.getId(), ni.getTitle()));
    }

    protected final void userJoinsGame(
            @Nonnull UUID gameId,
            @Nonnull BasicUserDetails user,
            @Nonnull MultiValueMap<String, HttpCookie> cookies
    ) {
        final var response = mcBackEndClient.joinGame(gameId, user, cookies, true, true);
        response.expectStatus().isFound();
    }

    protected final void endRecruitment(UUID gameId) {
        final var cookies = login(ProcessFixtures.ADMINISTRATOR);
        try {
            mcBackEndClient.endRecruitment(gameId, ProcessFixtures.ADMINISTRATOR, cookies, true, true);
        } finally {
            logout(ProcessFixtures.ADMINISTRATOR, cookies);
        }
    }

    @Nonnull
    protected final UUID addUser(@Nonnull final BasicUserDetails userDetails) {
        Objects.requireNonNull(userDetails, "userDetails");

        final var cookies = login(ProcessFixtures.ADMINISTRATOR);
        try {
            final var response = mcBackEndClient.addUser(
                    ProcessFixtures.ADMINISTRATOR, userDetails,
                    cookies, true, true)
                    ;
            response.expectStatus().isFound();
            final var location = response.returnResult(Void.class)
                    .getResponseHeaders().getLocation();
            if (location == null) {
                throw new IllegalStateException("response has Location header");
            }
            return UUID.fromString(
                    McBackEndClient.USER_URI_TEMPLATE.match(location.toString()).get("id"));
        } finally {
            logout(ProcessFixtures.ADMINISTRATOR, cookies);
        }
    }

    @Nonnull
    protected final MultiValueMap<String, HttpCookie> login(
            @Nonnull final BasicUserDetails user
    ) {
        final var response = mcBackEndClient.getSelf(user);

        response.expectStatus().isOk();
        final var cookies = response.returnResult(String.class)
                .getResponseCookies();
        if (!cookies.containsKey(McBackEndClient.SESSION_COOKIE_NAME)
                || !cookies.containsKey(McBackEndClient.XSRF_TOKEN_COOKIE_NAME)) {
            throw new IllegalStateException(
                    "Cookies missing from response " + cookies);
        }
        final MultiValueMap<String, HttpCookie> result = new LinkedMultiValueMap<>();
        cookies.forEach(result::addAll);
        return result;
    }

    protected final void logout(
            @Nullable final BasicUserDetails user,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies
    ) {
        final var response = mcBackEndClient.logout(user, cookies, true, true);
        response.expectStatus().is2xxSuccessful();
    }

}
