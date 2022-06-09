package uk.badamson.mc.repository;
/*
 * © Copyright Benedict Adamson 2019-20,22.
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.data.repository.CrudRepository;

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link CrudRepository}
 * interface.
 * </p>
 */
public class CrudRepositoryTest {

   public static abstract class AbstractFake<T, ID>
            implements CrudRepository<T, ID> {

      protected static void requireNonNull(@Nullable final Object object,
               @Nonnull final String message) {
         try {
            Objects.requireNonNull(object, message);
         } catch (final NullPointerException e) {
            throw new IllegalArgumentException(e);
         }
      }

      protected final Map<ID, T> entities = new ConcurrentHashMap<>();

      protected abstract T copy(T entity);

      @Override
      public final long count() {
         return entities.size();
      }

      @Override
      public final void delete(@Nonnull final T entity) {
         requireNonNull(entity, "entity");
         deleteById(getId(entity));
      }

      @Override
      public final void deleteAll() {
         entities.clear();
      }

      @Override
      public final void deleteAll(@Nonnull final Iterable<? extends T> entities) {
         requireNonNull(entities, "entities");
         for (final T entity : entities) {
            this.entities.remove(getId(entity));
         }
      }

      @Override
      public final void deleteById(@Nonnull final ID identifier) {
         requireNonNull(identifier, "identifier");
         entities.remove(identifier);
      }

      @Override
      public void deleteAllById(@Nonnull Iterable<? extends ID> identifiers) {
         requireNonNull(identifiers, "identifiers");
         for (final ID identifier: identifiers) {
            entities.remove(identifier);
         }
      }

      @Override
      public final boolean existsById(@Nonnull final ID identifier) {
         requireNonNull(identifier, "identifier");
         return entities.containsKey(identifier);
      }

      @Nonnull
      @Override
      public final Iterable<T> findAll() {
         // Return copies of the entities, so we are isolated from downstream
         // mutations
         return entities.values().stream().map(this::copy)
                  .collect(toList());
      }

      @Nonnull
      @Override
      public final Iterable<T> findAllById(@Nonnull final Iterable<ID> identifiers) {
         requireNonNull(identifiers, "identifiers");
         // Return copies of the entities, so we are isolated from downstream
         // mutations
         return StreamSupport.stream(identifiers.spliterator(), false)
                  .distinct().filter(Objects::nonNull)
                  .map(entities::get).map(this::copy)
                  .collect(toUnmodifiableList());
      }

      @Nonnull
      @Override
      public final Optional<T> findById(@Nonnull final ID identifier) {
         requireNonNull(identifier, "identifier");
         var entity = entities.get(identifier);
         // Return copy so we are isolated from downstream mutations
         if (entity != null) {
            entity = copy(entity);
         }
         return Optional.ofNullable(entity);
      }

      protected abstract ID getId(T entity);

      @Nonnull
      @Override
      public final <T1 extends T> T1 save(@Nonnull final T1 entity) {
         requireNonNull(entity, "entity");
         // Save a copy to we are insulated from changes to the given entity
         // object.
         entities.put(getId(entity), copy(entity));
         return entity;
      }

      @Nonnull
      @Override
      public final <T1 extends T> Iterable<T1> saveAll(
              @Nonnull final Iterable<T1> entities) {
         requireNonNull(entities, "entities");
         for (final T1 entity : entities) {
            save(entity);
         }
         return entities;
      }

   }

   public static <T, ID> void assertInvariants(
            final CrudRepository<T, ID> repository) {
      // Do nothing
   }

}
