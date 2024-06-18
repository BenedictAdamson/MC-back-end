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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeoutException;

@Tag("BootJAR")
@Testcontainers
public class ProcessStartIT {
    private static final DockerImageName MONGO_DB_IMAGE = Fixtures.MONGO_DB_IMAGE;
    private static final String MONGO_DB_PASSWORD = "LetMeIn1";
    private static final String ADMINISTRATOR_PASSWORD = "LetMeIn2";

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(MONGO_DB_IMAGE);

    private static McBackEndProcess createProcess() throws IllegalStateException, TimeoutException {
        final var mongoDBPath = mongoDBContainer.getReplicaSetUrl();
        return new McBackEndProcess(mongoDBPath, MONGO_DB_PASSWORD, ADMINISTRATOR_PASSWORD);
    }


    @Test
    public void start() {
        try(var process = createProcess()) {
            process.assertThatNoErrorMessagesLogged();
        } catch (IllegalStateException | TimeoutException e) {
            throw new AssertionError(e);
        }
    }
}
