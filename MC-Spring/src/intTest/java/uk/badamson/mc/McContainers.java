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

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

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
    private static final DockerImageName MONGO_DB_IMAGE = DockerImageName.parse("mongo:4.4");

    private final MongoDBContainer db;
    private final McBackEndContainer be;

    public McContainers(@Nullable final Path failureRecordingDirectory) {
        super(failureRecordingDirectory);
        db = new MongoDBContainer(MONGO_DB_IMAGE).withNetwork(getNetwork())
                .withNetworkAliases(DB_HOST);
        db.start();
        be = new McBackEndContainer(db.getReplicaSetUrl(), ADMINISTRATOR_PASSWORD)
                .withNetwork(getNetwork())
                .withNetworkAliases(BE_HOST);
        be.start();
    }

    private static void assertThatNoErrorMessagesLogged(final String container,
                                                        final String logs) {
        assertThat(container + " logs no errors", logs,
                not(containsString("ERROR:")));
    }

    @Nonnull
    public final McBackEndContainer getBackEnd() {
        return be;
    }

    public void assertThatNoErrorMessagesLogged() {
        assertThatNoErrorMessagesLogged("db", db.getLogs());
        assertThatNoErrorMessagesLogged("be", be.getLogs());
    }

    @Override
    public void close() {
        /*
         * Close the resources top-down, to reduce the number of transient
         * connection errors.
         */
        be.close();
        db.close();
        super.close();
    }

    @Nonnull
    public URI createUriFromPath(final String path) {
        final var base = URI.create("http://" + be.getHost() + ":"
                + be.getFirstMappedPort());
        return base.resolve(path);
    }

    @Override
    protected void retainLogFiles(final String prefix) {
        assert getFailureRecordingDirectory() != null;
        super.retainLogFiles(prefix);
        retainLogFile(getFailureRecordingDirectory(), prefix, DB_HOST, db);
        retainLogFile(getFailureRecordingDirectory(), prefix, BE_HOST, be);
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

}
