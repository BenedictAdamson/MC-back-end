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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import uk.badamson.mc.Authority;
import uk.badamson.mc.User;
import uk.badamson.mc.repository.UserRepository;
import uk.badamson.mc.repository.UserRepositoryTest;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link ServiceImpl} class.
 * </p>
 */
public class ServiceImplTest {

   @Nested
   public class Add_User {

      @Nested
      public class One {

         @Test
         public void a() {
            test(userA, passwordEncoderA);
         }

         @Test
         public void b() {
            test(userB, passwordEncoderB);
         }

         private void test(final User user,
                  final PasswordEncoder passwordEncoder) {
            final var service = new ServiceImpl(passwordEncoder,
                     userRepositoryA, PASSWORD_A);
            ServiceTest.add_1(service, user);
            assertThat("Added user", service.getUsers().count(), is(2L));
         }
      }// class

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
            final var service = new ServiceImpl(passwordEncoderA,
                     userRepositoryA, PASSWORD_A);
            ServiceTest.add_2(service, user1, user2);
            assertThat("Added user", service.getUsers().count(), is(3L));
         }
      }// class
   }// class

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
   }// class

   @Nested
   public class Scenario {

      private ServiceImpl service;
      private Collection<User> users;

      @Test
      public void get_users_of_fresh_instance() {
         given_a_fresh_instance_of_MC();
         when_getting_the_users();
         assertAll(() -> then_the_list_of_users_has_one_user(),
                  () -> then_the_list_of_users_includes_a_user_named_Administrator());
      }

      private void given_a_fresh_instance_of_MC() {
         service = new ServiceImpl(passwordEncoderA, userRepositoryA,
                  PASSWORD_A);
      }

      private void then_the_list_of_users_has_one_user() {
         assertThat(users.size(), is(1));
      }

      private void then_the_list_of_users_includes_a_user_named_Administrator() {
         assertThat(users, not(hasItem((User) null)));// guard
         final Set<String> usernames = users.stream().map(u -> u.getUsername())
                  .collect(toUnmodifiableSet());
         assertThat("the list of users includes a user named \"Administrator\"",
                  usernames, hasItem(User.ADMINISTRATOR_USERNAME));
      }

      private void when_getting_the_users() {
         users = getUsers(service).collect(toList());
      }
   }// class

   private static final String PASSWORD_A = "letmein";

   private static final String PASSWORD_B = "password123";

   private static final String PASSWORD_C = "secret";

   public static void assertInvariants(final ServiceImpl service) {
      ServiceTest.assertInvariants(service);// inherited

      final UserRepository userRepository = service.getUserRepository();
      assertNotNull(userRepository,
               "Always have a (non null) user repository.");
      UserRepositoryTest.assertInvariants(userRepository);
   }

   private static ServiceImpl constructor(final PasswordEncoder passwordEncoder,
            final UserRepository userRepository,
            final String administratorPassword) {
      final var service = new ServiceImpl(passwordEncoder, userRepository,
               administratorPassword);

      assertInvariants(service);
      assertSame(userRepository, service.getUserRepository(),
               "The user repository of this service is the given user repository.");
      assertSame(passwordEncoder, service.getPasswordEncoder(),
               "The password encoder of this service is the given password encoder.");
      getUsers(service);
      final var encryptedAdminPassword = loadUserByUsername(service,
               User.ADMINISTRATOR_USERNAME).getPassword();
      assertTrue(
               passwordEncoder.matches(administratorPassword,
                        encryptedAdminPassword),
               "The password of the administrator user details found through this service is equal to the given administrator password encrypted by the given password encoder.");

      return service;
   }

   public static Stream<User> getUsers(final ServiceImpl service) {
      final var users = ServiceTest.getUsers(service);// inherited

      assertInvariants(service);

      return users;
   }

   public static UserDetails loadUserByUsername(final ServiceImpl service,
            final String username) {
      final UserDetails user = ServiceTest.loadUserByUsername(service,
               username);

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
      final var passwordEncoder = passwordEncoderA;
      final var repository = userRepositoryA;
      final var service = new ServiceImpl(passwordEncoder, repository,
               PASSWORD_A);
      repository.save(new User(User.ADMINISTRATOR_USERNAME,
               passwordEncoder.encode(PASSWORD_B), Authority.ALL, true, true, true, true));

      getUsers(service);
   }

   @BeforeEach
   public void setUpRepositories() {
      userRepositoryA = new UserRepositoryTest.Fake();
      userRepositoryB = new UserRepositoryTest.Fake();
   }

   @BeforeEach
   public void setUpUsers() {
      userA = new User("John", PASSWORD_A, Set.of(), true, true, true, true);
      userB = new User("Alan", PASSWORD_B, Authority.ALL, true, true, true, true);
      userC = new User("Gweezer", PASSWORD_C, Set.of(), true, true, true, true);
   }
}
