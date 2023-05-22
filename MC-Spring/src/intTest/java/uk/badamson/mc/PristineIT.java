package uk.badamson.mc;
/*
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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.badamson.mc.McContainers.HttpServer;

import java.io.IOException;
import java.net.HttpURLConnection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * <p>
 * Basic system test for the MC components operating together, testing it
 * operating as a pristine (fresh) installation.
 * </p>
 * <p>
 * These tests demonstrate that it is possible to configure the components to
 * communicate correctly with each other. They do not really demonstrate
 * specific correct functionality.
 * </p>
 */
@TestMethodOrder(OrderAnnotation.class)
@Testcontainers
@Tag("IT")
public class PristineIT implements AutoCloseable {

    /**
     * All the tests are read-only, so we do not need to recreate the containers
     * for each test. This is a big win, because creating all the containers is
     * very expensive.
     */
    private static final McContainers containers = new McContainers(null);

    @BeforeAll
    public static void open() {
        containers.start();
    }

    @AfterAll
    public static void stop() {
        containers.stop();
    }

    private void assertHttpStatusOk(final HttpServer server) {
        assertThat("HTTP status", getRootHttpResponseCode(server),
                is(HttpURLConnection.HTTP_OK));
    }

    @Override
    public void close() {
        stop();
    }

    @Test
    @Order(2)
    public void getHomePageFromFrontEnd() {
        assertHttpStatusOk(HttpServer.FRONT_END);
    }

    @Test
    @Order(2)
    public void getHomePageThroughIngress() {
        assertHttpStatusOk(HttpServer.INGRESS);
    }

    private int getRootHttpResponseCode(final HttpServer server) {
        try {
            final var localUrl = containers.createUriFromPath(server, "");
            final var connection = (HttpURLConnection) localUrl.toURL()
                    .openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            return connection.getResponseCode();
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @Order(3)
    public void getUsersPageFromFrontEnd() {
        assertHttpStatusOk(HttpServer.FRONT_END
        );
    }

    @Test
    @Order(3)
    public void getUsersPageThroughIngress() {
        assertHttpStatusOk(HttpServer.INGRESS
        );
    }

    @Test
    @Order(1)
    public void start() {
        containers.assertThatNoErrorMessagesLogged();
    }
}
