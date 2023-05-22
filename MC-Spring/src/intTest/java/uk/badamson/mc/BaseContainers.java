package uk.badamson.mc;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

abstract class BaseContainers implements Startable, TestLifecycleAware {

    @Nullable
    private final Path failureRecordingDirectory;
    private final Network network = Network.newNetwork();

    protected BaseContainers(@Nullable Path failureRecordingDirectory) {
        this.failureRecordingDirectory = failureRecordingDirectory;
        if (failureRecordingDirectory != null) {
            try {
                Files.createDirectories(failureRecordingDirectory);
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    protected static void retainLogFile(
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

    protected static void startInParallel(@Nonnull GenericContainer<?>... containers) {
        Stream.of(containers).parallel().forEach(GenericContainer::start);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void close() {
        network.close();
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void afterTest(final TestDescription description, final Optional<Throwable> throwable) {
        if (getFailureRecordingDirectory() != null) {
            final var prefix = description.getFilesystemFriendlyName();
            retainLogFiles(prefix);
        }
    }

    @OverridingMethodsMustInvokeSuper
    protected void retainLogFiles(final String prefix) {
        assert getFailureRecordingDirectory() != null;
    }

    @Nullable
    protected final Path getFailureRecordingDirectory() {
        return failureRecordingDirectory;
    }

    @Nonnull
    protected final Network getNetwork() {
        return network;
    }
}
