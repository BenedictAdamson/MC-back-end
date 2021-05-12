package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2019-20.
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
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import uk.badamson.mc.Authority;
import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.User;

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link UserService}
 * interface.
 * </p>
 */
public class UserServiceTest {

   public static User add(final UserService service,
            final BasicUserDetails userDetails) {
      final var user = service.add(userDetails);

      assertInvariants(service);
      assertNotNull(user, "user");// guard
      assertAll("Attributes",
               () -> assertEquals(userDetails.getAuthorities(),
                        user.getAuthorities(), "authorities"),
               () -> assertEquals(userDetails.getUsername(), user.getUsername(),
                        "username"),
               () -> assertTrue(
                        service.getPasswordEncoder().matches(
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

      try {
         service.loadUserByUsername(userDetails.getUsername());
      } catch (final UsernameNotFoundException e) {
         throw new AssertionError(
                  "Can subsequently load the user using the username", e);
      }

      return user;
   }

   public static void add_1(final UserService service,
            final BasicUserDetails userDetails) {
      final var user = add(service, userDetails);

      final var users = service.getUsers();
      final UserDetails userDetailsAfter = loadUserByUsername(service,
               userDetails.getUsername());
      final var usersList = users.collect(toList());
      assertThat(
               "A subsequently retrieved sequence of the users will include a user equivalent to the returned user.",
               usersList, hasItem(user));
      assertAll(
               "Subsequently finding user details using the username of the given user will retrieve user details "
                        + "equivalent to the user details of the given user.",
               () -> assertThat("authorities",
                        userDetailsAfter.getAuthorities(),
                        is(userDetails.getAuthorities())),
               () -> assertThat("username", userDetailsAfter.getUsername(),
                        is(userDetails.getUsername())),
               () -> assertThat("accountNonExpired",
                        userDetailsAfter.isAccountNonExpired(),
                        is(userDetails.isAccountNonExpired())),
               () -> assertThat("accountNonLocked",
                        userDetailsAfter.isAccountNonLocked(),
                        is(userDetails.isAccountNonLocked())),
               () -> assertThat("credentialsNonExpired",
                        userDetailsAfter.isCredentialsNonExpired(),
                        is(userDetails.isCredentialsNonExpired())),
               () -> assertThat("enabled", userDetailsAfter.isEnabled(),
                        is(userDetails.isEnabled())),
               () -> assertTrue(
                        service.getPasswordEncoder().matches(
                                 userDetails.getPassword(),
                                 userDetailsAfter.getPassword()),
                        "Recorded password has been encrypted using the pasword encoder of this service."));
   }

   public static void add_2(final UserService service, final User userDetails1,
            final User userDetails2) {
      final var user1 = add(service, userDetails1);
      final var user2 = add(service, userDetails2);

      final var users = service.getUsers();
      final var usersList = users.collect(toList());
      assertThat(
               "A subsequently retrieved sequence of the users will include a user equivalent to the returned user [1].",
               usersList, hasItem(user1));
      assertThat(
               "A subsequently retrieved sequence of the users will include a user equivalent to the returned user [2].",
               usersList, hasItem(user2));
   }

   public static void assertInvariants(final UserService service) {
      assertNotNull(service.getPasswordEncoder(), "Not null, passwordEncoder");
   }

   public static Optional<User> getUser(final UserService service,
            final UUID id) {
      final var result = service.getUser(id);

      assertInvariants(service);
      assertTrue(result.isEmpty() || id.equals(result.get().getId()),
               "Returns either an empty value, "
                        + "or a value for which the identifier is equivalent to the given ID");
      return result;
   }

   public static User getUser_administrator(final UserService service) {
      final var result = getUser(service, User.ADMINISTRATOR_ID);

      assertThat("Can always get user information for the administrator.",
               result.isPresent());// guard
      final var administrator = result.get();
      assertThat("Administrator has full authority",
               administrator.getAuthorities(), is(Authority.ALL));
      return administrator;
   }

   public static Stream<User> getUsers(final UserService service) {
      final var users = service.getUsers();

      assertInvariants(service);
      assertNotNull(users, "Always returns a (non null) stream.");// guard
      final var usersList = users.collect(toList());
      assertThat("The sequence of users has no null elements",
               !usersList.stream().anyMatch(user -> user == null));// guard
      final var userNames = usersList.stream().map(user -> user.getUsername())
               .collect(toUnmodifiableSet());
      assertThat("The list of users always has an administrator.", userNames,
               hasItem(BasicUserDetails.ADMINISTRATOR_USERNAME));
      assertEquals(userNames.size(), usersList.size(),
               "Does not contain users with duplicate usernames.");

      return usersList.stream();
   }

   public static User loadUserByUsername(final UserService service,
            final String username) throws UsernameNotFoundException {
      final User user;
      try {
         user = service.loadUserByUsername(username);
      } catch (final UsernameNotFoundException e) {
         assertInvariants(service);
         throw e;
      }

      assertInvariants(service);
      assertNotNull(user, "Non null user");// guard

      return user;
   }

   public static User loadUserByUsername_administrator(
            final UserService service) {
      final User user;
      try {
         user = loadUserByUsername(service,
                  BasicUserDetails.ADMINISTRATOR_USERNAME);
      } catch (final UsernameNotFoundException e) {
         throw new AssertionError(
                  "Always have user details for the administrator.", e);
      }
      assertThat("The administrator has a complete set of authorities.",
               user.getAuthorities(), is(Authority.ALL));
      return user;
   }
}
