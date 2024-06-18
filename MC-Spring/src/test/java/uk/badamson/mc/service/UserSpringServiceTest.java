package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2019-24.
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.badamson.mc.*;
import uk.badamson.mc.rest.UserResponse;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Tag("Spring")
public class UserSpringServiceTest {

    private static final String USERNAME_A = "John";
    private static final String PASSWORD_A = "hello";

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(ProcessFixtures.MONGO_DB_IMAGE);
    @Autowired
    UserSpringService service;
    @Autowired
    org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Nonnull
    private User add(final BasicUserDetails userDetails) {
        final var user = service.add(userDetails);

        assertThat(user, notNullValue());
        assertAll("Attributes",
                () -> assertEquals(userDetails.getAuthorities(),
                        user.getAuthorities(), "authorities"),
                () -> assertEquals(userDetails.getUsername(), user.getUsername(),
                        "username"),
                () -> assertTrue(
                        passwordEncoder.matches(
                                userDetails.getPassword(), user.getPassword()),
                        "password (encrypted)"),
                () -> assertEquals(userDetails.isAccountNonExpired(),
                        user.isAccountNonExpired(), "accountNonExpired"),
                () -> assertEquals(userDetails.isAccountNonLocked(),
                        user.isAccountNonLocked(), "accountNonLocked"),
                () -> assertEquals(userDetails.isCredentialsNonExpired(),
                        user.isCredentialsNonExpired(),
                        "credentialsNonExpired"),
                () -> assertEquals(userDetails.isEnabled(), user.isEnabled(),
                        "enabled"));
        assertDoesNotThrow(() ->
                service.loadUserByUsername(userDetails.getUsername()), "Can subsequently load the user using the username");

        return user;
    }

    @Nonnull
    private Optional<User> getUser(final UUID id) {
        final var result = service.getUser(id);

        assertThat(
                "Returns either an empty value, or a value for which the identifier is equivalent to the given ID",
                result.isEmpty() || id.equals(result.get().getId()));
        return result;
    }

    @Nonnull
    private Stream<UserResponse> getUsers() {
        final var users = service.getUsers();

        assertThat(users, notNullValue());
        final var usersList = users.toList();
        assertThat("The sequence of users has no null elements",
                usersList.stream().noneMatch(Objects::isNull));
        final var userNames = usersList.stream().map(UserResponse::username)
                .collect(toUnmodifiableSet());
        assertThat("The list of users always has an administrator.", userNames,
                hasItem(BasicUserDetails.ADMINISTRATOR_USERNAME));
        assertEquals(userNames.size(), usersList.size(),
                "Does not contain users with duplicate usernames.");

        return usersList.stream();
    }

    @Nonnull
    private SpringUser loadUserByUsername(final String username)
            throws UsernameNotFoundException {
        final var user = service.loadUserByUsername(username);

        assertThat(user, notNullValue());

        return user;
    }

    @Nested
    public class Add {

        @Test
        public void alreadyExists() {
            final BasicUserDetails user = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            add(user);
            assertThrows(UserExistsException.class, () -> add(user));
        }

        @Test
        public void doesNotAlreadyExist() {
            final var userDetails = ProcessFixtures.createBasicUserDetailsWithAllRoles();

            add(userDetails);

            assertDoesNotThrow(
                    () -> loadUserByUsername(userDetails.getUsername()),
                    "Can subsequently retrieve the user by username");
            assertThat("Collections of users includes a user with the given name",
                    getUsers().anyMatch(ur -> ur.username().equals(userDetails.getUsername())));
        }

    }

    @Nested
    public class GetUser {

        @Test
        public void absent() {
            final var id = UUID.randomUUID();

            final var result = getUser(id);

            assertThat(result.isEmpty(), is(true));
        }

        @Test
        public void present() {
            final var userDetails = new BasicUserDetails(USERNAME_A, PASSWORD_A,
                    Authority.ALL, true, true, true, true);
            final var user = add(userDetails);
            final var userId = user.getId();

            final var result = getUser(userId);

            assertThat(result.isPresent(), is(true));
        }
    }

    @Nested
    public class LoadUserByUserName {

        @Test
        public void absent() {
            assertThrows(UsernameNotFoundException.class, () -> loadUserByUsername(USERNAME_A));
        }

        @Test
        public void present() {
            final var userDetails = ProcessFixtures.createBasicUserDetailsWithAllRoles();
            final var userName = userDetails.getUsername();
            add(userDetails);

            assertDoesNotThrow(() -> loadUserByUsername(userName));
        }

    }

}
