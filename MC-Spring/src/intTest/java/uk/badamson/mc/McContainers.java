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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public final class McContainers implements Startable, TestLifecycleAware {

    private static final String ADMINISTRATOR_PASSWORD = "secret4";
    private static final String BE_HOST = "be";
    private static final String DB_HOST = "db";
    private static final DockerImageName MONGO_DB_IMAGE = DockerImageName.parse("mongo:4.4");

    private final MongoDBContainer db;
    private final McBackEndContainer be;
    @Nullable
    private final Path failureRecordingDirectory;
    private final Network network = Network.newNetwork();

    public McContainers(@Nullable final Path failureRecordingDirectory) {
        this.failureRecordingDirectory = failureRecordingDirectory;
        if (failureRecordingDirectory != null) {
            try {
                Files.createDirectories(failureRecordingDirectory);
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        db = new MongoDBContainer(MONGO_DB_IMAGE).withNetwork(network)
                .withNetworkAliases(DB_HOST);
        db.start();
        be = new McBackEndContainer(db.getReplicaSetUrl(), ADMINISTRATOR_PASSWORD)
                .withNetwork(network)
                .withNetworkAliases(BE_HOST);
        be.start();
    }

    private static void assertThatNoErrorMessagesLogged(final String container,
                                                        final String logs) {
        assertThat(container + " logs no errors", logs,
                not(containsString("ERROR:")));
    }

    private static void retainLogFile(
            @Nonnull final Path directory,
            @Nonnull final String baseFileName,
            @Nonnull final String host,
            @Nonnull final GenericContainer<?> container) {
        final String leafName = baseFileName + "-" + host + ".log";
        final Path path = directory.resolve(leafName);
        try {
            Files.writeString(path, container.getLogs(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void assertThatNoErrorMessagesLogged() {
        assertThatNoErrorMessagesLogged("db", db.getLogs());
        assertThatNoErrorMessagesLogged("be", be.getLogs());
    }

    @javax.annotation.OverridingMethodsMustInvokeSuper
    @Override
    public void close() {
        /*
         * Close the resources top-down, to reduce the number of transient
         * connection errors.
         */
        be.close();
        db.close();
        network.close();
    }

    private void retainLogFiles(final String prefix) {
        assert failureRecordingDirectory != null;
        assert failureRecordingDirectory != null;
        retainLogFile(failureRecordingDirectory, prefix, DB_HOST, db);
        retainLogFile(failureRecordingDirectory, prefix, BE_HOST, be);
    }

    @Override
    public void start() {
        /*
         * Start the containers bottom-up, and wait until each is ready, to reduce
         * the number of transient connection errors.
         */
        db.start();
        be.start();
    }

    @Override
    public void stop() {
        /*
         * Stop the resources top-down, to reduce the number of transient
         * connection errors.
         */
        be.stop();
        db.stop();
        close();
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void afterTest(final TestDescription description, final Optional<Throwable> throwable) {
        if (failureRecordingDirectory != null) {
            final var prefix = description.getFilesystemFriendlyName();
            retainLogFiles(prefix);
        }
    }

}
