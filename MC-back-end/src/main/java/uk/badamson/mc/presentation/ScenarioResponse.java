package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2021-22.
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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.mc.GamePlayers;
import uk.badamson.mc.NamedUUID;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toUnmodifiableSet;

/**
 * <p>
 * A game scenario of the Mission Command game.
 * </p>
 */
public class ScenarioResponse {

    private static boolean hasDuplicateIds(final List<NamedUUID> characters) {
        final var ids = characters.stream().map(NamedUUID::getId)
                .collect(toUnmodifiableSet());
        return ids.size() != Set.copyOf(ids).size();
    }

    /**
     * <p>
     * Whether a given list of named IDs is a valid list of persons in a scenario
     * that {@linkplain GamePlayers players} can play.
     * </p>
     * A valid list meets all these constraints:
     * <ul>
     * <li>Non null.</li>
     * <li>not {@linkplain List#isEmpty() empty}.</li>
     * <li>has no null elements.</li>
     * <li>has no elements with duplicate {@linkplain NamedUUID#getId()
     * IDs}.</li>
     * </ul>
     */
    public static boolean isValidCharacters(
            final List<NamedUUID> characters) {
        return characters != null && !characters.isEmpty()
                && !hasDuplicateIds(characters);
    }

    @org.springframework.data.annotation.Id
    private final UUID identifier;
    private final String title;
    private final String description;
    private final List<NamedUUID> characters;

    /**
     * <p>
     * Construct a game scenario with given attributes and aggregates.
     * </p>
     *
     * @param identifier  The identifier for this scenario.
     * @param title       A short human-readable identifier for this scenario.
     * @param description A human-readable description for the scenario.
     * @param characters  The names of the persons in this scenario that
     *                    {@linkplain GamePlayers players} can play.
     * @throws NullPointerException     <ul>
     *                                             <li>If {@code identifier} is null</li>
     *                                             <li>If {@code title} is null</li>
     *                                             <li>If {@code description} is null</li>
     *                                             <li>If {@code description} is characters</li>
     *                                             </ul>
     * @throws IllegalArgumentException <ul>
     *                                             <li>If the {@code title} is not
     *                                             {@linkplain NamedUUID#isValidTitle(String) valid}.</li>
     *                                             <li>If the {@code characters} are not a
     *                                             {@linkplain #isValidCharacters(List) valid list of
     *                                             characters}.</li>
     *                                             </ul>
     */
    @JsonCreator
    public ScenarioResponse(@JsonProperty("identifier") @Nonnull final UUID identifier,
                            @Nonnull @JsonProperty("title") final String title,
                            @Nonnull @JsonProperty("description") final String description,
                            @Nonnull @JsonProperty("characters") final List<NamedUUID> characters) {
        this.identifier = Objects.requireNonNull(identifier, "identifier");
        this.title = Objects.requireNonNull(title, "title");
        this.description = Objects.requireNonNull(description, "description");
        // Can not use List.copyOf() because does not copy unmodifiable lists
        this.characters = List.copyOf(Objects.requireNonNull(characters, "characters"));

        if (!NamedUUID.isValidTitle(title)) {
            throw new IllegalArgumentException("invalid title");
        }
        if (!isValidCharacters(this.characters)) {// copy then check to avoid race
            // hazards
            throw new IllegalArgumentException("invalid characters");
        }
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to another object.
     * </p>
     * <ul>
     * <li>The {@link ScenarioResponse} class has <i>entity semantics</i>, with the
     * {@linkplain #getIdentifier() identifier} serving as a unique identifier:
     * this object is equivalent to another object if, and only of, the other
     * object is also a {@link ScenarioResponse} and the two have
     * {@linkplain UUID#equals(Object) equivalent} {@linkplain #getIdentifier()
     * identifiers}.</li>
     * </ul>
     */
    @Override
    public final boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof final ScenarioResponse other)) {
            return false;
        }
        return identifier.equals(other.getIdentifier());
    }

    /**
     * <p>
     * The names and IDs of the persons in this scenario that
     * {@linkplain GamePlayers players} can play.
     * </p>
     * <ul>
     * <li>The list of characters is in descending order of selection priority:
     * with all else equal, players should be allocated characters near the start
     * of the list.</li>
     * </ul>
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "authorities is unmodifiable")
    @Nonnull
    @JsonProperty("characters")
    public final List<NamedUUID> getCharacters() {
        return characters;
    }

    /**
     * <p>
     * A human readable description for this scenario.
     * </p>
     * <p>
     * Although different scenarios should have different descriptions,
     * descriptions are not guaranteed to be unique.
     * </p>
     *
     * @see NamedUUID#getTitle()
     */
    @Nonnull
    @JsonProperty("description")
    public final String getDescription() {
        return description;
    }

    /**
     * <p>
     * The unique identifier for this scenario.
     * </p>
     * <ul>
     * <li>Not null.</li>
     * </ul>
     *
     * @return the identifier.
     */
    @Nonnull
    @JsonProperty("identifier")
    public final UUID getIdentifier() {
        return identifier;
    }

    /**
     * <p>
     * A unique ID with a human readable title, for this scenario.
     * </p>
     */
    @Nonnull
    @JsonIgnore
    public final NamedUUID getNamedUUID() {
        return new NamedUUID(identifier, title);
    }

    /**
     * <p>
     * A short human readable identifier for this scenario.
     * </p>
     * <p>
     * Although the title is an identifier, it is not guaranteed to be a unique
     * identifier.
     * </p>
     */
    @Nonnull
    @JsonProperty("title")
    public final String getTitle() {
        return title;
    }

    @Override
    public final int hashCode() {
        return identifier.hashCode();
    }

}
