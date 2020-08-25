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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * <p>
 * Definitions of BDD steps for, for features about unknown pages (resources).
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext
public class UnknownResourceSteps {

   @Autowired
   private BackEndWorldCore worldCore;

   @When("getting the unknown resource at {string}")
   public void getting_the_unknown_resource_at(final String path)
            throws Exception {
      worldCore.getJson(path);
   }

   @Then("MC replies with Not Found")
   public void mc_replies_with_not_found() throws Exception {
      worldCore.getResponse()
               .andExpect(MockMvcResultMatchers.status().isNotFound());
   }

}
