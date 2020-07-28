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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * <p>
 * Abstract base class for system tests of the MC back-end, using
 * Testcontainers.
 * </p>
 * <p>
 * These tests build the Docker image using the real Dockerfile, so they also
 * tests that Dockerfile.
 * </p>
 */
abstract class AbstractTestcontainersIT {

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

   /**
    * Use to initialise a @Container annotated GenericContainer value.
    */
   protected final 
   GenericContainer<?> createBasicContainer()
   {
      return new GenericContainer<>(createImage());
   }
   
   private final ImageFromDockerfile createImage() {
      return new ImageFromDockerfile()
               .withFileFromPath("Dockerfile", DOCKERFILE)
               .withFileFromPath("target/MC-back-end-.jar", JAR);
   }

}
