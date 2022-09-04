package uk.badamson.mc.rest;

import uk.badamson.mc.GameIdentifier;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;

public final class Paths {

    public static final String USERS_PATH = "/api/user";

    public static final String USER_PATH_PATTERN = "/api/user/{id}";

    public static final String SELF_PATH = "/api/self";

    public static final String SCENARIOS_PATH = "/api/scenario";

    public static final String SCENARIO_PATH_PATTERN = "/api/scenario/{id}";

    public static final String CURRENT_GAME_PATH = "/api/self/current-game";

    public static final String GAMES_PATH_PATTERN = "/api/scenario/{scenario}/games";

    public static final String GAME_PATH_PATTERN = "/api/game/{game}";

    private Paths() {
        throw new AssertionError("must not instantiate");
    }

    @Nonnull
    public static String createPathForUser(@Nonnull final UUID id) {
        Objects.requireNonNull(id, "id");
        return "/api/user/" + id;
    }

    @Nonnull
    public static String createPathForScenario(final UUID id) {
        Objects.requireNonNull(id, "id");
        return "/api/scenario/" + id;
    }

    @Nonnull
    public static String createPathForGamesOfScenario(@Nonnull final UUID scenario) {
        return "/api/scenario/" + scenario + "/games";
    }

    private static String format(@Nonnull final GameIdentifier id) {
        return id.getScenario().toString() + '@' + id.getCreated();
    }

    public static GameIdentifier parseGameIdentifier(@Nonnull String text) throws IllegalArgumentException {
        final int at = text.indexOf('@');
        if (at < 0) {
            throw new IllegalArgumentException("missing separator");
        } else if (at == text.length() - 1) {
            throw new IllegalArgumentException("created");
        }
        final UUID scenario;
        try {
            scenario = UUID.fromString(text.substring(0, at));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("scenario", e);
        }
        final Instant created;
        try {
            created = Instant.parse(text.substring(at + 1));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("created", e);
        }
        return new GameIdentifier(scenario, created);
    }

    @Nonnull
    public static String createPathForGame(@Nonnull final GameIdentifier id) {
        return "/api/game/" + format(id);
    }
}
