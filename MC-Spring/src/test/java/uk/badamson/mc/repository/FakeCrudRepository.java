package uk.badamson.mc.repository;
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

import org.springframework.data.repository.CrudRepository;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

abstract class FakeCrudRepository<T, ID> implements CrudRepository<T, ID> {
    private final Map<ID, T> data = new ConcurrentHashMap<>();

    private static <ID> @Nonnull Set<ID> createSet(@Nonnull Iterable<ID> ids) {
        return StreamSupport.stream(ids.spliterator(), false).collect(Collectors.toSet());
    }

    @Nonnull
    @Override
    final public <S extends T> S save(@Nonnull S entity) {
        data.put(idOf(entity), entity);
        return entity;
    }

    @Nonnull
    @Override
    public final <S extends T> Iterable<S> saveAll(@Nonnull Iterable<S> entities) {
        for (var entity : entities) {
            save(entity);
        }
        return entities;
    }

    @Nonnull
    @Override
    public final Optional<T> findById(@Nonnull ID id) {
        return Optional.of(data.get(id));
    }

    @Override
    public boolean existsById(@Nonnull ID id) {
        return data.containsKey(id);
    }

    @Nonnull
    @Override
    public Iterable<T> findAll() {
        return List.copyOf(data.values());
    }

    @Nonnull
    @Override
    public final Iterable<T> findAllById(@Nonnull Iterable<ID> ids) {
        final Set<ID> keys = createSet(ids);
        return data.entrySet().stream()
                .filter(e -> keys.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .toList();
    }

    @Override
    public final long count() {
        return data.size();
    }

    @Override
    public final void deleteById(@Nonnull ID id) {
        data.remove(id);
    }

    @Override
    public final void delete(@Nonnull T entity) {
        deleteById(idOf(entity));
    }

    @Override
    public final void deleteAllById(@Nonnull Iterable<? extends ID> ids) {
        createSet(ids).forEach(this::deleteById);
    }

    @Override
    public final void deleteAll(@Nonnull Iterable<? extends T> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public final void deleteAll() {
        data.clear();
    }

    @Nonnull
    protected final Stream<T> entityStream() {
        return data.values().stream();
    }

    @Nonnull
    protected abstract ID idOf(@Nonnull T entity);
}
