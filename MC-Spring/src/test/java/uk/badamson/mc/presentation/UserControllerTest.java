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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriTemplate;
import uk.badamson.mc.Authority;
import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.rest.AuthorityValue;
import uk.badamson.mc.rest.Paths;
import uk.badamson.mc.rest.UserDetailsRequest;
import uk.badamson.mc.rest.UserResponse;
import uk.badamson.mc.spring.SpringAuthority;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UserControllerTest extends ControllerTest {
    private static final UriTemplate USER_URI_TEMPLATE = new UriTemplate(Paths.USER_PATH_PATTERN);

    private final UserController userController = new UserController(userService);

    @Nonnull
    private static UUID parseUserUri(@Nonnull URI uri) {
        return UUID.fromString(USER_URI_TEMPLATE.match(uri.getPath()).get("id"));
    }

    @Nonnull
    private static UUID getUserFromLocationHeader(@Nonnull ResponseEntity<?> response) {
        try {
            return parseUserUri(Objects.requireNonNull(response.getHeaders().getLocation()));
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void getAll() {
        Stream<UserResponse> response = userController.getAll();

        assertThat(response, notNullValue());
        Collection<UserResponse> all = response.toList();
        assertThat(all, not(empty()));
    }

    @Nested
    public class AddUser {

        @Test
        public void exists() {
            final var existingUserDetails = createBasicUserDetails(Authority.ALL);
            userService.add(existingUserDetails);
            final var userName = existingUserDetails.getUsername();
            final var request = new UserDetailsRequest(userName, "password", Set.of(), true, true, true, true);

            final var exception = assertThrows(ResponseStatusException.class, () -> addUser(request));

            assertThat(exception.getStatusCode(), is(HttpStatus.CONFLICT));
        }

        @Test
        public void administrator() {
            final var request = new UserDetailsRequest(
                    BasicUserDetails.ADMINISTRATOR_USERNAME, "password",
                    Set.of(), true, true, true, true
            );

            final var exception = assertThrows(ResponseStatusException.class, () -> addUser(request));

            assertThat(exception.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        }

        private ResponseEntity<Void> addUser(
                final UserDetailsRequest detailsOfUserToAdd
        ) {
            final var response = userController.addUser(detailsOfUserToAdd);
            assertThat(response, notNullValue());
            return response;
        }

        @Nested
        public class ValidRequest {
            @Test
            public void a() {
                test(new UserDetailsRequest(createUserName(), "password", Set.of(), true, true, true, true));
            }

            @Test
            public void b() {
                test(new UserDetailsRequest(createUserName(), "letMeIn", EnumSet.of(AuthorityValue.ROLE_MANAGE_GAMES), false, false, false, false));
            }

            private void test(UserDetailsRequest detailsOfUserToAdd) {
                final var response = addUser(detailsOfUserToAdd);

                final var userId = getUserFromLocationHeader(response);
                final var user = userService.getUser(userId).orElseThrow();
                assertThat(user.getAuthorities(), is(AuthorityValue.convertFromValue(detailsOfUserToAdd.authorities())));
                assertThat(user.getUsername(), is(detailsOfUserToAdd.username()));
            }
        }
    }

    @Nested
    public class GetSelf {
        @Test
        public void a() {
            test(createSpringUser(Authority.ALL));
        }

        @Test
        public void b() {
            test(createSpringUser(EnumSet.of(Authority.ROLE_MANAGE_GAMES)));
        }

        private void test(final SpringUser requestingUser) {
            Set<AuthorityValue> expectedResponseAuthorities = AuthorityValue.convertToValue(SpringAuthority.convertFromSpring(requestingUser.getAuthorities()));

            UserResponse response = userController.getSelf(requestingUser);

            assertThat(response, notNullValue());
            assertThat(response.id(), is(requestingUser.getId()));
            assertThat(response.authorities(), is(expectedResponseAuthorities));
            assertThat(response.username(), is(requestingUser.getUsername()));
        }
    }

    @Nested
    public class GetUser {
        @Test
        public void unknownUser() {
            final var id = UUID.randomUUID();

            final var exception = assertThrows(ResponseStatusException.class, () -> test(id));

            assertThat(exception.getStatusCode(), is(HttpStatus.NOT_FOUND));
        }

        @Test
        public void knownUser() {
            final var id = userService.add(createBasicUserDetails(Authority.ALL)).getId();

            test(id);
        }

        private void test(final UUID id) {
            final UserResponse response = userController.getUser(id);

            assertThat(response, notNullValue());
            assertThat(response.id(), is(id));
        }
    }

}
