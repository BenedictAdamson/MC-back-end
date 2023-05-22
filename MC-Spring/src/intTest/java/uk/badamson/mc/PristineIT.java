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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestMethodOrder(OrderAnnotation.class)
@Testcontainers
@Tag("IT")
public class PristineIT implements AutoCloseable {

    private static McContainers containers;

    @BeforeAll
    public static void open() {
        containers = new McContainers(null);
    }

    @AfterAll
    public static void stop() {
        if (containers != null) {
            containers.stop();
            containers = null;
        }
    }

    @Override
    public void close() {
        stop();
    }


    @Test
    @Order(1)
    public void start() {
        containers.assertThatNoErrorMessagesLogged();
    }
}
