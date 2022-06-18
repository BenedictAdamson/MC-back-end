package uk.badamson.mc.repository;
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
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.PersistenceCreator;
import uk.badamson.mc.Game;
import uk.badamson.mc.User;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * <p>
 * The set of {@linkplain User users} who played, or are playing, a particular
 * {@link Game game}.
 * </p>
 */
public class GamePlayersDTO {

    private static boolean hasNoDuplicates(final Collection<UUID> values) {
        return values.size() == Set.copyOf(values).size();
    }

    /**
     * <p>
     * Whether a given map is a valid {@linkplain #getUsers() users} map.
     * </p>
     */
    public static boolean isValidUsers(final Map<UUID, UUID> users) {
        return users != null
                && users.entrySet().stream()
                .allMatch(GamePlayersDTO::isValidUsersEntry)
                && hasNoDuplicates(users.values());
    }

    private static boolean isValidUsersEntry(final Map.Entry<UUID, UUID> entry) {
        final var character = entry.getKey();
        final var user = entry.getValue();
        return character != null && user != null;
    }

    @org.springframework.data.annotation.Id
    private final Game.Identifier game;

    private boolean recruiting;
    private final Map<UUID, UUID> users;

    /**
     * <p>
     * Construct a set of players with given values.
     * </p>
     *
     * @param game       The unique identifier for the game for which this is the set of
     *                   players.
     * @param recruiting Whether the game is <i>recruiting</i> new players.
     * @param users      The ({@linkplain User#getId() unique IDs} of the
     *                   {@linkplain User users} who played, or are playing, the
     *                   {@code game}, and the unique IDs of the characters they played.
     * @throws NullPointerException     <ul>
     *                                             <li>If {@code game} is null.</li>
     *                                             <li>If {@code users} is null.</li>
     *                                             </ul>
     * @throws IllegalArgumentException If {@code users} is not a {@linkplain #isValidUsers(Map) valid
     *                                  users map}
     */
    @JsonCreator
    @PersistenceCreator
    public GamePlayersDTO(@Nonnull @JsonProperty("game") final Game.Identifier game,
                          @JsonProperty("recruiting") final boolean recruiting,
                          @Nonnull @JsonProperty("users") final Map<UUID, UUID> users) {
        this.game = Objects.requireNonNull(game, "game");
        this.recruiting = recruiting;
        this.users = new HashMap<>(Objects.requireNonNull(users, "users"));

        if (!isValidUsers(this.users)) {// copy then test to avoid race hazards
            throw new IllegalArgumentException("users");
        }
    }

    /**
     * <p>
     * Construct a copy of a set of players.
     * </p>
     *
     * @throws NullPointerException If {@code that} is null
     */
    public GamePlayersDTO(@Nonnull final GamePlayersDTO that) {
        Objects.requireNonNull(that, "that");
        game = that.game;
        recruiting = that.recruiting;
        this.users = new HashMap<>(that.users);
    }

    /**
     * <p>
     * Add a {@linkplain User#getId() user ID} to the {@linkplain #getUsers() set
     * of users who played, or are playing}, the {@linkplain #getGame() game}.
     * </p>
     *
     * @param character The ID of the character that the user played.
     * @param user      The unique ID of the user to add as a player.
     * @throws NullPointerException  <ul>
     *                                          <li>If {@code character} is null.</li>
     *                                          <li>If {@code user} is null.</li>
     *                                          </ul>
     * @throws IllegalStateException <ul>
     *                                          <li>If the game is not {@linkplain #isRecruiting() recruiting}
     *                                          players.</li>
     *                                          <li>If the game already has the given user, but for a different
     *                                          character.</li>
     *                                          </ul>
     */
    public final void addUser(@Nonnull final UUID character,
                              @Nonnull final UUID user) {
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(user, "users");
        if (!recruiting) {
            throw new IllegalStateException("Game not recruiting players");
        }
        if (!user.equals(users.get(character)) && users.containsValue(user)) {
            throw new IllegalArgumentException(
                    "User already present with a different character");
        }

        users.put(character, user);
    }

    /**
     * <p>
     * Indicate that this game is not {@linkplain #isRecruiting() recruiting}
     * players (any longer).
     * </p>
     * <p>
     * This mutator is idempotent: the mutator does not have the precondition
     * that it is recruiting.
     * </p>
     *
     * @see #isRecruiting()
     */
    public final void endRecruitment() {
        recruiting = false;
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to another object.
     * </p>
     * <ul>
     * <li>The {@link GamePlayersDTO} class has <i>entity semantics</i>, with the
     * {@linkplain #getGame() identifier} serving as a unique identifier: this
     * object is equivalent to another object if, and only of, the other object
     * is also a {@link GamePlayersDTO} and the two have
     * {@linkplain Game.Identifier#equals(Object) equivalent}
     * {@linkplain #getGame() identifiers}.</li>
     * </ul>
     *
     * @param that The object to compare with this.
     * @return Whether this object is equivalent to {@code that} object.
     */
    @Override
    public final boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof final GamePlayersDTO other)) {
            return false;
        }
        return game.equals(other.game);
    }

    /**
     * <p>
     * The {@linkplain Game#getIdentifier() unique identifier for the game} for
     * which this provides the set of players.
     * </p>
     * <ul>
     * <li>Not null.</li>
     * </ul>
     *
     * @return the identifier.
     */
    @Nonnull
    @JsonProperty("game")
    public final Game.Identifier getGame() {
        return game;
    }

    /**
     * <p>
     * The ({@linkplain User#getId() unique IDs} of the {@linkplain User users}
     * who played, or are playing, the {@linkplain #getGame() game}, and the IDs
     * of the characters they played.
     * </p>
     * <ul>
     * <li>The map maps a character ID to the ID of the user who is playing (or
     * played, or will play) that character.</li>
     * </ul>
     * <ul>
     * <li>Always returns a {@linkplain #isValidUsers(Map) valid users map}.</li>
     * <li>The returned map of users is not modifiable.</li>
     * </ul>
     *
     * @return the users
     */
    @Nonnull
    @JsonProperty("users")
    public final Map<UUID, UUID> getUsers() {
        return Collections.unmodifiableMap(users);
    }

    @Override
    public final int hashCode() {
        return game.hashCode();
    }

    /**
     * @see #endRecruitment()
     */
    @JsonProperty("recruiting")
    public final boolean isRecruiting() {
        return recruiting;
    }

}
