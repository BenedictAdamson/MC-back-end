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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * <p>
 * Definitions of BDD steps for, for features about unknown pages (resources).
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class UnknownPageSteps {

   @Autowired
   private BackEndWorld world;

   @When("getting the unknown resource at {string}")
   public void getting_the_unknown_resource_at(final String path)
            throws Exception {
      world.getJson(path);
   }

   @Then("MC replies with Client Error")
   public void mc_replies_with_client_error() throws Exception {
      world.getResponse().andExpect(status().is4xxClientError());
   }

   @When("modifying the unknown resource with a {string} at {string}")
   public void modifying_the_unknown_resource_with_a(final String verb,
            final String path) throws Exception {
      final var method = HttpMethod.valueOf(verb);
      assert method != null;
      world.exchangeJson(method, path);
   }

}
