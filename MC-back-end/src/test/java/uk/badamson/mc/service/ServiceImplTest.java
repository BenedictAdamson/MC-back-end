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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.badamson.mc.Authority;
import uk.badamson.mc.Player;
import uk.badamson.mc.repository.PlayerRepository;
import uk.badamson.mc.repository.PlayerRepositoryTest;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link ServiceImpl} class.
 * </p>
 */
public class ServiceImplTest {

   @Nested
   public class Add_Player {

      @Nested
      public class One {

         @Test
         public void a() {
            test(playerA, passwordEncoderA);
         }

         @Test
         public void b() {
            test(playerB, passwordEncoderB);
         }

         private void test(final Player player,
                  final PasswordEncoder passwordEncoder) {
            final var service = new ServiceImpl(passwordEncoder,
                     playerRepositoryA, PASSWORD_A);
            ServiceTest.add_1(service, player);
            StepVerifier.create(service.getPlayers()).expectNextCount(2)
                     .verifyComplete();
         }
      }// class

      @Nested
      public class Two {

         @Test
         public void a() {
            test(playerA, playerB);
         }

         @Test
         public void b() {
            test(playerB, playerC);
         }

         private void test(final Player player1, final Player player2) {
            final var service = new ServiceImpl(passwordEncoderA,
                     playerRepositoryA, PASSWORD_A);
            ServiceTest.add_2(service, player1, player2);
            StepVerifier.create(service.getPlayers()).expectNextCount(3)
                     .verifyComplete();
         }
      }// class
   }// class

   @Nested
   public class Constructor {

      @Test
      public void a() {
         constructor(passwordEncoderA, playerRepositoryA, PASSWORD_A);
      }

      @Test
      public void b() {
         constructor(passwordEncoderB, playerRepositoryB, PASSWORD_B);
      }
   }// class

   @Nested
   public class Scenario {

      private ServiceImpl service;
      private Flux<Player> players;

      @Test
      public void get_players_of_fresh_instance() {
         given_a_fresh_instance_of_MC();
         when_getting_the_players();
         assertAll(() -> then_the_list_of_players_has_one_player(),
                  () -> then_the_list_of_players_includes_the_administrator(),
                  () -> then_the_list_of_players_includes_a_player_named_Administrator());
      }

      private void given_a_fresh_instance_of_MC() {
         service = new ServiceImpl(passwordEncoderA, playerRepositoryA,
                  PASSWORD_A);
      }

      private void then_the_list_of_players_has_one_player() {
         StepVerifier.create(players).expectNextCount(1);
      }

      private void then_the_list_of_players_includes_a_player_named_Administrator() {
         final List<Player> playersList = new ArrayList<>(1);
         players.subscribe(p -> playersList.add(p));
         playersList.forEach(Assertions::assertNotNull);
         assertThat(
                  "the list of players includes a player named \"Administrator\"",
                  playersList,
                  containsInAnyOrder(new Player(Player.ADMINISTRATOR_USERNAME,
                           null, Authority.ALL)));
      }

      private void then_the_list_of_players_includes_the_administrator() {
         StepVerifier.create(players)
                  .expectNextMatches(player -> Player.ADMINISTRATOR_USERNAME
                           .equals(player.getUsername()));
      }

      private void when_getting_the_players() {
         players = getPlayers(service);
      }
   }// class

   private static final String PASSWORD_A = "letmein";

   private static final String PASSWORD_B = "password123";

   private static final String PASSWORD_C = "secret";

   public static void assertInvariants(final ServiceImpl service) {
      ServiceTest.assertInvariants(service);// inherited

      final PlayerRepository playerRepository = service.getPlayerRepository();
      assertNotNull(playerRepository,
               "Always have a (non null) player repository.");
      PlayerRepositoryTest.assertInvariants(playerRepository);
   }

   private static ServiceImpl constructor(final PasswordEncoder passwordEncoder,
            final PlayerRepository playerRepository,
            final String administratorPassword) {
      final var service = new ServiceImpl(passwordEncoder, playerRepository,
               administratorPassword);

      assertInvariants(service);
      assertSame(playerRepository, service.getPlayerRepository(),
               "The player repository of this service is the given player repository.");
      assertSame(passwordEncoder, service.getPasswordEncoder(),
               "The password encoder of this service is the given password encoder.");
      getPlayers(service);
      final var encryptedAdminPassword = findByUsername(service,
               Player.ADMINISTRATOR_USERNAME).block().getPassword();
      assertTrue(
               passwordEncoder.matches(administratorPassword,
                        encryptedAdminPassword),
               "The password of the administrator user details found through this service is equal to the given administrator password encrypted by the given password encoder.");

      return service;
   }

   public static Mono<UserDetails> findByUsername(final ServiceImpl service,
            final String username) {
      final var publisher = ServiceTest.findByUsername(service, username);

      assertInvariants(service);

      return publisher;
   }

   public static Flux<Player> getPlayers(final ServiceImpl service) {
      final Flux<Player> players = ServiceTest.getPlayers(service);// inherited

      assertInvariants(service);

      return players;
   }

   private final PasswordEncoder passwordEncoderA = new BCryptPasswordEncoder(
            4);

   private final PasswordEncoder passwordEncoderB = new BCryptPasswordEncoder(
            5);

   private PlayerRepository playerRepositoryA;

   private PlayerRepository playerRepositoryB;

   private Player playerA;

   private Player playerB;

   private Player playerC;

   @Test
   public void administratorInRepository() {
      final var passwordEncoder = passwordEncoderA;
      final var repository = playerRepositoryA;
      final var service = new ServiceImpl(passwordEncoder, repository,
               PASSWORD_A);
      repository
               .save(new Player(Player.ADMINISTRATOR_USERNAME,
                        passwordEncoder.encode(PASSWORD_B), Authority.ALL))
               .block();

      getPlayers(service);
   }

   @BeforeEach
   public void setUpPlayers() {
      playerA = new Player("John", PASSWORD_A, Set.of());
      playerB = new Player("Alan", PASSWORD_B, Authority.ALL);
      playerC = new Player("Gweezer", PASSWORD_C, Set.of());
   }

   @BeforeEach
   public void setUpRepositories() {
      playerRepositoryA = new PlayerRepositoryTest.Fake();
      playerRepositoryB = new PlayerRepositoryTest.Fake();
   }
}
