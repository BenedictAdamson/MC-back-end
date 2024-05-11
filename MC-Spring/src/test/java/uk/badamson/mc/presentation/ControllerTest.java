package uk.badamson.mc.presentation;
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

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.AbstractPasswordEncoder;
import org.springframework.web.util.UriTemplate;
import uk.badamson.mc.Authority;
import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.repository.*;
import uk.badamson.mc.rest.Paths;
import uk.badamson.mc.service.GameSpringService;
import uk.badamson.mc.service.ScenarioSpringService;
import uk.badamson.mc.service.UserSpringService;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

abstract class ControllerTest {
    private static final Instant NOW = Instant.now();
    private static final String ADMINISTRATOR_PASSWORD = "letMeIn";

    private static final UriTemplate GAME_URI_TEMPLATE = new UriTemplate(Paths.GAME_PATH_PATTERN);

    private final Clock clock = Clock.fixed(NOW, ZoneId.systemDefault());

    private final CurrentUserGameSpringRepository currentUserGameRepository = new FakeCurrentUserGameSpringRepository();
    private final GameSpringRepository gameRepository = new FakeGameSpringRepository();
    private final UserSpringRepository userRepository = new FakeUserSpringRepository();
    private final MCSpringRepositoryAdapter repository = new MCSpringRepositoryAdapter(currentUserGameRepository, gameRepository, userRepository);
    protected final ScenarioSpringService scenarioService = new ScenarioSpringService(repository);
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder = new AbstractPasswordEncoder() {
        @Override
        protected byte[] encode(CharSequence rawPassword, byte[] salt) {
            return rawPassword.toString().getBytes(StandardCharsets.UTF_8);
        }
    };
    protected final UserSpringService userService = new UserSpringService(passwordEncoder, ADMINISTRATOR_PASSWORD, repository);
    protected final GameSpringService gameService = new GameSpringService(clock, scenarioService, userService, repository);

    @Nonnull
    protected static BasicUserDetails createBasicUserDetails(final Set<Authority> authorities) {
        return new BasicUserDetails(createUserName(), "secret",
                authorities,
                true, true, true, true);
    }

    protected static String createUserName() {
        return "jeff-" + UUID.randomUUID();
    }

    @Nonnull
    private static UUID parseGameUri(@Nonnull URI uri) {
        return UUID.fromString(GAME_URI_TEMPLATE.match(uri.getPath()).get("game"));
    }

    @Nonnull
    protected static UUID getGameFromLocationHeader(@Nonnull ResponseEntity<?> response) {
        try {
            return parseGameUri(Objects.requireNonNull(response.getHeaders().getLocation()));
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }

    }

    @Nonnull
    protected final UUID getValidScenarioId() {
        return scenarioService.getScenarioIdentifiers().findAny().orElseThrow();
    }

    @Nonnull
    protected final UUID createGame() {
        return gameService.create(getValidScenarioId()).getIdentifier();
    }

    @Nonnull
    protected final SpringUser createSpringUser(Set<Authority> authorities) {
        return SpringUser.convertToSpring(userService.add(createBasicUserDetails(authorities)));
    }

}
