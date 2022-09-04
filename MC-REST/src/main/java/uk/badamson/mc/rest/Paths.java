package uk.badamson.mc.rest;

import uk.badamson.mc.GameIdentifier;

import javax.annotation.Nonnull;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public final class Paths {

    public static final String USERS_PATH = "/api/user";

    public static final String USER_PATH_PATTERN = "/api/user/{id}";

    public static final String SELF_PATH = "/api/self";

    public static final String SCENARIOS_PATH = "/api/scenario";

    public static final String SCENARIO_PATH_PATTERN = "/api/scenario/{id}";

    public static final String CURRENT_GAME_PATH = "/api/self/current-game";

    public static final String GAMES_PATH_PATTERN = "/api/game/{scenario}";

    public static final String GAME_PATH_PATTERN = "/api/game/{scenario}/{created:.+}";

    public static final DateTimeFormatter URI_DATETIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

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
        return "/api/game/" + scenario + "/";
    }

    @Nonnull
    public static String createPathForGame(@Nonnull final GameIdentifier id) {
        Objects.requireNonNull(id, "id");
        return createPathForGamesOfScenario(id.getScenario())
                + Paths.URI_DATETIME_FORMATTER.format(id.getCreated());
    }
}
