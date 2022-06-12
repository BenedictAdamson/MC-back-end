package uk.badamson.mc.repository;
/*
 * © Copyright Benedict Adamson 2022.
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
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.Objects;
import java.util.Optional;

public abstract class RepositoryAdapter<KEY, VALUE> implements KeyValueRepository<KEY, VALUE> {

    private final CrudRepository<VALUE, KEY> springRepository;

    protected RepositoryAdapter(@Nonnull CrudRepository<VALUE, KEY> springRepository) {
        this.springRepository = Objects.requireNonNull(springRepository, "springRepository");
    }

    @OverridingMethodsMustInvokeSuper
    @Nonnull
    protected CrudRepository<VALUE, KEY> getSpringRepository() {
        return springRepository;
    }

    @Override
    public final void save(@Nonnull KEY id, @Nonnull VALUE entity) {
        getSpringRepository().save(entity);
    }

    @Nonnull
    @Override
    public final Optional<VALUE> find(@Nonnull KEY id) {
        return getSpringRepository().findById(id);
    }

    @Override
    public final boolean exists(@Nonnull KEY id) {
        return getSpringRepository().existsById(id);
    }

    @Nonnull
    @Override
    public final Iterable<VALUE> findAll() {
        return getSpringRepository().findAll();
    }

    @Override
    public final long count() {
        return getSpringRepository().count();
    }

    @Override
    public final void delete(@Nonnull KEY id) {
        getSpringRepository().deleteById(id);
    }

    @Override
    public void deleteAll() {
        getSpringRepository().deleteAll();
    }
}
