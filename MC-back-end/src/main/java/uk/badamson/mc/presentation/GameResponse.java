package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2020-22.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.PersistenceCreator;
import uk.badamson.mc.Scenario;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>
 * A game (play) of a scenario of the Mission Command game.
 * </p>
 */
public class GameResponse {

    /**
     * <p>
     * A unique identifier for a {@linkplain GameResponse game} (play) of the Mission
     * Command game.
     * </p>
     */
    @Immutable
    public static final class Identifier {

        private final UUID scenario;
        private final Instant created;

        /**
         * <p>
         * Create an object with given attribute values.
         * </p>
         *
         * @param scenario The unique identifier for the {@linkplain Scenario scenario}
         *                 that the game is an instance of.
         * @param created  The point in time when the game was created (set up).
         * @throws NullPointerException <ul>
         *                                         <li>If {@code scenario} is null.</li>
         *                                         <li>If {@code created} is null.</li>
         *                                         </ul>
         */
        @JsonCreator
        public Identifier(@Nonnull @JsonProperty("scenario") final UUID scenario,
                          @Nonnull @JsonProperty("created") final Instant created) {
            this.scenario = Objects.requireNonNull(scenario, "scenario");
            this.created = Objects.requireNonNull(created, "created");

        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof final Identifier other)) {
                return false;
            }
            /*
             * Two Identifiers are unlikely to have the same created value, so
             * check those values first.
             */
            return created.equals(other.created)
                    && scenario.equals(other.scenario);
        }

        /**
         * <p>
         * The point in time when the game was created (set up).
         * </p>
         * <p>
         * This will usually be not long before playing of the game started. This
         * type uses the creation time as an identifier, rather than the game
         * start time, so it can represent games that have not yet been started,
         * but are in the process of being set up.
         * </p>
         */
        @Nonnull
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public Instant getCreated() {
            return created;
        }

        /**
         * <p>
         * The unique identifier for the {@linkplain Scenario scenario} that the
         * game is an instance of.
         * </p>
         */
        @Nonnull
        public UUID getScenario() {
            return scenario;
        }

        @Override
        public int hashCode() {
            return Objects.hash(created, scenario);
        }

        @Override
        public String toString() {
            return scenario + "@" + created;
        }

    }

    public enum RunState {
        WAITING_TO_START, RUNNING, STOPPED
    }

    @org.springframework.data.annotation.Id
    private final Identifier identifier;

    private RunState runState;

    /**
     * <p>
     * Construct a copy of a game.
     * </p>
     *
     * @throws NullPointerException If {@code that} is null
     */
    public GameResponse(@Nonnull final GameResponse that) {
        Objects.requireNonNull(that, "that");
        identifier = that.identifier;
        runState = that.runState;
    }

    /**
     * <p>
     * Construct a game with given attribute values.
     * </p>
     *
     * @throws NullPointerException <ul>
     *                                         <li>If {@code identifier} is null.</li>
     *                                         <li>If {@code runState} is null.</li>
     *                                         </ul>
     */
    @JsonCreator
    @PersistenceCreator
    public GameResponse(@Nonnull @JsonProperty("identifier") final Identifier identifier,
                        @Nonnull @JsonProperty("runState") final RunState runState) {
        this.identifier = Objects.requireNonNull(identifier, "identifier");
        this.runState = Objects.requireNonNull(runState, "runState");
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to another object.
     * </p>
     * <ul>
     * <li>The {@link GameResponse} class has <i>entity semantics</i>, with the
     * {@linkplain #getIdentifier() identifier} serving as a unique identifier:
     * this object is equivalent to another object if, and only of, the other
     * object is also a {@link GameResponse} and the two have
     * {@linkplain Identifier#equals(Object) equivalent}
     * {@linkplain #getIdentifier() identifiers}.</li>
     * </ul>
     */
    @Override
    public final boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof final GameResponse other)) {
            return false;
        }
        return identifier.equals(other.getIdentifier());
    }

    /**
     * <p>
     * The unique identifier for this game.
     * </p>
     */
    @Nonnull
    @JsonProperty("identifier")
    public final Identifier getIdentifier() {
        return identifier;
    }

    @Nonnull
    @JsonProperty("runState")
    public RunState getRunState() {
        return runState;
    }

    @Override
    public final int hashCode() {
        return identifier.hashCode();
    }

    public void setRunState(@Nonnull final RunState runState) {
        this.runState = Objects.requireNonNull(runState, "runState");
    }

}
