package uk.badamson.mc;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import uk.badamson.mc.repository.PlayerRepository;

/**
 * <p>
 * Unit tests for the {@link ReactiveAuthenticationManager} bean and its
 * collaborators.
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class ReactiveAuthenticationManagerTest {

   @Nested
   public class Valid {

      @Test
      public void a() {
         test(USERNAME_A, PASSWORD_A, Set.of());
      }

      @Test
      public void b() {
         test(USERNAME_B, PASSWORD_B, Authority.ALL);
      }

      private void test(final String username, final String password,
               final Set<Authority> authorities) {
         final Player player = new Player(username,
                  passwordEncoder.encode(password), authorities);
         playerRepository.save(player).block();

         final Authentication authentication = authenticate_usernameAndPassword(
                  username, password);

         assertNotNull(authentication, "Able to authnticate");
         assertEquals(username, authentication.getName(), "name");
         assertEquals(authorities, Set.copyOf(authentication.getAuthorities()),
                  "Granted all the authorities of the user");
      }
   }// class

   @Nested
   public class WrongPassword {

      @Test
      public void a() {
         test(USERNAME_A, PASSWORD_A, PASSWORD_B);
      }

      @Test
      public void b() {
         test(USERNAME_B, PASSWORD_B, PASSWORD_C);
      }

      private void test(final String username, final String password,
               final String givenPassword) {
         assert !password.equals(givenPassword);

         final Player player = new Player(username,
                  passwordEncoder.encode(password), Set.of());
         playerRepository.save(player).block();

         assertThrows(BadCredentialsException.class,
                  () -> authenticate_usernameAndPassword(username,
                           givenPassword));
      }
   }// class

   static {
      Hooks.onOperatorDebug();
   }

   private static final String PASSWORD_A = "letmein";

   private static final String PASSWORD_B = "password123";

   private static final String PASSWORD_C = "secret";

   private static final String USERNAME_A = "John";

   private static final String USERNAME_B = "Jeff";

   public static void assertInvariants(
            final ReactiveAuthenticationManager authenticationManager) {
      // Do nothing
   }

   public static Mono<Authentication> authenticate(
            final ReactiveAuthenticationManager authenticationManager,
            final Authentication authentication) {
      final var publisher = authenticationManager.authenticate(authentication);

      assertNotNull(publisher, "Returns a (non null) publisher");
      assertInvariants(authenticationManager);

      return publisher;
   }

   @Autowired
   private PlayerRepository playerRepository;

   @Autowired
   private ReactiveAuthenticationManager authenticationManager;

   @Autowired
   private PasswordEncoder passwordEncoder;

   @Test
   public void authenticate_unknonwUsername() {
      assertThrows(BadCredentialsException.class,
               () -> authenticate_usernameAndPassword(USERNAME_A, PASSWORD_A));
   }

   private Authentication authenticate_usernameAndPassword(
            final String username, final String password) {
      final Authentication token = new UsernamePasswordAuthenticationToken(
               username, password);

      final var publisher = authenticate(authenticationManager, token);

      return publisher.block();
   }
}
