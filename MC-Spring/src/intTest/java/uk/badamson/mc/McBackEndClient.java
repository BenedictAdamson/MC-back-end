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

public final class McBackEndClient {

    public static final String SESSION_COOKIE_NAME = "JSESSIONID";
    public static final String XSRF_TOKEN_COOKIE_NAME = "XSRF-TOKEN";

    public static final UriTemplate USER_URI_TEMPLATE = new UriTemplate(Paths.USER_PATH_PATTERN);

    private static final UriTemplate GAME_URI_TEMPLATE = new UriTemplate(Paths.GAME_PATH_PATTERN);

    private static final String SCHEME = "http";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Nonnull
    private final String baseUrl;

    public McBackEndClient(
            @Nonnull final String host,
            @Nonnegative final int port
    ) {
        this.baseUrl = createBaseUrl(host, port);
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

    private static String encodeAsJson(final Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (final Exception e) {
            throw new IllegalArgumentException("can not encode Object as JSON", e);
        }
    }

    @SuppressFBWarnings(value = "DCN_NULLPOINTER_EXCEPTION", justification = "exception translation")
    public static UUID parseCreateGameResponse(final ResponseSpec response) {
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

    @SuppressFBWarnings(value="NP_NULL_ON_SOME_PATH", justification = "SpotBugs bug")
    private static void secure(
            @Nonnull final RequestHeadersSpec<?> request,
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken
    ) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(cookies, "cookies");

        if (authenticatingUser != null) {
            request.headers(headers -> headers.setBasicAuth(
                    authenticatingUser.getUsername(), authenticatingUser.getPassword())
            );
        }
        if (includeSessionCookie) {
            final var sessionCookie = Objects.requireNonNull(cookies.getFirst(SESSION_COOKIE_NAME));
            request.cookie(sessionCookie.getName(), sessionCookie.getValue());
        }
        if (includeXsrfToken) {
            final var xsrfCookie = Objects.requireNonNull(cookies.getFirst(XSRF_TOKEN_COOKIE_NAME));
            var value = xsrfCookie.getValue();
            request.headers(headers -> headers.add("X-XSRF-TOKEN", value));
            request.cookie(xsrfCookie.getName(), value);
        }
    }

    public WebTestClient.ResponseSpec addUser(
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final BasicUserDetails addingUserDetails,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken
    ) {
        final var request = connectWebTestClient().post().uri("/api/user")
                .contentType(MediaType.APPLICATION_JSON);
        secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        return request.bodyValue(McBackEndClient.encodeAsJson(addingUserDetails)).exchange();
    }

    @Nonnull
    private WebTestClient connectWebTestClient() {
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
        final var request = connectWebTestClient()
                .get().uri(Paths.createPathForUser(id))
                .accept(MediaType.APPLICATION_JSON);
        secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        return request.exchange();
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
        secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec joinGame(
            @Nonnull final UUID gameId,
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var request = connectWebTestClient().post()
                .uri(Paths.createPathForJoiningGame(gameId))
                .accept(MediaType.APPLICATION_JSON);
        secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec getCurrentGame(
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var request = connectWebTestClient().get().uri(Paths.CURRENT_GAME_PATH)
                .accept(MediaType.APPLICATION_JSON);
        secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec endRecruitment(
            @Nonnull final UUID gameId,
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var request = connectWebTestClient().post()
                .uri(Paths.createPathForEndRecruitmentOfGame(gameId))
                .accept(MediaType.APPLICATION_JSON);
        secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec startGame(
            @Nonnull final UUID gameId,
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var request = connectWebTestClient().post()
                .uri(Paths.createPathForStartingGame(gameId))
                .accept(MediaType.APPLICATION_JSON);
        secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec stopGame(
            @Nonnull final UUID gameId,
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var request = connectWebTestClient().post()
                .uri(Paths.createPathForStoppingGame(gameId))
                .accept(MediaType.APPLICATION_JSON);
        secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec createGameForScenario(
            @Nonnull final UUID scenarioId,
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var request = connectWebTestClient().post()
                .uri(Paths.createPathForGamesOfScenario(scenarioId))
                .accept(MediaType.APPLICATION_JSON);
        secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec getGame(
            @Nonnull final UUID gameId,
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken) {
        final var request = connectWebTestClient().get()
                .uri(Paths.createPathForGame(gameId))
                .accept(MediaType.APPLICATION_JSON);
        secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec getGamesOfScenario(
            @Nonnull final UUID scenarioId,
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken
    ) {
        final var request = connectWebTestClient().get()
                .uri(Paths.createPathForGamesOfScenario(scenarioId))
                .accept(MediaType.APPLICATION_JSON);
        secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        return request.exchange();
    }

    @Nonnull
    public WebTestClient.ResponseSpec mayJoin(
            @Nonnull final UUID gameId,
            @Nullable final BasicUserDetails authenticatingUser,
            @Nonnull final MultiValueMap<String, HttpCookie> cookies,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken
    ) {
        final var request = connectWebTestClient().get()
                .uri(Paths.createPathForMayJoinQueryOfGame(gameId))
                .accept(MediaType.APPLICATION_JSON);
        secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
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
