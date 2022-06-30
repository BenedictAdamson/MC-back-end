package uk.badamson.mc.rest;
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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>
 * A {@link UUID} associated with a human-readable title or name.
 * </p>
 * <p>
 * This type is intended for contexts where a precise and unique ID is needed,
 * but it is also necessary to present a human readable ID.
 * </p>
 */
@Immutable
public final class NamedUUID {

    /**
     * <p>
     * Whether a given text is valid as a {@linkplain #getTitle() title}
     * attribute for a {@link NamedUUID}.
     * </p>
     * <p>
     * A valid title conforms to all these constraints:
     * </p>
     * <ul>
     * <li>Not null.</li>
     * <li>Not {@linkplain String#isEmpty() empty}.</li>
     * <li>At most 64 code points {@linkplain String#length() long}.</li>
     * </ul>
     *
     * @param text the text of interest
     * @return whether valid
     */
    public static boolean isValidTitle(final String text) {
        if (text == null) {
            return false;
        } else {
            final var length = text.length();
            return 0 < length && length <= 64;
        }
    }

    private final UUID id;
    private final String title;

    /**
     * <p>
     * Construct an identifier with given attribute values.
     * </p>
     *
     * @param id    The unique identifier.
     * @param title A short human-readable identifier.
     * @throws NullPointerException     <ul>
     *                                             <li>If {@code id} is null</li>
     *                                             <li>If {@code title} is null</li>
     *                                             </ul>
     * @throws IllegalArgumentException If the {@code title} is not {@linkplain #isValidTitle(String)
     *                                  valid}.
     */
    @JsonCreator
    public NamedUUID(@Nonnull @JsonProperty("id") final UUID id,
                     @Nonnull @JsonProperty("title") final String title) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        if (!isValidTitle(title)) {
            throw new IllegalArgumentException("invalid title");
        }
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to another object.
     * </p>
     * <ul>
     * <li>The {@link NamedUUID} class has <i>entity semantics</i>, with the
     * {@linkplain #getId() ID} serving as a unique identifier: this object is
     * equivalent to another object if, and only of, the other object is also a
     * {@link NamedUUID} and the two have {@linkplain String#equals(Object)
     * equivalent} {@linkplain #getId() IDs}.</li>
     * </ul>
     */
    @Override
    public boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof final NamedUUID other)) {
            return false;
        }
        return id.equals(other.getId());
    }

    /**
     * <p>
     * The unique identifier.
     * </p>
     *
     * @return the identifier
     * @see #getTitle()
     */
    @Nonnull
    public UUID getId() {
        return id;
    }

    /**
     * <p>
     * A short human readable identifier.
     * </p>
     * <p>
     * Although the title is an identifier, it is not guaranteed to be a unique
     * identifier.
     * </p>
     */
    @Nonnull
    public String getTitle() {
        return title;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}