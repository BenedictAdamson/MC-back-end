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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import uk.badamson.mc.Game;
import uk.badamson.mc.GamePlayers;
import uk.badamson.mc.repository.CurrentUserGameRepository;
import uk.badamson.mc.repository.CurrentUserGameRepositoryTest;
import uk.badamson.mc.repository.GamePlayersRepository;
import uk.badamson.mc.repository.GamePlayersRepositoryTest;
import uk.badamson.mc.repository.GameRepositoryTest;
import uk.badamson.mc.repository.UserRepository;
import uk.badamson.mc.repository.UserRepositoryTest;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link GamePlayersServiceImpl}
 * class.
 * </p>
 */
public class GamePlayersServiceImplTest {
   @Nested
   public class Constructor {

      @Test
      public void a() {
         test(gamePlayersRepositoryA, currentUserGameRepositoryA, gameServiceA,
                  userServiceA);
      }

      @Test
      public void b() {
         test(gamePlayersRepositoryB, currentUserGameRepositoryB, gameServiceB,
                  userServiceB);
      }

      private void test(final GamePlayersRepository gamePlayersRepository,
               final CurrentUserGameRepository currentUserGameRepository,
               final GameService gameService, final UserService userService) {
         final var service = new GamePlayersServiceImpl(gamePlayersRepository,
                  currentUserGameRepository, gameService, userService);

         assertInvariants(service);
         assertAll("Has the given assoications",
                  () -> assertSame(gamePlayersRepository,
                           service.getGamePlayersRepository(),
                           "gamePlayersRepository"),
                  () -> assertSame(currentUserGameRepository,
                           service.getCurrentUserGameRepository(),
                           "currentUserGameRepository"),
                  () -> assertSame(gameService, service.getGameService(),
                           "gameService"),
                  () -> assertSame(userService, service.getUserService(),
                           "userService"));
      }
   }// class

   @Nested
   public class EndRecruitment {

      @Nested
      public class InRepository {

         @Test
         public void a() {
            test(true, Set.of());
         }

         @Test
         public void b() {
            test(false, Set.of(USER_ID_A));
         }

         @Test
         public void c() {
            test(true, Set.of(USER_ID_B));
         }

         private void test(final boolean recruiting0, final Set<UUID> users) {
            final var gamePlayersRepository = gamePlayersRepositoryA;
            final var currentUserGameRepository = currentUserGameRepositoryA;
            final var gameService = gameServiceA;

            final var scenario = gameService.getScenarioService()
                     .getScenarioIdentifiers().findAny().get();
            final var game = gameService.create(scenario);
            final var id = game.getIdentifier();
            final var gamePlayersInRepository = new GamePlayers(id, recruiting0,
                     users);
            gamePlayersRepository.save(gamePlayersInRepository);
            final var service = new GamePlayersServiceImpl(
                     gamePlayersRepository, currentUserGameRepository,
                     gameService, userServiceA);

            final var gamePlayers = endRecruitment(service, id);

            assertAll("Attributes of returned value",
                     () -> assertThat("game", gamePlayers.getGame(), is(id)),
                     () -> assertThat("recruiting", gamePlayers.isRecruiting(),
                              is(false)),
                     () -> assertThat("users", gamePlayers.getUsers(),
                              is(users)));
         }
      }// class

      @Nested
      public class NoSuchGame {
         @Test
         public void a() {
            test(IDENTIFIER_A);
         }

         @Test
         public void b() {
            test(IDENTIFIER_B);
         }

         @Test
         public void inRepository() {
            final var gamePlayersRepository = gamePlayersRepositoryA;
            final var currentUserGameRepository = currentUserGameRepositoryA;
            final var gameService = gameServiceA;

            // Tough test: a valid scenario ID
            final var scenario = gameService.getScenarioService()
                     .getScenarioIdentifiers().findAny().get();
            final var id = new Game.Identifier(scenario, Instant.EPOCH);
            final var gamePlayersInrepository = new GamePlayers(id, true,
                     Set.of());
            gamePlayersRepository.save(gamePlayersInrepository);
            final var service = new GamePlayersServiceImpl(
                     gamePlayersRepository, currentUserGameRepository,
                     gameService, userServiceA);

            assertThrows(NoSuchElementException.class,
                     () -> endRecruitment(service, id));
         }

         private void test(final Game.Identifier id) {
            final var service = new GamePlayersServiceImpl(
                     gamePlayersRepositoryA, currentUserGameRepositoryA,
                     gameServiceA, userServiceA);

            assertThrows(NoSuchElementException.class,
                     () -> endRecruitment(service, id));
         }

      }// class

      @Test
      public void notInRepository() {
         final var gameService = gameServiceA;
         final var scenario = gameService.getScenarioService()
                  .getScenarioIdentifiers().findAny().get();
         final var game = gameService.create(scenario);
         final var id = game.getIdentifier();
         final var service = new GamePlayersServiceImpl(gamePlayersRepositoryA,
                  currentUserGameRepositoryA, gameService, userServiceA);

         final var gamePlayers = endRecruitment(service, id);

         assertAll("Changed default",
                  () -> assertThat("recruiting", gamePlayers.isRecruiting(),
                           is(false)),
                  () -> assertThat("users", gamePlayers.getUsers(), empty()));
      }
   }// class

   @Nested
   public class GetGamePlayers {

      @Nested
      public class InRepository {

         @Test
         public void a() {
            test(true, Set.of());
         }

         @Test
         public void b() {
            test(false, Set.of(USER_ID_A));
         }

         private void test(final boolean recruiting, final Set<UUID> users) {
            final var gameService = gameServiceA;
            final var gamePlayersRepository = gamePlayersRepositoryA;
            final var scenario = gameService.getScenarioService()
                     .getScenarioIdentifiers().findAny().get();
            final var game = gameService.create(scenario);
            final var id = game.getIdentifier();
            final var gamePlayersInrepository = new GamePlayers(id, recruiting,
                     users);
            gamePlayersRepository.save(gamePlayersInrepository);
            final var service = new GamePlayersServiceImpl(
                     gamePlayersRepository, currentUserGameRepositoryA,
                     gameService, userServiceA);

            final var result = getGamePlayers(service, id);

            assertTrue(result.isPresent(), "present");// guard
            final var gamePlayers = result.get();
            assertAll("Attributes of returned value",
                     () -> assertThat("game", gamePlayers.getGame(), is(id)),
                     () -> assertThat("recruiting", gamePlayers.isRecruiting(),
                              is(recruiting)),
                     () -> assertThat("users", gamePlayers.getUsers(),
                              is(users)));
         }
      }// class

      @Nested
      public class NoSuchGame {
         @Test
         public void a() {
            test(IDENTIFIER_A);
         }

         @Test
         public void b() {
            test(IDENTIFIER_B);
         }

         @Test
         public void inRepository() {
            final var gameService = gameServiceA;
            final var gamePlayersRepository = gamePlayersRepositoryA;
            // Tough test: a valid scenario ID
            final var scenario = gameService.getScenarioService()
                     .getScenarioIdentifiers().findAny().get();
            final var id = new Game.Identifier(scenario, Instant.EPOCH);
            final var gamePlayersInrepository = new GamePlayers(id, true,
                     Set.of());
            gamePlayersRepository.save(gamePlayersInrepository);
            final var service = new GamePlayersServiceImpl(
                     gamePlayersRepository, currentUserGameRepositoryA,
                     gameService, userServiceA);

            final var result = getGamePlayers(service, id);

            assertTrue(result.isEmpty(), "empty");
         }

         private void test(final Game.Identifier id) {
            final var service = new GamePlayersServiceImpl(
                     gamePlayersRepositoryA, currentUserGameRepositoryA,
                     gameServiceA, userServiceA);

            final var result = getGamePlayers(service, id);

            assertTrue(result.isEmpty(), "empty");
         }

      }// class

      @Test
      public void notInRepository() {
         final var gameService = gameServiceA;
         final var scenario = gameService.getScenarioService()
                  .getScenarioIdentifiers().findAny().get();
         final var game = gameService.create(scenario);
         final var id = game.getIdentifier();
         final var service = new GamePlayersServiceImpl(gamePlayersRepositoryA,
                  currentUserGameRepositoryA, gameService, userServiceA);

         final var result = getGamePlayers(service, id);

         assertTrue(result.isPresent(), "present");// guard
         final var gamePlayers = result.get();
         assertIsDefault(gamePlayers);
      }
   }// class

   private static final ZoneId UTC = ZoneId.from(ZoneOffset.UTC);

   private static final Clock CLOCK_A = Clock.systemUTC();

   private static final Clock CLOCK_B = Clock.fixed(Instant.EPOCH, UTC);

   private static final UUID USER_ID_A = UUID.randomUUID();

   private static final UUID USER_ID_B = UUID.randomUUID();

   private static final Game.Identifier IDENTIFIER_A = new Game.Identifier(
            UUID.randomUUID(), Instant.EPOCH);

   private static final Game.Identifier IDENTIFIER_B = new Game.Identifier(
            UUID.randomUUID(), Instant.now());

   private static final String PASSWORD_A = "letmein";

   private static final String PASSWORD_B = "password123";

   public static void assertInvariants(final GamePlayersServiceImpl service) {
      GamePlayersServiceTest.assertInvariants(service);// inherited

      assertNotNull(service.getGamePlayersRepository(), "Not null, repository");
   }

   private static void assertIsDefault(final GamePlayers gamePlayers) {
      assertAll("Default",
               () -> assertTrue(gamePlayers.isRecruiting(), "recruiting"),
               () -> assertThat("users", gamePlayers.getUsers(), empty()));
   }

   public static GamePlayers endRecruitment(
            final GamePlayersServiceImpl service, final Game.Identifier id)
            throws NoSuchElementException {
      final GamePlayers result;
      try {
         result = GamePlayersServiceTest.endRecruitment(service, id);
      } catch (final NoSuchElementException e) {
         assertInvariants(service);
         throw e;
      }
      assertInvariants(service);
      return result;
   }

   public static Optional<GamePlayers> getGamePlayers(
            final GamePlayersServiceImpl service, final Game.Identifier id) {
      final var result = GamePlayersServiceTest.getGamePlayers(service, id);// inherited
      assertInvariants(service);
      return result;
   }

   private final PasswordEncoder passwordEncoderA = new BCryptPasswordEncoder(
            4);

   private final PasswordEncoder passwordEncoderB = new BCryptPasswordEncoder(
            5);

   private final ScenarioService scenarioServiceA = new ScenarioServiceImpl();

   private final ScenarioService scenarioServiceB = new ScenarioServiceImpl();

   private GameRepositoryTest.Fake gameRepositoryA;

   private GameRepositoryTest.Fake gameRepositoryB;

   private GamePlayersRepositoryTest.Fake gamePlayersRepositoryA;

   private GamePlayersRepositoryTest.Fake gamePlayersRepositoryB;

   private CurrentUserGameRepositoryTest.Fake currentUserGameRepositoryA;

   private CurrentUserGameRepositoryTest.Fake currentUserGameRepositoryB;

   private UserRepository userRepositoryA;

   private UserRepository userRepositoryB;

   private GameService gameServiceA;

   private GameService gameServiceB;

   private UserService userServiceA;

   private UserService userServiceB;

   @BeforeEach
   public void setUp() {
      gameRepositoryA = new GameRepositoryTest.Fake();
      gameRepositoryB = new GameRepositoryTest.Fake();
      gamePlayersRepositoryA = new GamePlayersRepositoryTest.Fake();
      gamePlayersRepositoryB = new GamePlayersRepositoryTest.Fake();
      currentUserGameRepositoryA = new CurrentUserGameRepositoryTest.Fake();
      currentUserGameRepositoryB = new CurrentUserGameRepositoryTest.Fake();
      userRepositoryA = new UserRepositoryTest.Fake();
      userRepositoryB = new UserRepositoryTest.Fake();

      gameServiceA = new GameServiceImpl(gameRepositoryA, CLOCK_A,
               scenarioServiceA);
      gameServiceB = new GameServiceImpl(gameRepositoryB, CLOCK_B,
               scenarioServiceB);
      userServiceA = new UserServiceImpl(passwordEncoderA, userRepositoryA,
               PASSWORD_A);
      userServiceB = new UserServiceImpl(passwordEncoderB, userRepositoryB,
               PASSWORD_B);
   }
}
