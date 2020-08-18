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

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.badamson.mc.Authority;
import uk.badamson.mc.User;

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link Service} interface.
 * </p>
 */
public class ServiceTest {

   public static Mono<Void> add(final Service service, final User user) {
      final var publisher = service.add(user);

      assertInvariants(service);
      assertNotNull(publisher, "Always returns a (non null) publisher.");

      return publisher;
   }

   public static void add_1(final Service service, final User user) {
      add(service, user).block();
      final Flux<User> users = service.getUsers();
      final UserDetails userDetails = findByUsername(service,
               user.getUsername()).block();
      StepVerifier.create(users.filter(p -> user.equals(p)))
               .expectNext(user)
               .as("A subsequently retrieved sequence of the users will include a user equivalent to the given user.")
               .verifyComplete();
      assertEquals(user, userDetails,
               "Subsequently finding user details using the username of the given user will retrieve user details equivalent to the user details of the given user.");
      assertTrue(
               service.getPasswordEncoder().matches(user.getPassword(),
                        userDetails.getPassword()),
               "Recorded password has been encrypted using the pasword encoder of this service.");
   }

   public static void add_2(final Service service, final User user1,
            final User user2) {
      add(service, user1).block();
      add(service, user2).block();
      final Flux<User> users = service.getUsers();
      assertTrue(users.hasElement(user1).block(),
               "A subsequently retrieved sequence of the users will include a user equivalent to the given user [1].");
      assertTrue(users.hasElement(user2).block(),
               "A subsequently retrieved sequence of the users will include a user equivalent to the given user [2].");
   }

   public static void assertInvariants(final Service service) {
      // Do nothing
   }

   public static Mono<UserDetails> findByUsername(final Service service,
            final String username) {
      final var administrator = User.ADMINISTRATOR_USERNAME.equals(username);

      final var publisher = service.findByUsername(username);

      assertInvariants(service);
      assertNotNull(publisher, "Non null user details publisher");
      assertNotNull(service.getPasswordEncoder(),
               "Always have a (non-null) password encoder");

      final var userDetails = publisher.block();
      assertTrue(!(administrator && userDetails == null),
               "Always have user details for the administrator.");
      if (userDetails != null) {
         assertThat("The administrator has a complete set of authorities.",
                  userDetails.getAuthorities(),
                  administrator ? is(Authority.ALL) : anything());
      }

      return publisher;
   }

   public static Flux<User> getUsers(final Service service) {
      final Flux<User> users = service.getUsers();

      assertInvariants(service);
      assertNotNull(users, "Always returns a (non null) publisher.");
      assertTrue(users
               .any(p -> User.ADMINISTRATOR_USERNAME.equals(p.getUsername()))
               .block(), "The list of users always has an administrator.");
      final List<User> usersList = users.collectList().block();
      final Set<String> userNames = usersList.stream()
               .map(user -> user.getUsername())
               .collect(Collectors.toUnmodifiableSet());
      assertEquals(userNames.size(), usersList.size(),
               "Does not contain users with duplicate usernames.");

      return users;
   }
}
