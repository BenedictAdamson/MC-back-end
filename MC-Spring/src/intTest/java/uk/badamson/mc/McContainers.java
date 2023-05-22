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

import org.testcontainers.containers.GenericContainer;
import uk.badamson.mc.presentation.McReverseProxyContainer;
import uk.badamson.mc.repository.McDatabaseContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class McContainers extends BaseContainers {

    private static final String ADMINISTRATOR_PASSWORD = "secret4";
    private static final String BE_HOST = "be";
    private static final String DB_HOST = "db";
    private static final String REVERSE_PROXY_HOST = "in";

    private static final URI BASE_PRIVATE_NETWORK_URI = URI
            .create("http://" + REVERSE_PROXY_HOST);

    private static final String DB_ROOT_PASSWORD = "secret2";
    private static final String DB_USER_PASSWORD = "secret3";
    private final McDatabaseContainer db;
    private final McBackEndContainer be;
    private final McReverseProxyContainer in;

    /**
     * @param failureRecordingDirectory The location of a directory in which to store files holding
     *                                  verbose information about failed test cases. Or {@code null} if
     *                                  no such records are to be made.
     */
    public McContainers(@Nullable final Path failureRecordingDirectory) {
        super(failureRecordingDirectory);
        db = new McDatabaseContainer(
                DB_ROOT_PASSWORD, DB_USER_PASSWORD).withNetwork(getNetwork())
                .withNetworkAliases(DB_HOST);
        be = new McBackEndContainer(DB_HOST,
                DB_USER_PASSWORD, ADMINISTRATOR_PASSWORD).withNetwork(getNetwork())
                .withNetworkAliases(BE_HOST);
        in = McReverseProxyContainer.createWithRealBe()
                .withNetwork(getNetwork()).withNetworkAliases(REVERSE_PROXY_HOST);
    }

    private static void assertThatNoErrorMessagesLogged(final String container,
                                                        final String logs) {
        assertThat(container + " logs no errors", logs,
                not(containsString("ERROR:")));
    }

    public static URI createIngressPrivateNetworkUriFromPath(final String path) {
        return BASE_PRIVATE_NETWORK_URI.resolve(path);
    }

    @Nonnull
    public final McBackEndContainer getBackEnd() {
        return be;
    }

    public void assertThatNoErrorMessagesLogged() {
        assertThatNoErrorMessagesLogged("db", db.getLogs());
        assertThatNoErrorMessagesLogged("be", be.getLogs());
        assertThatNoErrorMessagesLogged("fe", getFrontEnd().getLogs());
        assertThatNoErrorMessagesLogged("in", in.getLogs());
        assertThatNoErrorMessagesLogged("browser", getBrowser().getLogs());
    }

    @Override
    public void close() {
        /*
         * Close the resources top-down, to reduce the number of transient
         * connection errors.
         */
        in.close();
        be.close();
        db.close();
        super.close();
    }

    @Nonnull
    public URI createUriFromPath(final HttpServer server, final String path) {
        GenericContainer<?> container = switch (server) {
            case BACK_END -> be;
            case FRONT_END -> getFrontEnd();
            case INGRESS -> in;
        };
        final var base = URI.create("http://" + container.getHost() + ":"
                + container.getFirstMappedPort());
        return base.resolve(path);
    }

    @Override
    protected void retainLogFiles(final String prefix) {
        assert getFailureRecordingDirectory() != null;
        super.retainLogFiles(prefix);
        retainLogFile(getFailureRecordingDirectory(), prefix, DB_HOST, db);
        retainLogFile(getFailureRecordingDirectory(), prefix, BE_HOST, be);
        retainLogFile(getFailureRecordingDirectory(), prefix, REVERSE_PROXY_HOST, in);
    }

    @Override
    public void start() {
        /*
         * Start the containers bottom-up, and wait until each is ready, to reduce
         * the number of transient connection errors.
         */
        startInParallel(db, getFrontEnd(), getBrowser());
        be.start();
        in.start();
    }

    @Override
    public void stop() {
        /*
         * Stop the resources top-down, to reduce the number of transient
         * connection errors.
         */
        getBrowser().stop();
        in.stop();
        getFrontEnd().stop();
        be.stop();
        db.stop();
        close();
    }

    public enum HttpServer {
        BACK_END, FRONT_END, INGRESS
    }// enum
}
