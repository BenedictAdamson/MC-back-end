package uk.badamson.mc.service;
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
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import uk.badamson.dbc.assertions.ObjectVerifier;
import uk.badamson.mc.Authority;
import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.User;
import uk.badamson.mc.repository.UserRepository;
import uk.badamson.mc.repository.UserRepositoryTest;

public class UserServiceImplTest {

   @Nested
   public class Add {

      @Nested
      public class AlreadyExists {

         @Test
         public void a() {
            test(userA);
         }

         @Test
         public void b() {
            test(userB);
         }

         private void test(final User user) {
            final var service = new UserServiceImpl(passwordEncoderA,
                     userRepositoryA, PASSWORD_A);
            add(service, user);
            assertThrows(UserExistsException.class,
                     () -> UserServiceTest.add(service, user));
         }

      }

      @Nested
      public class One {

         @Test
         public void accountNonExpired() {
            final var accountNonExpired = false;
            final var accountNonLocked = true;
            final var credentialsNonExpired = true;
            final var enabled = true;
            final var user = new BasicUserDetails(USERNAME_A, PASSWORD_A,
                     Authority.ALL, accountNonExpired, accountNonLocked,
                     credentialsNonExpired, enabled);

            test(user, passwordEncoderA);
         }

         @Test
         public void accountNonLocked() {
            final var accountNonExpired = true;
            final var accountNonLocked = false;
            final var credentialsNonExpired = true;
            final var enabled = true;
            final var user = new BasicUserDetails(USERNAME_A, PASSWORD_A,
                     Authority.ALL, accountNonExpired, accountNonLocked,
                     credentialsNonExpired, enabled);

            test(user, passwordEncoderA);
         }

         @Test
         public void authorities() {
            final var accountNonExpired = true;
            final var accountNonLocked = true;
            final var credentialsNonExpired = true;
            final var enabled = true;
            final var user = new BasicUserDetails(USERNAME_A, PASSWORD_A,
                     Set.of(Authority.ROLE_PLAYER), accountNonExpired,
                     accountNonLocked, credentialsNonExpired, enabled);

            test(user, passwordEncoderA);
         }

         @Test
         public void base() {
            final var accountNonExpired = true;
            final var accountNonLocked = true;
            final var credentialsNonExpired = true;
            final var enabled = true;
            final var user = new BasicUserDetails(USERNAME_A, PASSWORD_A,
                     Authority.ALL, accountNonExpired, accountNonLocked,
                     credentialsNonExpired, enabled);

            test(user, passwordEncoderA);
         }

         @Test
         public void credentialsNonExpired() {
            final var accountNonExpired = true;
            final var accountNonLocked = true;
            final var credentialsNonExpired = false;
            final var enabled = true;
            final var user = new BasicUserDetails(USERNAME_A, PASSWORD_A,
                     Authority.ALL, accountNonExpired, accountNonLocked,
                     credentialsNonExpired, enabled);

            test(user, passwordEncoderA);
         }

         @Test
         public void enabled() {
            final var accountNonExpired = true;
            final var accountNonLocked = true;
            final var credentialsNonExpired = true;
            final var enabled = false;
            final var user = new BasicUserDetails(USERNAME_A, PASSWORD_A,
                     Authority.ALL, accountNonExpired, accountNonLocked,
                     credentialsNonExpired, enabled);

            test(user, passwordEncoderA);
         }

         @Test
         public void password() {
            final var accountNonExpired = true;
            final var accountNonLocked = true;
            final var credentialsNonExpired = true;
            final var enabled = true;
            final var user = new BasicUserDetails(USERNAME_A, PASSWORD_B,
                     Authority.ALL, accountNonExpired, accountNonLocked,
                     credentialsNonExpired, enabled);

            test(user, passwordEncoderA);
         }

         private void test(final BasicUserDetails userDetails,
                  final PasswordEncoder passwordEncoder) {
            final var service = new UserServiceImpl(passwordEncoder,
                     userRepositoryA, PASSWORD_A);

            UserServiceTest.add_1(service, userDetails);

            assertThat("Added a user", service.getUsers().count(), is(2L));
         }

         @Test
         public void username() {
            final var accountNonExpired = true;
            final var accountNonLocked = true;
            final var credentialsNonExpired = true;
            final var enabled = true;
            final var user = new BasicUserDetails(USERNAME_B, PASSWORD_A,
                     Authority.ALL, accountNonExpired, accountNonLocked,
                     credentialsNonExpired, enabled);

            test(user, passwordEncoderA);
         }

         @Test
         public void usesPasswordEncoder() {
            test(userA, passwordEncoderB);
         }
      }

      @Nested
      public class Two {

         @Test
         public void a() {
            test(userA, userB);
         }

         @Test
         public void b() {
            test(userB, userC);
         }

         private void test(final User user1, final User user2) {
            final var service = new UserServiceImpl(passwordEncoderA,
                     userRepositoryA, PASSWORD_A);
            UserServiceTest.add_2(service, user1, user2);
            assertThat("Added user", service.getUsers().count(), is(3L));
         }
      }
   }

   @Nested
   public class Constructor {

      @Test
      public void a() {
         constructor(passwordEncoderA, userRepositoryA, PASSWORD_A);
      }

      @Test
      public void b() {
         constructor(passwordEncoderB, userRepositoryB, PASSWORD_B);
      }
   }

   @Nested
   public class GetUser {

      @Test
      public void absent() {
         final var service = new UserServiceImpl(passwordEncoderA,
                  userRepositoryA, PASSWORD_A);

         final var result = getUser(service, USER_ID_A);

         assertTrue(result.isEmpty(), "empty");
      }

      @Test
      public void administrator() {
         final var service = new UserServiceImpl(passwordEncoderA,
                  userRepositoryA, PASSWORD_A);

         getUser_administrator(service);
      }

      @Test
      public void present() {
         final var userDetails = new BasicUserDetails(USERNAME_A, PASSWORD_A,
                  Authority.ALL, true, true, true, true);
         final var service = new UserServiceImpl(passwordEncoderA,
                  userRepositoryA, PASSWORD_A);
         final var user = service.add(userDetails);

         final var result = getUser(service, user.getId());

         assertTrue(result.isPresent(), "present");
      }
   }

   @Nested
   public class LoadUserByUserName {

      @Test
      public void absent() {
         final var service = new UserServiceImpl(passwordEncoderA,
                  userRepositoryA, PASSWORD_A);

         assertThrows(UsernameNotFoundException.class,
                  () -> loadUserByUsername(service, USERNAME_A));
      }

      @Test
      public void administrator() {
         final var service = new UserServiceImpl(passwordEncoderA,
                  userRepositoryA, PASSWORD_A);

         loadUserByUsername_administrator(service);
      }

      @Test
      public void present() {
         final var userDetails = new BasicUserDetails(USERNAME_A, PASSWORD_A,
                  Authority.ALL, true, true, true, true);
         final var service = new UserServiceImpl(passwordEncoderA,
                  userRepositoryA, PASSWORD_A);
         final var user = service.add(userDetails);

         loadUserByUsername(service, user.getUsername());
      }

   }

   @Nested
   public class Scenario {

      private UserServiceImpl service;
      private Collection<User> users;

      @Test
      public void get_users_of_fresh_instance() {
         given_a_fresh_instance_of_MC();
         when_getting_the_users();
         assertAll(this::then_the_list_of_users_has_one_user,
                 this::then_the_list_of_users_includes_a_user_named_Administrator);
      }

      private void given_a_fresh_instance_of_MC() {
         service = new UserServiceImpl(passwordEncoderA, userRepositoryA,
                  PASSWORD_A);
      }

      private void then_the_list_of_users_has_one_user() {
         assertThat(users.size(), is(1));
      }

      private void then_the_list_of_users_includes_a_user_named_Administrator() {
         assertThat(users, not(hasItem((User) null)));// guard
         final var usernames = users.stream().map(BasicUserDetails::getUsername)
                  .collect(toUnmodifiableSet());
         assertThat("the list of users includes a user named \"Administrator\"",
                  usernames, hasItem(BasicUserDetails.ADMINISTRATOR_USERNAME));
      }

      private void when_getting_the_users() {
         users = getUsers(service).collect(toList());
      }
   }

   private static final String USERNAME_C = "Gweezer";

   private static final String USERNAME_B = "Alan";

   private static final String USERNAME_A = "John";

   private static final String PASSWORD_A = "letmein";

   private static final String PASSWORD_B = "password123";

   private static final String PASSWORD_C = "secret";

   private static final UUID USER_ID_A = UUID.randomUUID();

   public static User add(final UserServiceImpl service,
            final BasicUserDetails userDetails) {
      final var user = UserServiceTest.add(service, userDetails);// inherited

      assertInvariants(service);

      return user;
   }

   public static void assertInvariants(final UserServiceImpl service) {
      ObjectVerifier.assertInvariants(service);// inherited
      UserServiceTest.assertInvariants(service);// inherited

      final var userRepository = service.getUserRepository();
      assertNotNull(userRepository,
               "Always have a (non null) user repository.");
      UserRepositoryTest.assertInvariants(userRepository);
   }

   private static void constructor(
            final PasswordEncoder passwordEncoder,
            final UserRepository userRepository,
            final String administratorPassword) {
      final var service = new UserServiceImpl(passwordEncoder, userRepository,
               administratorPassword);

      assertInvariants(service);
      assertSame(userRepository, service.getUserRepository(),
               "The user repository of this service is the given user repository.");
      assertSame(passwordEncoder, service.getPasswordEncoder(),
               "The password encoder of this service is the given password encoder.");
      getUsers(service);
      final var encryptedAdminPassword = loadUserByUsername(service,
               BasicUserDetails.ADMINISTRATOR_USERNAME).getPassword();
      assertTrue(
               passwordEncoder.matches(administratorPassword,
                        encryptedAdminPassword),
               "The password of the administrator user details found through this service is equal to the given administrator password encrypted by the given password encoder.");

   }

   public static Optional<User> getUser(final UserServiceImpl service,
            final UUID id) {
      final var result = UserServiceTest.getUser(service, id);// inherited

      assertInvariants(service);
      return result;
   }

   public static User getUser_administrator(final UserServiceImpl service) {
      final var result = UserServiceTest.getUser_administrator(service);// inherited

      assertInvariants(service);
      return result;
   }

   public static Stream<User> getUsers(final UserServiceImpl service) {
      final var users = UserServiceTest.getUsers(service);// inherited

      assertInvariants(service);

      return users;
   }

   public static UserDetails loadUserByUsername(final UserServiceImpl service,
            final String username) {
      final UserDetails user = UserServiceTest.loadUserByUsername(service,
               username);// inherited

      assertInvariants(service);

      return user;
   }

   public static User loadUserByUsername_administrator(
            final UserServiceImpl service) {
      final var user = UserServiceTest
               .loadUserByUsername_administrator(service);// inherited

      assertInvariants(service);
      return user;
   }

   private final PasswordEncoder passwordEncoderA = new BCryptPasswordEncoder(
            4);

   private final PasswordEncoder passwordEncoderB = new BCryptPasswordEncoder(
            5);

   private UserRepository userRepositoryA;

   private UserRepository userRepositoryB;

   private User userA;

   private User userB;

   private User userC;

   @Test
   public void administratorInRepository() {
      final var repository = userRepositoryA;
      final var service = new UserServiceImpl(passwordEncoderA, repository,
               PASSWORD_A);
      repository.save(User.createAdministrator(PASSWORD_B));

      getUsers(service);
   }

   @BeforeEach
   public void setUpRepositories() {
      userRepositoryA = new UserRepositoryTest.Fake();
      userRepositoryB = new UserRepositoryTest.Fake();
   }

   @BeforeEach
   public void setUpUsers() {
      userA = new User(UUID.randomUUID(), USERNAME_A, PASSWORD_A, Set.of(),
               true, true, true, true);
      userB = new User(UUID.randomUUID(), USERNAME_B, PASSWORD_B, Authority.ALL,
               true, true, true, true);
      userC = new User(UUID.randomUUID(), USERNAME_C, PASSWORD_C, Set.of(),
               true, true, true, true);
   }

}
