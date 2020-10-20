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

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import uk.badamson.mc.service.ScenarioService;

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

   @Autowired
   private ScenarioService service;

   private Set<UUID> ids = Set.of();

   private UUID id;

   private Scenario responseScenario;

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
   public void mc_serves_scenario_page() throws Exception {
      final var responseText = worldCore.getResponseBodyAsString();
      final var mapper = new ObjectMapper();
      responseScenario = mapper.readValue(responseText, Scenario.class);
      assertEquals(id, responseScenario.getIdentifier().getId(),
               "scenario has the requested ID");
   }

   @Then("MC serves the scenarios page")
   public void mc_serves_scenarios_page() throws Exception {
      worldCore.responseIsOk();
   }

   @When("Navigate to one scenario")
   public void navigate_to_one_scenario() throws Exception {
      id = ids.stream().findAny().get();
      worldCore.performRequest(
               get("/api/scenario/" + id).accept(MediaType.APPLICATION_JSON));
   }

   @Then("the response is a list of scenarios")
   public void response_is_list_of_scenarios() {
      try {
         getResponseAsScenarioIdentifierList();
      } catch (final IOException e) {
         throw new AssertionFailedError("Can decode response", e);
      }
   }

   @Then("The scenario page includes the scenario description")
   public void scenario_page_includes_scenario_description() {
      Objects.requireNonNull(service, "service");
      Objects.requireNonNull(id, "id");
      Objects.requireNonNull(responseScenario, "responseScenario");

      final var expectedScenario = service.getScenario(id).get();
      assertThat("description", responseScenario.getDescription(),
               is(expectedScenario.getDescription()));
   }

   @When("Viewing the scenarios")
   public void viewing_scenarios() {
      ids = service.getScenarioIdentifiers().map(id -> id.getId())
               .collect(toUnmodifiableSet());
   }
}
