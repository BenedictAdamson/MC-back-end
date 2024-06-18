package uk.badamson.mc.presentation.page;
/*
 * © Copyright Benedict Adamson 2019-20,22.
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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * <p>
 * A <i>page object</i> for the scenarios page.
 * </p>
 */
@Immutable
public final class ScenariosPage extends Page {

    private static final By SCENARIO_LIST_LOCATOR = By.tagName("ul");
    private static final String PATH = "/scenario";

    /**
     * <p>
     * Construct a user page associated with a given home page.
     * </p>
     *
     * @param homePage The web home page.
     * @throws NullPointerException If {@code homePage} is null.
     */
    public ScenariosPage(final HomePage homePage) {
        super(homePage);
    }

    private void assertHasHeadingSayingScenarios(final WebElement body) {
        final var heading = assertHasElement(body, By.tagName("h2"));// guard
        assertThat("Has a header saying \"Scenarios\"", heading.getText(),
                containsString("Scenarios"));
    }

    public void assertHasListOfScenarios() {
        assertHasListOfScenarios(getBody());
    }

    private void assertHasListOfScenarios(final WebElement body) {
        assertHasElement(body, SCENARIO_LIST_LOCATOR);
    }

    @Override
    protected void assertValidBody(@Nonnull final WebElement body) {
        assertAll(() -> assertHasHeadingSayingScenarios(body),
                () -> assertHasListOfScenarios(body));
    }

    private List<WebElement> findScenarioElements() {
        return getBody().findElement(SCENARIO_LIST_LOCATOR)
                .findElements(By.tagName("li"));
    }

    public List<String> getScenarioTitles() {
        final var listEntries = findScenarioElements();
        final List<String> result = new ArrayList<>(listEntries.size());
        for (final var listEntry : listEntries) {
            result.add(listEntry.getText());
        }
        return result;
    }

    @Override
    protected void assertValidPath(@Nonnull final String path) {
        assertThat("path", path, is(PATH));
    }

    public ScenarioPage navigateToScenario(final int i) {
        final var entry = findScenarioElements().get(i);
        final var title = entry.getText();
        final var link = entry.findElement(By.tagName("a"));
        link.click();
        final var scenarioPage = new ScenarioPage(this, title);
        scenarioPage.awaitIsReady();
        return scenarioPage;
    }
}
