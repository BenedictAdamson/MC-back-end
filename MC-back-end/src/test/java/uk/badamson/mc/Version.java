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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * <p>
 * The version of the SUT
 * </p>
 */
public final class Version {

   public static final String VERSION = getVersion();

   private static Properties getApplicationProperties() throws IOException {
      final InputStream stream = Thread.currentThread().getContextClassLoader()
               .getResourceAsStream("application.properties");
      if (stream == null) {
         throw new FileNotFoundException(
                  "resource application.properties not found");
      }
      final Properties properties = new Properties();
      properties.load(stream);
      return properties;
   }

   private static String getVersion() {
      String version;
      try {
         version = getApplicationProperties().getProperty("build.version");
      } catch (final IOException e) {
         throw new IllegalStateException(
                  "unable to read application.properties resource", e);
      }
      if (version == null || version.isEmpty()) {
         throw new IllegalStateException(
                  "missing build.version property in application.properties resource");
      }
      return version;
   }

}
