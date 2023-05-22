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

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public final class SutVersions {

    private static final Properties APPLICATION_PROPERTIES = loadApplicationProperties();
    public static final String MC_WEB_VERSION = getApplicationProperty("build.version");
    public static final String MC_BACK_END_VERSION = getApplicationProperty("MC.backend.version");

    private static Properties loadApplicationProperties() {
        final var stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("application.properties");
        final Properties properties;
        try {
            if (stream == null) {
                throw new FileNotFoundException(
                        "resource application.properties not found");
            }
            properties = new Properties();
            properties.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return properties;
    }

    private static String getApplicationProperty(@Nonnull String key) {
        final String version = APPLICATION_PROPERTIES.getProperty(key);
        if (version == null || version.isEmpty()) {
            throw new IllegalStateException("missing property in application.properties resource");
        }
        return version;
    }

}
