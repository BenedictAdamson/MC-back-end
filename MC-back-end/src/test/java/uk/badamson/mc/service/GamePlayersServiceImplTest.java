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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.badamson.mc.Game;
import uk.badamson.mc.Game.Identifier;
import uk.badamson.mc.repository.GamePlayersRepository;
import uk.badamson.mc.repository.GamePlayersRepositoryTest;
import uk.badamson.mc.repository.GameRepositoryTest;

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
         test(gamePlayersRepositoryA, gameServiceA);
      }

      @Test
      public void b() {
         test(gamePlayersRepositoryB, gameServiceB);
      }

      private void test(final GamePlayersRepository repository,
               final GameService gameService) {
         final var service = new GamePlayersServiceImpl(repository,
                  gameService);

         assertInvariants(service);
         assertAll("Has the given assoications",
                  () -> assertSame(repository, service.getRepository(),
                           "repository"),
                  () -> assertSame(gameService, service.getGameService(),
                           "gameService"));
      }
   }// class

   private static final ZoneId UTC = ZoneId.from(ZoneOffset.UTC);

   private static final Clock CLOCK_A = Clock.systemUTC();

   private static final Clock CLOCK_B = Clock.fixed(Instant.EPOCH, UTC);

   private static final UUID SCENARIO_ID_A = UUID.randomUUID();

   private static final Identifier IDENTIFIER_A = new Game.Identifier(
            SCENARIO_ID_A, Instant.now());

   public static void assertInvariants(final GamePlayersServiceImpl service) {
      GamePlayersServiceTest.assertInvariants(service);// inherited

      assertNotNull(service.getRepository(), "Not null, repository");
   }

   private final ScenarioService scenarioServiceA = new ScenarioServiceImpl();

   private final ScenarioService scenarioServiceB = new ScenarioServiceImpl();

   private GameRepositoryTest.Fake gameRepositoryA;

   private GameRepositoryTest.Fake gameRepositoryB;

   private GamePlayersRepositoryTest.Fake gamePlayersRepositoryA;

   private GamePlayersRepositoryTest.Fake gamePlayersRepositoryB;

   private GameService gameServiceA;

   private GameService gameServiceB;

   @BeforeEach
   public void setUp() {
      gameRepositoryA = new GameRepositoryTest.Fake();
      gameRepositoryB = new GameRepositoryTest.Fake();
      gamePlayersRepositoryA = new GamePlayersRepositoryTest.Fake();
      gamePlayersRepositoryB = new GamePlayersRepositoryTest.Fake();
      gameServiceA = new GameServiceImpl(gameRepositoryA, CLOCK_A,
               scenarioServiceA);
      gameServiceB = new GameServiceImpl(gameRepositoryB, CLOCK_B,
               scenarioServiceB);
   }
}
