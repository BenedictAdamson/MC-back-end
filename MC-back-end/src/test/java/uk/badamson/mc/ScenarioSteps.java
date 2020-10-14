package uk.badamson.mc;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * <p>
 * Definitions of BDD steps, for features about game scenarios.
 * </p>
 */
@SpringBootTest(classes = TestConfiguration.class,
         webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class ScenarioSteps {

   @Autowired
   private BackEndWorldCore worldCore;

   @When("getting the scenarios")
   public void getting_scenarios() {
      throw new UnsupportedOperationException();
   }

   @Then("MC serves the scenarios page")
   public void mc_serves_scenarios_page() {
      throw new UnsupportedOperationException();
   }

   @Then("the response is a list of scenarios")
   public void response_is_list_of_scenarios() {
      throw new UnsupportedOperationException();
   }
}
