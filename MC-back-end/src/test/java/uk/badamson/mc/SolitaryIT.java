package uk.badamson.mc;
/*
 * © Copyright Benedict Adamson 2019-20.
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * <p>
 * Basic system test for the MC back-end, testing it operating alone without any
 * needed servers.
 * </p>
 * <p>
 * The MC back-end expects and needs a database server to be present, so this
 * tests that it provides useful diagnostic messages if the database is missing
 * or misbehaving. The test builds the Docker image using the real Dockerfile,
 * so this also tests that Dockerfile.
 * </p>
 */
@Testcontainers
@Tag("IT")
public class SolitaryIT {

   public static final String EXPECTED_STARTED_MESSAGE = "Started Application";
   public static final String EXPECTED_ERROR_MESSAGE = "Connection refused";

   private static final Path TARGET_DIR = Paths.get("target");
   private static final Path DOCKERFILE = Paths.get("Dockerfile");

   private static final String SUT_VERSION;
   static {
      SUT_VERSION = System.getProperty("sutVersion", "");
      if (SUT_VERSION == null || SUT_VERSION.isEmpty()) {
         throw new IllegalStateException("setVersion property not set");
      }
   }
   private static final Path JAR = TARGET_DIR
            .resolve("MC-back-end-" + SUT_VERSION + ".jar");

   @Container
   private final GenericContainer<?> container = new GenericContainer<>(
            new ImageFromDockerfile().withFileFromPath("Dockerfile", DOCKERFILE)
                     .withFileFromPath("target/MC-back-end-.jar", JAR));

   @Test
   public void start() {
      final var consumer = new WaitingConsumer();
      container.followOutput(consumer);
      try {
         consumer.waitUntil(frame -> {
            final String text = frame.getUtf8String();
            return text.contains(EXPECTED_STARTED_MESSAGE)
                     || text.contains(EXPECTED_ERROR_MESSAGE);
         }, 11000, TimeUnit.MILLISECONDS);
      } catch (final TimeoutException e) {
         // Fall through to the assertion check (which will fail)
      }

      final var logs = container.getLogs();
      assertThat("Logged error message", logs,
               containsString(EXPECTED_ERROR_MESSAGE));
   }
}
