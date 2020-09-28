package uk.badamson.mc.presentation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestBody;

import uk.badamson.mc.Authority;
import uk.badamson.mc.TestConfiguration;
import uk.badamson.mc.User;
import uk.badamson.mc.WorldCore;
import uk.badamson.mc.service.Service;

/* 
 * © Copyright Benedict Adamson 2020.
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

/**
 * <p>
 * Unit tests of the {@link UserController} class.
 * <p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class UserControllerTest {

   @Autowired
   private UserController controller;
   
   @Autowired
   private Service service;
   
   @Autowired
   private MockMvc mockMvc;

   @Test
   public void addPermitted() throws Exception {
      final var user = new User("jeff", "letmein", Authority.ALL, true, true, true, true);
      final var addedUser = new User("allan", "password1", Set.of(Authority.PLAYER), true, true, true, true);
      service.add(user);
      
      var response = mockMvc.perform(post("/api/user")
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON).with(user(user))
               .content(WorldCore.encodeAsJson(addedUser)));
      response.andExpect(status().isCreated());
   }
   

   private void add(final User player) {
      Objects.requireNonNull(controller, "controller");
      Objects.requireNonNull(player, "player");
      controller.add(player);
   }   
}
