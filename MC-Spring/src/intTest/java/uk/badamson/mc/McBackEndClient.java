package uk.badamson.mc;
/*
 * © Copyright Benedict Adamson 2019-23.
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
import org.springframework.http.HttpCookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.RequestBodySpec;
import org.springframework.test.web.reactive.server.WebTestClient.RequestHeadersSpec;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriTemplate;
import uk.badamson.mc.rest.Paths;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

final class McBackEndClient {

    private static final String XSRF_TOKEN_COOKIE_NAME = "XSRF-TOKEN";

    private static final String SESSION_COOKIE_NAME = "JSESSIONID";

    private static final UriTemplate USER_URI_TEMPLATE = new UriTemplate(Paths.USER_PATH_PATTERN);

    private static final UriTemplate GAME_URI_TEMPLATE = new UriTemplate(Paths.GAME_PATH_PATTERN);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String SCHEME = "http";

    @Nonnull
    private final String host;
    @Nonnegative
    private final int port;
    @Nonnull
    private final User administrator;

    McBackEndClient(
            @Nonnull final String host,
            @Nonnegative final int port,
            @Nonnull final String administratorPassword
    ) {
        Objects.requireNonNull(host);
        Objects.requireNonNull(administratorPassword);
        if (port < 0) {
            throw new IllegalArgumentException();
        }
        this.host = host;
        this.port = port;
        this.administrator = User.createAdministrator(administratorPassword);
    }

    private static String encodeAsJson(final Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (final Exception e) {
            throw new IllegalArgumentException("can not encode Object as JSON", e);
        }
    }

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

    private static void secure(final RequestBodySpec request,
                               final BasicUserDetails user,
                               final MultiValueMap<String, HttpCookie> cookies) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(cookies, "cookies");

        final var sessionCookie = cookies.getFirst(SESSION_COOKIE_NAME);
        final var xsrfCookie = cookies.getFirst(XSRF_TOKEN_COOKIE_NAME);
        Objects.requireNonNull(sessionCookie, "sessionCookie");
        Objects.requireNonNull(xsrfCookie, "xsrfCookie");
        if (user != null) {
            request.headers(headers -> headers.setBasicAuth(user.getUsername(), user.getPassword()));
        }
        request.headers(headers -> headers.add("X-XSRF-TOKEN", xsrfCookie.getValue()));
        request.cookie(sessionCookie.getName(), sessionCookie.getValue());
        request.cookie(xsrfCookie.getName(), xsrfCookie.getValue());
    }

    public UUID addUser(final BasicUserDetails userDetails) {
        try {
            Objects.requireNonNull(userDetails, "userDetails");

            final var cookies = login(administrator);
            final var headers = connectWebTestClient("/api/user").post()
                    .contentType(MediaType.APPLICATION_JSON);
            secure(headers, administrator, cookies);
            final var request = headers.bodyValue(encodeAsJson(userDetails));

            final var response = request.exchange();
            response.expectStatus().isFound();
            final var location = response.returnResult(Void.class)
                    .getResponseHeaders().getLocation();
            if(location == null) {
                throw new IllegalStateException("response has Location header");
            }
            final var id = UUID.fromString(
                    USER_URI_TEMPLATE.match(location.toString()).get("id"));
            logout(administrator, cookies);
            return id;
        } catch (final Exception e) {
            throw new RuntimeException("Failed to add user", e);
        }
    }

    @Nonnull
    private WebTestClient connectWebTestClient(@Nonnull final String path) {
        final URI uri;
        try {
            uri = new URI(SCHEME, null, host, port, path, null, null);
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        return WebTestClient.bindToServer().baseUrl(uri.toString()).build();
    }

    private RequestBodySpec createCreateGameRequest(final UUID scenario, final User user,
                                                    final MultiValueMap<String, HttpCookie> cookies) {
        Objects.requireNonNull(cookies, "cookies");

        final var path = Paths.createPathForGamesOfScenario(scenario);
        final var request = connectWebTestClient(path).post()
                .accept(MediaType.APPLICATION_JSON);
        secure(request, user, cookies);
        return request;
    }

    public UUID createGame(final UUID scenario) {
        Objects.requireNonNull(scenario, "scenario");

        final var cookies = login(administrator);
        final var request = createCreateGameRequest(scenario, administrator,
                cookies);
        final var response = request.exchange();
        logout(administrator, cookies);

        response.expectStatus().isFound();
        return parseCreateGameResponse(response);
    }

    private RequestHeadersSpec<?> createGetSelfRequest(final String username,
                                                       final String password) {
        return connectWebTestClient("/api/self").get()
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBasicAuth(username, password));
    }

    public Stream<NamedUUID> getScenarios() {
        return connectWebTestClient("/api/scenario").get().accept(MediaType.APPLICATION_JSON)
                .exchange().returnResult(uk.badamson.mc.rest.NamedUUID.class)
                .getResponseBody().toStream().map(ni -> new NamedUUID(ni.getId(), ni.getTitle()));
    }

    private MultiValueMap<String, HttpCookie> login(final BasicUserDetails user) {
        final var request = createGetSelfRequest(user.getUsername(),
                user.getPassword());

        final var response = request.exchange();

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

    private void logout(final User user,
                        final MultiValueMap<String, HttpCookie> cookies) {
        final var request = connectWebTestClient("/logout").post();
        secure(request, user, cookies);
        final var response = request.exchange();
        response.expectStatus().is2xxSuccessful();
    }

}
