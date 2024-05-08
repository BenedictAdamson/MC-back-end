package uk.badamson.mc;
/*
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

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.HttpCookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.RequestHeadersSpec;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriTemplate;
import uk.badamson.mc.rest.Paths;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public final class McBackEndClient {

    static final String SESSION_COOKIE_NAME = "JSESSIONID";

    static final UriTemplate USER_URI_TEMPLATE = new UriTemplate(Paths.USER_PATH_PATTERN);

    private static final UriTemplate GAME_URI_TEMPLATE = new UriTemplate(Paths.GAME_PATH_PATTERN);

    private static final String XSRF_TOKEN_COOKIE_NAME = "XSRF-TOKEN";
    private static final String SCHEME = "http";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Nonnull
    private final String baseUrl;
    @Nonnull
    private final User administrator;

    public McBackEndClient(
            @Nonnull final String host,
            @Nonnegative final int port,
            @Nonnull final String administratorPassword
    ) {
        Objects.requireNonNull(administratorPassword);
        this.baseUrl = createBaseUrl(host, port);
        this.administrator = User.createAdministrator(administratorPassword);
    }

    @Nonnull
    private static String createBaseUrl(
            @Nonnull final String host,
            @Nonnegative final int port
    ) {
        Objects.requireNonNull(host);
        try {
            return new URI(SCHEME, null, host, port, "", null, null).toString();
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String encodeAsJson(final Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (final Exception e) {
            throw new IllegalArgumentException("can not encode Object as JSON", e);
        }
    }

    @SuppressFBWarnings(value = "DCN_NULLPOINTER_EXCEPTION", justification = "exception translation")
    private static UUID parseCreateGameResponse(final ResponseSpec response) {
        Objects.requireNonNull(response, "response");
        try {
            final var location = response.returnResult(String.class)
                    .getResponseHeaders().getLocation();
            Objects.requireNonNull(location, "Has Location header");
            final var uriComponents = GAME_URI_TEMPLATE.match(location.getPath());
            return UUID.fromString(uriComponents.get("game"));
        } catch (final NullPointerException e) {
            throw new IllegalArgumentException("Invalid response", e);
        }
    }

    public static void secure(
            @Nonnull final RequestHeadersSpec<?> request,
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(cookies, "cookies");
        final var sessionCookie = includeSessionCookie ?
                Objects.requireNonNull(cookies.getFirst(SESSION_COOKIE_NAME))
                : null;
        final var xsrfCookie = includeXsrfToken ?
                Objects.requireNonNull(cookies.getFirst(XSRF_TOKEN_COOKIE_NAME))
                : null;
        secure(request, authenticatingUser, sessionCookie, xsrfCookie);
    }


    static void secure(
            @Nonnull final RequestHeadersSpec<?> request,
            @Nullable final BasicUserDetails authenticatingUser,
            @Nullable final HttpCookie sessionCookie,
            @Nullable final HttpCookie xsrfCookie
    ) {
        Objects.requireNonNull(request, "request");

        if (authenticatingUser != null) {
            request.headers(headers -> headers.setBasicAuth(authenticatingUser.getUsername(), authenticatingUser.getPassword()));
        }
        if (xsrfCookie != null) {
            var value = xsrfCookie.getValue();
            request.headers(headers -> headers.add("X-XSRF-TOKEN", value));
            request.cookie(xsrfCookie.getName(), value);
        }
        if (sessionCookie != null) {
            request.cookie(sessionCookie.getName(), sessionCookie.getValue());
        }
    }

    public WebTestClient.ResponseSpec addUser(
            @Nonnull final BasicUserDetails loggedInUser,
            @Nonnull final BasicUserDetails addingUserDetails,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken
    ) {
        final var headers = connectWebTestClient().post().uri("/api/user")
                .contentType(MediaType.APPLICATION_JSON);
        McBackEndClient.secure(headers, loggedInUser, cookies, includeSessionCookie, includeXsrfToken);
        final var request = headers.bodyValue(McBackEndClient.encodeAsJson(addingUserDetails));
        return request.exchange();
    }

    public UUID addUser(final BasicUserDetails userDetails) {
        Objects.requireNonNull(userDetails, "userDetails");

        final var cookies = login(administrator);
        try {
            final var response = addUser(administrator, userDetails, cookies, true, true);
            response.expectStatus().isFound();
            final var location = response.returnResult(Void.class)
                    .getResponseHeaders().getLocation();
            if (location == null) {
                throw new IllegalStateException("response has Location header");
            }
            return UUID.fromString(
                    USER_URI_TEMPLATE.match(location.toString()).get("id"));
        } finally {
            logout(administrator, cookies);
        }
    }

    @Nonnull
    public WebTestClient connectWebTestClient() {
        return WebTestClient.bindToServer()
                .baseUrl(baseUrl)
                .build();
    }

    public WebTestClient.ResponseSpec getUser(
            @Nonnull final UUID id,
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var path = Paths.createPathForUser(id);
        final var request = connectWebTestClient()
                .get().uri(path)
                .accept(MediaType.APPLICATION_JSON);
        McBackEndClient.secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        return request.exchange();
    }

    public UUID createGame(final UUID scenario) {
        Objects.requireNonNull(scenario, "scenario");

        final var cookies = login(administrator);
        final WebTestClient.ResponseSpec response;
        try {
            final var path = Paths.createPathForGamesOfScenario(scenario);
            final var request = connectWebTestClient().post().uri(path)
                    .accept(MediaType.APPLICATION_JSON);
            secure(request, administrator, cookies, true, true);
            response = request.exchange();
        } finally {
            logout(administrator, cookies);
        }
        response.expectStatus().isFound();
        return parseCreateGameResponse(response);
    }

    public Stream<NamedUUID> getScenarios() {
        return getAllScenarios().returnResult(uk.badamson.mc.rest.NamedUUID.class)
                .getResponseBody().toStream().map(ni -> new NamedUUID(ni.getId(), ni.getTitle()));
    }

    @Nonnull
    public MultiValueMap<String, HttpCookie> login(final BasicUserDetails user) {
        final var response = getSelf(user);

        response.expectStatus().isOk();
        final var cookies = response.returnResult(String.class)
                .getResponseCookies();
        if (!cookies.containsKey(SESSION_COOKIE_NAME)
                || !cookies.containsKey(XSRF_TOKEN_COOKIE_NAME)) {
            throw new IllegalStateException(
                    "Cookies missing from response " + cookies);
        }
        final MultiValueMap<String, HttpCookie> result = new LinkedMultiValueMap<>();
        cookies.forEach(result::addAll);
        return result;
    }

    @Nonnull
    public ResponseSpec getSelf(@Nonnull BasicUserDetails user) {
        return connectWebTestClient().get().uri("/api/self")
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBasicAuth(user.getUsername(), user.getPassword()))
                .exchange();
    }

    public WebTestClient.ResponseSpec logout(
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken
    ) {
        final var request = connectWebTestClient().post().uri("/logout");
        McBackEndClient.secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        return request.exchange();
    }

    public void logout(
            @Nullable final BasicUserDetails user,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies
    ) {
        final var response = logout(user, cookies, true, true);
        response.expectStatus().is2xxSuccessful();
    }

    @Nonnull
    public WebTestClient.ResponseSpec joinGame(
            @Nonnull final UUID gameId,
            @Nonnull final BasicUserDetails user,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeAuthentication,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var path = Paths.createPathForJoiningGame(gameId);
        final var request = connectWebTestClient().post()
                .uri(path)
                .accept(MediaType.APPLICATION_JSON);
        McBackEndClient.secure(
                request, includeAuthentication ? user : null,
                cookies,
                includeSessionCookie, includeXsrfToken
        );
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec getCurrentGame(
            @Nonnull final BasicUserDetails user,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeAuthentication,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var request = connectWebTestClient().get().uri(Paths.CURRENT_GAME_PATH)
                .accept(MediaType.APPLICATION_JSON);
        McBackEndClient.secure(
                request, includeAuthentication ? user : null,
                cookies,
                includeSessionCookie, includeXsrfToken
        );
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec endRecruitment(
            @Nonnull final UUID gameId,
            @Nonnull final BasicUserDetails user,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeAuthentication,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var path = Paths.createPathForEndRecruitmentOfGame(gameId);
        final var request = connectWebTestClient().post().uri(path)
                .accept(MediaType.APPLICATION_JSON);
        McBackEndClient.secure(
                request, includeAuthentication ? user : null,
                cookies,
                includeSessionCookie, includeXsrfToken
        );
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec startGame(
            @Nonnull final UUID gameId,
            @Nonnull final BasicUserDetails user,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeAuthentication,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var path = Paths.createPathForStartingGame(gameId);
        final var request = connectWebTestClient().post().uri(path)
                .accept(MediaType.APPLICATION_JSON);
        McBackEndClient.secure(
                request, includeAuthentication ? user : null,
                cookies,
                includeSessionCookie, includeXsrfToken
        );
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec stopGame(
            @Nonnull final UUID gameId,
            @Nonnull final BasicUserDetails user,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeAuthentication,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var path = Paths.createPathForStoppingGame(gameId);
        final var request = connectWebTestClient().post().uri(path)
                .accept(MediaType.APPLICATION_JSON);
        McBackEndClient.secure(
                request, includeAuthentication ? user : null,
                cookies,
                includeSessionCookie, includeXsrfToken
        );
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec createGameForScenario(
            @Nonnull final UUID scenarioId,
            @Nonnull final BasicUserDetails user,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeAuthentication,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var path = Paths.createPathForGamesOfScenario(scenarioId);
        final var request = connectWebTestClient().post().uri(path)
                .accept(MediaType.APPLICATION_JSON);
        McBackEndClient.secure(
                request, includeAuthentication? user: null,
                cookies,
                includeSessionCookie, includeXsrfToken
        );
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec getGame(
            @Nonnull final UUID gameId,
            @Nonnull final BasicUserDetails user,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeAuthentication,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var path = Paths.createPathForGame(gameId);
        final var request = connectWebTestClient().get().uri(path)
                .accept(MediaType.APPLICATION_JSON);
        McBackEndClient.secure(
                request, includeAuthentication ? user : null,
                cookies,
                includeSessionCookie, includeXsrfToken
        );
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec getGamesOfScenario(
            @Nonnull final UUID scenarioId,
            @Nonnull final BasicUserDetails user,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeAuthentication,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken
    ) {
        final var path = Paths.createPathForGamesOfScenario(scenarioId);
        final var request = connectWebTestClient().get().uri(path)
                .accept(MediaType.APPLICATION_JSON);
        McBackEndClient.secure(
                request, includeAuthentication ? user : null,
                cookies,
                includeSessionCookie, includeXsrfToken
        );
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec mayJoin(
            @Nonnull final UUID gameId,
            @Nonnull final BasicUserDetails user,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeAuthentication,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken
    ) {
        final var path = Paths.createPathForMayJoinQueryOfGame(gameId);
        final var request = connectWebTestClient().get().uri(path)
                .accept(MediaType.APPLICATION_JSON);
        McBackEndClient.secure(
                request, includeAuthentication ? user : null,
                cookies,
                includeSessionCookie, includeXsrfToken
        );
        return request.exchange();
    }

    public WebTestClient.ResponseSpec getAllScenarios() {
        return connectWebTestClient()
                .get().uri("/api/scenario")
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
    }

    public WebTestClient.ResponseSpec getScenario(final UUID id) {
        return connectWebTestClient()
                .get().uri(Paths.createPathForScenario(id))
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
    }
}
