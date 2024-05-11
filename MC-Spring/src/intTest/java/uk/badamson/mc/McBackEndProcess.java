package uk.badamson.mc;
/*
 * © Copyright Benedict Adamson 2024.
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.random.RandomGenerator;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressFBWarnings(value = "DCN_NULLPOINTER_EXCEPTION", justification="Chained exception")
public final class McBackEndProcess implements AutoCloseable {

    private static final String VERSION = SutVersions.MC_BACK_END_VERSION;
    private static final String JAR_FILENAME = "MC-Spring-" + VERSION + ".jar";

    private static final Path JAR_PATH;
    private static final Duration POLL_INTERVAL = Duration.ofMillis(10);
    private static final RandomGenerator RANDOM_GENERATOR = RandomGenerator.getDefault();
    private static final Charset ENCODING = Charset.defaultCharset();

    static {
        final var applicationPropertiesUrl = Thread.currentThread().getContextClassLoader().getResource("application.properties");
        final Path applicationPropertiesPath;
        try {
            applicationPropertiesPath = Paths.get(Objects.requireNonNull(applicationPropertiesUrl).toURI()).toAbsolutePath();
        } catch (URISyntaxException | NullPointerException e) {
            throw new IllegalStateException(e);
        }
        final Path jarDirectory;
        try {
            jarDirectory = applicationPropertiesPath.getParent().getParent().getParent().getParent();
            Objects.requireNonNull(jarDirectory);
        } catch (NullPointerException e) {
            throw new IllegalStateException("Missing parent directory in path " + applicationPropertiesPath, e);
        }
        final var buildDir = jarDirectory.resolve("build").resolve("libs");
        JAR_PATH = buildDir.resolve(JAR_FILENAME);
    }

    private static void setEnvironment(
            @Nonnull final Map<String, String> env,
            @Nonnull final String mongoDbPassword,
            @Nonnull final String administratorPassword
    ) {
        env.put("SPRING_DATA_MONGODB_PASSWORD", Objects.requireNonNull(mongoDbPassword));
        env.put("ADMINISTRATOR_PASSWORD", Objects.requireNonNull(administratorPassword));
    }

    private static List<String> createCommandLine(
            @Nonnegative final int serverPort,
            @Nonnegative final int debugPort,
            @Nonnull String dbUri,
            @Nonnull List<String> options
    ) {
        Objects.requireNonNull(dbUri);
        List<String> command = new ArrayList<>(options.size() + 6);
        command.addAll(List.of(
                "java",
                "-agentlib:jdwp=transport=dt_socket,address="  + debugPort + ",server=y,suspend=n",
                "-jar",
                JAR_PATH.toString(),
                "--server.port=" + serverPort,
                "--spring.data.mongodb.uri=" + dbUri
        ));
        command.addAll(options);
        return command;
    }

    /*
     * We want thread safe processing of the output of the process within the McBackEndProcess constructor.
     * But the memory model does not provide the full semantics of final data members until return from a
     * constructor. The parts that must be accessed from multiple threads musty therefore be in a separate class
     * that completes construction before the pump thread begins processing.
     */
    private static final class Delegate implements Runnable {
        @Nonnull
        private final Process process;
        @GuardedBy("this")
        private final ByteArrayOutputStream logBytes = new ByteArrayOutputStream();

        Delegate(@Nonnull Process process) {
            this.process = process;
        }

        void closeOutputStream() throws IOException {
            process.getOutputStream().close();
        }

        synchronized String getLog() {
            return logBytes.toString(ENCODING);
        }


        void close() {
            process.destroy();
            try {
                // Give it a chance to shut-down cleanly.
                process.waitFor(7L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        }

        @Override
        public void run() {
            pumpOutput();
        }

        private void pumpOutput() {
            final var inputStream = process.getInputStream();
            do {
                try {
                    int c = inputStream.read();
                    if (c == -1) {
                        return;
                    }
                    synchronized (this) {
                        logBytes.write(c);
                    }
                } catch (IOException e) {
                    // Treat as no data available
                }
            } while (true);
        }
    }

    @Nonnull
    private final Delegate delegate;
    @Nonnegative
    final int serverPort;
    @Nonnegative
    final int debugPort;

    public McBackEndProcess(
            @Nonnegative final int serverPort,
            @Nonnegative final int debugPort,
            @Nonnull final String dbUri,
            @Nonnull final String mongoDbPassword,
            @Nonnull final String administratorPassword,
            @Nonnull final List<String> options
    ) throws IllegalStateException, TimeoutException {
        this.serverPort = serverPort;
        this.debugPort = debugPort;
        final var processBuilder = new ProcessBuilder(createCommandLine(serverPort, debugPort, dbUri, options))
                .redirectErrorStream(true);
        setEnvironment(processBuilder.environment(), mongoDbPassword, administratorPassword);
        try {
            delegate = new Delegate(processBuilder.start());
            new Thread(delegate).start();
            try {
                delegate.closeOutputStream();
                waitForLogMessage("Starting", Duration.ofSeconds(4));
                waitForLogMessage("Bootstrapping Spring Data MongoDB", Duration.ofSeconds(2));
                waitForLogMessage("Finished Spring Data repository scanning", Duration.ofSeconds(10));
                waitForLogMessage("Opened connection", Duration.ofSeconds(5));
                waitForLogMessage("Started", Duration.ofSeconds(30));
            } catch (TimeoutException e) {
                delegate.close();
                var e2 = new TimeoutException(
                        "Timeout while awaiting process to start-up:\n" + getLog()
                );
                e2.initCause(e);
                throw e2;
            } catch (InterruptedException e) {
                delegate.close();
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } catch (IOException e) {
                delegate.close();
                throw e;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public McBackEndProcess(
            @Nonnull final String dbUri,
            @Nonnull final String mongoDbPassword,
            @Nonnull final String administratorPassword
    ) throws IllegalStateException, TimeoutException {
        this(randomServerPort(), randomServerPort(), dbUri, mongoDbPassword, administratorPassword, List.of());
    }

    private static int randomServerPort() {
        return RANDOM_GENERATOR.ints(49152, 65535)
                .findAny().orElseThrow();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Nonnegative
    public int getServerPort() {
        return serverPort;
    }

    public void waitForLogMessage(
            String message,
            Duration timeout
    ) throws IOException, InterruptedException, TimeoutException {
        Objects.requireNonNull(message);
        var now = Instant.now();
        String log = getLog();
        boolean found = log.contains(message);
        final Instant stopTime = now.plus(timeout);
        while (!found && !now.isAfter(stopTime)) {
            var sleep = Duration.between(now, stopTime);
            if (0 < POLL_INTERVAL.compareTo(sleep)) {
                sleep = POLL_INTERVAL;
            }
            Thread.sleep(sleep.toMillis());
            now = Instant.now();
            log = getLog();
            found = log.contains(message);
        }
        if (!found) {
            throw new TimeoutException("Timeout while awaiting " + message);
        }
    }

    public String getLog() {
        return delegate.getLog();
    }

    public void assertThatNoErrorMessagesLogged() {
        assertThat( "Logged no errors", getLog(),
                not(containsString("ERROR:")));
    }
}
