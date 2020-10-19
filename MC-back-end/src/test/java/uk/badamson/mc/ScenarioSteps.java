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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.util.List;

import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

   private void getResponseAsScenarioIdentifierList() throws IOException {
      final var response = worldCore.getResponseBodyAsString();
      new ObjectMapper().readValue(response,
               new TypeReference<List<Scenario.Identifier>>() {
               });
   }

   private void getScenarios() throws Exception {
      worldCore.performRequest(
               get("/api/scenario").accept(MediaType.APPLICATION_JSON));
   }

   @When("getting the scenarios")
   public void getting_scenarios() throws Exception {
      getScenarios();
   }

   @When("MC serves the scenario page")
   public void mc_serves_scenario_page() {
      throw new UnsupportedOperationException();
   }

   @Then("MC serves the scenarios page")
   public void mc_serves_scenarios_page() throws Exception {
      worldCore.responseIsOk();
   }

   @When("Navigate to one scenario")
   public void navigate_to_one_scenario() {
      throw new UnsupportedOperationException();
   }

   @Then("the response is a list of scenarios")
   public void response_is_list_of_scenarios() {
      try {
         getResponseAsScenarioIdentifierList();
      } catch (final IOException e) {
         throw new AssertionFailedError("Can decode response", e);
      }
   }

   @When("Viewing the scenarios")
   public void viewing_scenarios() {
      throw new UnsupportedOperationException();
   }
}
