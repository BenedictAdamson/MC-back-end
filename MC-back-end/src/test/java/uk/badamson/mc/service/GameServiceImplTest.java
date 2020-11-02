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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.repository.GameRepository;
import uk.badamson.mc.repository.GameRepositoryTest;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link GameServiceImpl} class.
 * </p>
 */
public class GameServiceImplTest {
   @Nested
   public class Constructor {

      @Test
      public void a() {
         test(gameRepositoryA, CLOCK_A, scenarioServiceA);
      }

      @Test
      public void b() {
         test(gameRepositoryB, CLOCK_B, scenarioServiceB);
      }

      private void test(final GameRepository repository, final Clock clock,
               final ScenarioService scenarioService) {
         final var service = new GameServiceImpl(repository, clock,
                  scenarioService);

         assertInvariants(service);
         assertAll("Has the given assoications",
                  () -> assertSame(repository, service.getRepository(),
                           "repository"),
                  () -> assertSame(clock, service.getClock(), "clock"),
                  () -> assertSame(scenarioService,
                           service.getScenarioService(), "scenarioService"));
      }
   }// class

   @Nested
   public class Create {

      @Test
      public void a() {
         test(SCENARIO_ID_A, Instant.EPOCH);
      }

      @Test
      public void b() {
         test(SCENARIO_ID_B, Instant.now());
      }

      private void test(final UUID scenario, final Instant now) {
         final var clock = Clock.fixed(now, UTC);
         final var service = new GameServiceImpl(gameRepositoryA, clock,
                  scenarioServiceA);

         final var game = create(service, scenario);

         final var identifier = game.getIdentifier();
         assertThat(
                  "The returned game has the current time as the creation time of its identifier.",
                  identifier.getCreated(), is(now));
         final var retrievedGame = service.getGame(identifier);
         assertNotNull(retrievedGame,
                  "can retrieve something using the ID (not null)");// guard
         assertTrue(retrievedGame.isPresent(),
                  "can retrieve something using the ID");// guard
         final var retrievedIdentifier = retrievedGame.get().getIdentifier();
         assertAll("can retrieve the created game using the ID",
                  () -> assertThat("scenario",
                           retrievedIdentifier.getScenario(), is(scenario)),
                  () -> assertThat("created", retrievedIdentifier.getCreated(),
                           is(now)));
      }
   }// class

   @Nested
   public class GetCreationTimesOfGamesOfScenario {

      @Test
      public void none() {
         final var service = new GameServiceImpl(gameRepositoryA, CLOCK_A,
                  scenarioServiceA);

         final var result = getCreationTimesOfGamesOfScenario(service,
                  SCENARIO_ID_A);

         assertEquals(0L, result.count(), "empty");
      }

      @Test
      public void one() {
         final var id = IDENTIFIER_A;
         final var scenarioId = id.getScenario();
         final var created = id.getCreated();
         gameRepositoryA.save(new Game(id));
         final var service = new GameServiceImpl(gameRepositoryA, CLOCK_A,
                  scenarioServiceA);

         final var result = getCreationTimesOfGamesOfScenario(service,
                  scenarioId);

         final var list = result.collect(toList());
         assertAll(() -> assertEquals(1L, list.size(), "count"),
                  () -> assertThat("has creation time", list,
                           hasItem(created)));
      }

      @Test
      public void otherScenarioHasOne() {
         final var scenarioId = SCENARIO_ID_B;
         gameRepositoryA.save(new Game(IDENTIFIER_A));
         final var service = new GameServiceImpl(gameRepositoryA, CLOCK_A,
                  scenarioServiceA);

         final var result = getCreationTimesOfGamesOfScenario(service,
                  scenarioId);

         assertEquals(0L, result.count(), "empty");
      }
   }// class

   @Nested
   public class GetGame {

      @Test
      public void absent() {
         final var service = new GameServiceImpl(gameRepositoryA, CLOCK_A,
                  scenarioServiceA);
         final var id = IDENTIFIER_A;

         final var result = getGame(service, id);

         assertTrue(result.isEmpty(), "absent");
      }

      @Test
      public void present() {
         final var id = IDENTIFIER_A;
         final var game = new Game(id);
         gameRepositoryA.save(game);
         final var service = new GameServiceImpl(gameRepositoryA, CLOCK_A,
                  scenarioServiceA);

         final var result = getGame(service, id);

         assertTrue(result.isPresent(), "present");// guard
         assertEquals(game, result.get(), "game");
      }
   }// class

   @Nested
   public class GetGameIdentifiers {

      @Test
      public void none() {
         final var service = new GameServiceImpl(gameRepositoryA, CLOCK_A,
                  scenarioServiceA);

         final var result = getGameIdentifiers(service);

         assertEquals(0L, result.count(), "empty");
      }

      @Test
      public void one() {
         final var id = IDENTIFIER_A;
         gameRepositoryA.save(new Game(id));
         final var service = new GameServiceImpl(gameRepositoryA, CLOCK_A,
                  scenarioServiceA);

         final var result = getGameIdentifiers(service);

         final var list = result.collect(toList());
         assertAll(() -> assertEquals(1L, list.size(), "count"),
                  () -> assertThat("has ID", list, hasItem(id)));
      }
   }// class

   private static final ZoneId UTC = ZoneId.from(ZoneOffset.UTC);

   private static final Clock CLOCK_A = Clock.systemUTC();

   private static final Clock CLOCK_B = Clock.fixed(Instant.EPOCH, UTC);

   private static final UUID SCENARIO_ID_A = UUID.randomUUID();

   private static final UUID SCENARIO_ID_B = UUID.randomUUID();

   private static final Identifier IDENTIFIER_A = new Game.Identifier(
            SCENARIO_ID_A, Instant.now());

   public static void assertInvariants(final GameServiceImpl service) {
      GameServiceTest.assertInvariants(service);// inherited

      assertNotNull(service.getRepository(), "Not null, repository");
   }

   public static Game create(final GameServiceImpl service,
            final UUID scenario) {
      final var game = GameServiceTest.create(service, scenario);// inherited

      assertInvariants(service);

      return game;
   }

   public static Stream<Instant> getCreationTimesOfGamesOfScenario(
            final GameServiceImpl service, final UUID scenario) {
      final var times = GameServiceTest
               .getCreationTimesOfGamesOfScenario(service, scenario);

      assertInvariants(service);

      return times;
   }

   public static Optional<Game> getGame(final GameServiceImpl service,
            final Game.Identifier id) {
      final var result = GameServiceTest.getGame(service, id);

      assertInvariants(service);
      return result;
   }

   public static Stream<Game.Identifier> getGameIdentifiers(
            final GameServiceImpl service) {
      final var games = GameServiceTest.getGameIdentifiers(service);// inherited

      assertInvariants(service);

      return games;
   }

   private GameRepositoryTest.Fake gameRepositoryA;

   private GameRepositoryTest.Fake gameRepositoryB;

   private ScenarioService scenarioServiceA = new ScenarioServiceImpl();

   private ScenarioService scenarioServiceB = new ScenarioServiceImpl();

   @BeforeEach
   public void createRepositories() {
      gameRepositoryA = new GameRepositoryTest.Fake();
      gameRepositoryB = new GameRepositoryTest.Fake();
   }
}
