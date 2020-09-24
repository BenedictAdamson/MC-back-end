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
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Stream;

import org.springframework.security.core.userdetails.UserDetails;

import uk.badamson.mc.Authority;
import uk.badamson.mc.User;

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link Service} interface.
 * </p>
 */
public class ServiceTest {

   public static void add(final Service service, final User user) {
      service.add(user);

      assertInvariants(service);
   }

   public static void add_1(final Service service, final User user) {
      add(service, user);
      final Stream<User> users = service.getUsers();
      final UserDetails userDetails = loadUserByUsername(service,
               user.getUsername());
      final var usersList = users.collect(toList());
      assertThat(
               "A subsequently retrieved sequence of the users will include a user equivalent to the given user.",
               usersList, hasItem(user));
      assertEquals(user, userDetails,
               "Subsequently finding user details using the username of the given user will retrieve user details equivalent to the user details of the given user.");
      assertTrue(
               service.getPasswordEncoder().matches(user.getPassword(),
                        userDetails.getPassword()),
               "Recorded password has been encrypted using the pasword encoder of this service.");
   }

   public static void add_2(final Service service, final User user1,
            final User user2) {
      add(service, user1);
      add(service, user2);
      final Stream<User> users = service.getUsers();
      final var usersList = users.collect(toList());
      assertThat(
               "A subsequently retrieved sequence of the users will include a user equivalent to the given user [1].",
               usersList, hasItem(user1));
      assertThat(
               "A subsequently retrieved sequence of the users will include a user equivalent to the given user [2].",
               usersList, hasItem(user2));
   }

   public static void assertInvariants(final Service service) {
      // Do nothing
   }

   public static Stream<User> getUsers(final Service service) {
      final Stream<User> users = service.getUsers();

      assertInvariants(service);
      assertNotNull(users, "Always returns a (non null) stream.");// guard
      final var usersList = users.collect(toList());
      final Set<String> userNames = usersList.stream()
               .map(user -> user.getUsername()).collect(toUnmodifiableSet());
      assertThat("The list of users always has an administrator.", userNames,
               hasItem(User.ADMINISTRATOR_USERNAME));
      assertEquals(userNames.size(), usersList.size(),
               "Does not contain users with duplicate usernames.");

      return usersList.stream();
   }

   public static UserDetails loadUserByUsername(final Service service,
            final String username) {
      final boolean administrator = User.ADMINISTRATOR_USERNAME
               .equals(username);

      final UserDetails userDetails = service.loadUserByUsername(username);

      assertInvariants(service);
      assertNotNull(userDetails, "Non null user");

      assertTrue(!(administrator && userDetails == null),
               "Always have user details for the administrator.");
      if (userDetails != null) {
         assertThat("The administrator has a complete set of authorities.",
                  userDetails.getAuthorities(),
                  administrator ? is(Authority.ALL) : anything());
      }
      return userDetails;
   }
}
