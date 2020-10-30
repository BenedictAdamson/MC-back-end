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

import java.time.Instant;
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
         test(repositoryA);
      }

      @Test
      public void b() {
         test(repositoryB);
      }

      private void test(final GameRepository repository) {
         final var service = new GameServiceImpl(repository);

         assertInvariants(service);
         assertSame(repository, service.getRepository(), "repository");
      }
   }// class

   @Nested
   public class Create {

      @Test
      public void a() {
         test(SCENARIO_ID_A);
      }

      @Test
      public void b() {
         test(SCENARIO_ID_B);
      }

      private void test(final UUID scenario) {
         final var service = new GameServiceImpl(repositoryA);

         final var game = create(service, scenario);

         final var identifier = game.getIdentifier();
         final var retrievedGame = service.getGame(identifier);
         assertNotNull(retrievedGame,
                  "can retrieve something using the ID (not null)");// guard
         assertTrue(retrievedGame.isPresent(),
                  "can retrieve something using the ID");// guard
         final var retrievedIdentifier = retrievedGame.get().getIdentifier();
         assertThat("scenario ID", retrievedIdentifier.getScenario(),
                  is(scenario));
      }
   }// class

   @Nested
   public class GetCreationTimesOfGamesOfScenario {

      @Test
      public void none() {
         final var service = new GameServiceImpl(repositoryA);

         final var result = getCreationTimesOfGamesOfScenario(service,
                  SCENARIO_ID_A);

         assertEquals(0L, result.count(), "empty");
      }

      @Test
      public void one() {
         final var id = IDENTIFIER_A;
         final var scenarioId = id.getScenario();
         final var created = id.getCreated();
         repositoryA.save(new Game(id));
         final var service = new GameServiceImpl(repositoryA);

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
         repositoryA.save(new Game(IDENTIFIER_A));
         final var service = new GameServiceImpl(repositoryA);

         final var result = getCreationTimesOfGamesOfScenario(service,
                  scenarioId);

         assertEquals(0L, result.count(), "empty");
      }
   }// class

   @Nested
   public class GetGame {

      @Test
      public void absent() {
         final var service = new GameServiceImpl(repositoryA);
         final var id = IDENTIFIER_A;

         final var result = getGame(service, id);

         assertTrue(result.isEmpty(), "absent");
      }

      @Test
      public void present() {
         final var id = IDENTIFIER_A;
         final var game = new Game(id);
         repositoryA.save(game);
         final var service = new GameServiceImpl(repositoryA);

         final var result = getGame(service, id);

         assertTrue(result.isPresent(), "present");// guard
         assertEquals(game, result.get(), "game");
      }
   }// class

   @Nested
   public class GetGameIdentifiers {

      @Test
      public void none() {
         final var service = new GameServiceImpl(repositoryA);

         final var result = getGameIdentifiers(service);

         assertEquals(0L, result.count(), "empty");
      }

      @Test
      public void one() {
         final var id = IDENTIFIER_A;
         repositoryA.save(new Game(id));
         final var service = new GameServiceImpl(repositoryA);

         final var result = getGameIdentifiers(service);

         final var list = result.collect(toList());
         assertAll(() -> assertEquals(1L, list.size(), "count"),
                  () -> assertThat("has ID", list, hasItem(id)));
      }
   }// class

   private static final UUID SCENARIO_ID_A = UUID.randomUUID();

   private static final UUID SCENARIO_ID_B = UUID.randomUUID();

   private static final Identifier IDENTIFIER_A = new Game.Identifier(
            SCENARIO_ID_A, Instant.now());

   public static void assertInvariants(final GameServiceImpl service) {
      GameServiceTest.assertInvariants(service);// inherited
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

   private GameRepositoryTest.Fake repositoryA;

   private GameRepositoryTest.Fake repositoryB;

   @BeforeEach
   public void createRepositories() {
      repositoryA = new GameRepositoryTest.Fake();
      repositoryB = new GameRepositoryTest.Fake();
   }
}
