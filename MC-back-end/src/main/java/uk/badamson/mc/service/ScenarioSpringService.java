package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2020,22.
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

import org.springframework.stereotype.Service;
import uk.badamson.mc.NamedUUID;
import uk.badamson.mc.Scenario;
import uk.badamson.mc.repository.MCRepository;
import uk.badamson.mc.repository.MCSpringRepositoryAdapter;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ScenarioSpringService {

    private final ScenarioService delegate;

    public ScenarioSpringService(@Nonnull MCSpringRepositoryAdapter repository) {
        delegate = new ScenarioService(repository);
    }

    @Nonnull
    final ScenarioService getDelegate() {
        return delegate;
    }

    @Nonnull
    public Stream<NamedUUID> getNamedScenarioIdentifiers() {
        return delegate.getNamedScenarioIdentifiers().stream();
    }

    @Nonnull
    public Optional<Scenario> getScenario(@Nonnull final UUID id) {
        return delegate.getScenario(id);
    }

    @Nonnull
    public Stream<UUID> getScenarioIdentifiers() {
        return delegate.getScenarioIdentifiers();
    }

}
