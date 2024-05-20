package uk.badamson.mc.presentation;
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
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

/**
 * <p>
 * A <i>page object</i> for a scenario page.
 * </p>
 */
@Immutable
public final class ScenarioPage extends Page {

    private static final String BASE = "/scenario/";

    private static final By CHARACTERS_LIST_LOCATOR = By.id("characters");
    private static final By GAMES_LIST_LOCATOR = By.id("games");
    private static final By CREATE_GAME_LOCATOR = By.id("create-game");

    private final String scenarioTitle;

    /**
     * <p>
     * Construct a scenario page associated with a given scenarios page and
     * having a given (expected) title.
     * </p>
     *
     * @param scenariosPage The scenarios page.
     * @param scenarioTitle The expected title.
     * @throws NullPointerException If {@code scenariosPage} is null. If {@code scenarioTitle} is
     *                              null.
     */
    public ScenarioPage(final ScenariosPage scenariosPage,
                        final String scenarioTitle) {
        super(scenariosPage);
        this.scenarioTitle = Objects.requireNonNull(scenarioTitle,
                "scenarioTitle");
    }

    private void assertDisplaysScenarioTitle(final WebElement body) {
        assertThat("displays the scenario title", body.getText(),
                containsString(scenarioTitle));
    }

    public void assertHasListOfCharacters() {
        assertHasListOfCharacters(getBody());
    }

    private void assertHasListOfCharacters(final WebElement body) {
        assertHasElement(body, CHARACTERS_LIST_LOCATOR);
    }

    public void assertHasListOfGames() {
        assertHasListOfGames(getBody());
    }

    private void assertHasListOfGames(final WebElement body) {
        assertHasElement(body, GAMES_LIST_LOCATOR);
    }

    @Override
    protected void assertValidBody(@Nonnull final WebElement body) {
        assertHasListOfGames(body);
        assertDisplaysScenarioTitle(body);
    }

    public GamePage createGame() {
        final var button = findCreateGameButton();
        button.click();
        final var gamePage = new GamePage(this, null);
        gamePage.awaitIsReady();
        return gamePage;
    }

    private WebElement findCreateGameButton() {
        return getBody().findElement(CREATE_GAME_LOCATOR);
    }

    private List<WebElement> findGameElements() {
        requireIsReady();
        return getBody().findElement(GAMES_LIST_LOCATOR)
                .findElements(By.tagName("li"));
    }

    @Nullable
    private WebElement findGameElement(@Nonnull UUID gameId) {
        final String idText = gameId.toString();
        requireIsReady();
        final var elements = findGameElements();
        for (var element: elements) {
            final var linkElement = element.findElement(By.tagName("a"));
            final String href = linkElement.getAttribute("href");
            if (href.contains(idText)) {
                return element;
            }
        }
        return null;
    }

    public int getNumberOfGamesListed() {
        return findGameElements().size();
    }

    public String getScenarioTitle() {
        return scenarioTitle;
    }

    public boolean hasLinksToGames() {
        requireIsReady();
        return !getBody().findElement(GAMES_LIST_LOCATOR)
                .findElements(By.tagName("a")).isEmpty();
    }

    public boolean isCreateGameButtonEnabled() {
        return isEnabled(getBody().findElement(CREATE_GAME_LOCATOR));
    }

    @Override
    protected void assertValidPath(@Nonnull final String path) {
        assertThat("path", path, startsWith(BASE));
    }

    public GamePage navigateToGamePage(final UUID gameId) {
        requireIsReady();
        final var gameElement = findGameElement(gameId);
        if (gameElement == null) {
            throw new IllegalStateException("No entry for game " + gameId);
        }
        final var link = gameElement.findElement(By.tagName("a"));
        final var creationTimeText = link.getText();
        link.click();
        final var gamePage = new GamePage(this, creationTimeText);
        gamePage.awaitIsReady();
        return gamePage;
    }

}
