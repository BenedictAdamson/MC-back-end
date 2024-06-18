package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2019-23.
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

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.opentest4j.MultipleFailuresError;
import org.springframework.web.util.UriTemplate;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>
 * A <i>page object</i> for a game page.
 * </p>
 */
@Immutable
public final class GamePage extends Page {

    private static final UriTemplate URI_TEMPLATE = new UriTemplate("/game/{game}");

    private static final Matcher<String> INDICATES_IS_A_GAME = containsString(
            "Game");
    private static final Matcher<String> INDICATES_WHETHER_RECRUITING_PLAYERS = containsString(
            "recruiting");
    private static final Matcher<String> INDICATES_IS_RECRUITING_PLAYERS = containsString(
            "This game is recruiting players");
    private static final Matcher<String> INDICATES_IS_NOT_RECRUITING_PLAYERS = containsString(
            "This game is not recruiting players");
    private static final Matcher<String> INDICATES_IS_NOT_JOINABLE = containsString(
            "You may not join this game");
    private static final Matcher<String> INDICATES_IS_JOINABLE = containsString(
            "You may join this game");
    private static final Matcher<String> INDICATES_IS_PLAYING = containsString(
            "You are playing this game");
    private static final Matcher<String> INDICATES_IS_NOT_PLAYING = containsString(
            "You are not playing this game");
    private static final Matcher<String> INDICATES_CHARACTER_PLAYED = containsString(
            "You are playing this game as ");
    private static final Matcher<String> INDICATES_IS_RUNNING = matchesPattern(
            "[Rr]unning");
    private static final Matcher<String> INDICATES_JOINING_INFORMATION = anyOf(
            INDICATES_IS_JOINABLE, INDICATES_IS_NOT_JOINABLE);

    private static final By SCENARIO_LINK_LOCATOR = By.id("scenario");
    private static final By RECRUITING_ELEMENT_LOCATOR = By.id("recruiting");
    private static final By RUN_STATE_ELEMENT_LOCATOR = By.id("run-state");
    private static final By PLAYING_ELEMENT_LOCATOR = By.id("playing");
    private static final By JOINABLE_ELEMENT_LOCATOR = By.id("joinable");
    private static final By END_RECRUITMENT_ELEMENT_LOCATOR = By
            .id("end-recruitment");
    private static final By JOIN_BUTTON_LOCATOR = By.id("join");
    private static final By START_BUTTON_LOCATOR = By.id("start");
    private static final By STOP_BUTTON_LOCATOR = By.id("stop");
    private static final By PLAYED_CHARACTERS_ELEMENT_LOCATOR = By
            .id("played-characters");

    private static final Matcher<WebElement> HAS_ENDED_RECRUITMENT = new WebElementMatcher() {

        @Override
        protected boolean matchesSafely(final WebElement body,
                                        final Description mismatchDescription) {
            final var elements = body.findElements(RECRUITING_ELEMENT_LOCATOR);
            return elements.size() == 1 && INDICATES_IS_NOT_RECRUITING_PLAYERS
                    .matches(elements.get(0).getText());
        }

    };

    private static final Matcher<WebElement> IS_PLAYING_GAME = new WebElementMatcher() {

        @Override
        protected boolean matchesSafely(final WebElement body,
                                        final Description mismatchDescription) {
            final var elements = body.findElements(PLAYING_ELEMENT_LOCATOR);
            return elements.size() == 1
                    && INDICATES_IS_PLAYING.matches(elements.get(0).getText());
        }

    };

    private static final Matcher<WebElement> IS_RUNNING = new WebElementMatcher() {

        @Override
        protected boolean matchesSafely(final WebElement body,
                                        final Description mismatchDescription) {
            final var elements = body.findElements(RUN_STATE_ELEMENT_LOCATOR);
            return elements.size() == 1
                    && INDICATES_IS_RUNNING.matches(elements.get(0).getText());
        }

    };

    private static final Matcher<WebElement> IS_NOT_RUNNING = new WebElementMatcher() {

        @Override
        protected boolean matchesSafely(final WebElement body,
                                        final Description mismatchDescription) {
            final var elements = body.findElements(RUN_STATE_ELEMENT_LOCATOR);
            return elements.size() == 1
                    && !INDICATES_IS_RUNNING.matches(elements.get(0).getText());
        }

    };

    private final ScenarioPage scenarioPage;

    private final Matcher<String> includesCreationTime;

    private final Matcher<String> includesScenarioTitle;

    public GamePage(final HomePage homePage) {
        super(homePage);
        this.scenarioPage = null;
        includesCreationTime = null;
        includesScenarioTitle = isA(String.class);
    }

    public GamePage(final ScenarioPage scenarioPage, final String creationTime) {
        super(scenarioPage);
        this.scenarioPage = scenarioPage;
        includesCreationTime = creationTime == null ? null
                : containsString(creationTime);
        includesScenarioTitle = containsString(scenarioPage.getScenarioTitle());
    }

    public void assertDoesNotIndicateWhichCharactersPlayedByOtherUsers() {
        assertThat("Does not list characters",
                getBody().findElements(PLAYED_CHARACTERS_ELEMENT_LOCATOR),
                empty());
    }

    private WebElement assertHasJoinableElement(final WebElement body) {
        return assertHasElement(body, JOINABLE_ELEMENT_LOCATOR);
    }

    private WebElement assertHasPlayingElement(final WebElement body) {
        return assertHasElement("Has an element for reporting whether playing",
                body, PLAYING_ELEMENT_LOCATOR);
    }

    public void assertIncludesCreationTime() {
        if (includesCreationTime != null) {
            assertThat("includes creation time", getBody().getText(),
                    includesCreationTime);
        }
        // else can not check
    }

    public void assertIncludesScenarioTitle() {
        assertThat("includes scenario title", getBody().getText(),
                includesScenarioTitle);
    }

    public void assertIndicatesGameHasNoPlayedCharacters() {
        final var playedCharacters = assertHasElement(getBody(),
                PLAYED_CHARACTERS_ELEMENT_LOCATOR);
        final var playedCharacterTitles = playedCharacters
                .findElements(By.tagName("li"));
        assertThat("played character titles", playedCharacterTitles, empty());
    }

    public void assertIndicatesIsNotRecruitingPlayers() {
        final var element = assertHasElement(getBody(),
                RECRUITING_ELEMENT_LOCATOR);
        MatcherAssert.assertThat(element.getText(), INDICATES_IS_NOT_RECRUITING_PLAYERS);
    }

    public void assertIndicatesIsRecruitingPlayers() {
        final var element = assertHasElement(getBody(),
                RECRUITING_ELEMENT_LOCATOR);
        MatcherAssert.assertThat(element.getText(), INDICATES_IS_RECRUITING_PLAYERS);
    }

    public void assertIndicatesNotRunning() {
        assertIndicatesNotRunning(getBody());
    }

    private void assertIndicatesNotRunning(final WebElement body)
            throws MultipleFailuresError {
        final var element = assertHasElement(body, RUN_STATE_ELEMENT_LOCATOR);
        assertThat("Run-state element text", element.getText(),
                not(INDICATES_IS_RUNNING));
    }

    public void assertIndicatesRunning() {
        assertIndicatesRunning(getBody());
    }

    private void assertIndicatesRunning(final WebElement body)
            throws MultipleFailuresError {
        final var element = assertHasElement(body, RUN_STATE_ELEMENT_LOCATOR);
        assertThat("Run-state element text", element.getText(),
                INDICATES_IS_RUNNING);
    }

    public void assertIndicatesUserIsNotPlayingGame() {
        final var playing = assertHasPlayingElement(getBody());
        assertThat("Indicates is not playing the game", playing.getText(),
                INDICATES_IS_NOT_PLAYING);
    }

    public void assertIndicatesUserIsPlayingGame() {
        final var playing = assertHasPlayingElement(getBody());
        assertThat("Indicates is playing the game", playing.getText(),
                INDICATES_IS_PLAYING);
    }

    public void assertIndicatesUserMayJoinGame() {
        final var joinable = assertHasElement(getBody(),
                JOINABLE_ELEMENT_LOCATOR);
        assertThat("Indicates may join game", joinable.getText(),
                INDICATES_IS_JOINABLE);
    }

    public void assertIndicatesUserMayNotJoinGame() {
        final var element = assertHasElement(getBody(), JOINABLE_ELEMENT_LOCATOR);
        MatcherAssert.assertThat(element.getText(), INDICATES_IS_NOT_JOINABLE);
    }

    public void assertIndicatesWhetherGameHasPlayers() {
        // FIXME
    }

    public void assertIndicatesWhetherRecruitingPlayers() {
        assertIndicatesWhetherRecruitingPlayers(getBody());
    }

    private void assertIndicatesWhetherRecruitingPlayers(final WebElement body)
            throws MultipleFailuresError {
        final var element = assertHasElement(body, RECRUITING_ELEMENT_LOCATOR);
        final var elementText = element.getText();
        assertAll(
                () -> assertThat("page text mentions recruiting", body.getText(),
                        INDICATES_WHETHER_RECRUITING_PLAYERS),
                () -> assertThat("Element text indicates something", elementText,
                        either(INDICATES_IS_RECRUITING_PLAYERS)
                                .or(INDICATES_IS_NOT_RECRUITING_PLAYERS)));
    }

    public void assertIndicatesWhetherRunning() {
        assertIndicatesWhetherRunning(getBody());
    }

    private void assertIndicatesWhetherRunning(final WebElement body)
            throws MultipleFailuresError {
        assertHasElement(body, RUN_STATE_ELEMENT_LOCATOR);
    }

    public void assertIndicatesWhetherUserIsPlayingGame() {
        assertIndicatesWhetherUserIsPlayingGame(getBody());
    }

    private void assertIndicatesWhetherUserIsPlayingGame(final WebElement body) {
        final var playing = assertHasPlayingElement(body);
        assertThat("Indicates whether playing the game", playing.getText(),
                either(INDICATES_IS_PLAYING).or(INDICATES_IS_NOT_PLAYING));
    }

    public void assertIndicatesWhetherUserMayJoinGame() {
        assertIndicatesWhetherUserMayJoinGame(getBody());
    }

    private void assertIndicatesWhetherUserMayJoinGame(final WebElement body) {
        final var joinable = assertHasJoinableElement(body);
        assertThat("Indicates whether the user may join this game",
                joinable.getText(), INDICATES_JOINING_INFORMATION);
    }

    public void assertIndicatesWhichCharacterIfAnyUserIsPlaying() {
        final var playing = assertHasPlayingElement(getBody());
        assertThat("Indicates which character (if any) user is playing",
                playing.getText(),
                either(INDICATES_CHARACTER_PLAYED).or(INDICATES_IS_NOT_PLAYING));
    }

    public void assertIndicatesWhichCharactersPlayedByWhichUsers() {
        assertHasElement(getBody(), PLAYED_CHARACTERS_ELEMENT_LOCATOR);
    }

    public void assertIndicatesWhichCharacterUserIsPlaying() {
        final var playing = assertHasPlayingElement(getBody());
        assertThat("Indicates which character user is playing", playing.getText(),
                INDICATES_CHARACTER_PLAYED);
    }

    private void assertJoinButtonConsistentWithJoinableText(
            final WebElement body) {
        final var button = assertHasElement("has a join button", body,
                JOIN_BUTTON_LOCATOR);
        final var description = assertHasJoinableElement(body);
        assertEquals(isEnabled(button),
                INDICATES_IS_JOINABLE.matches(description.getText()),
                "join button is enabled iff joinable text indicates is joinable");
    }

    @Override
    protected void assertValidBody(@Nonnull final WebElement body) {
        assertAll(() -> assertIndicatesWhetherUserMayJoinGame(body),
                () -> assertIndicatesWhetherUserIsPlayingGame(body),
                () -> assertJoinButtonConsistentWithJoinableText(body),
                () -> assertValidBodyText(body, body.getText()));
    }

    private void assertValidBodyText(final WebElement body,
                                     final String bodyText) throws MultipleFailuresError {
        final var universalConstraints = allOf(INDICATES_IS_A_GAME,
                INDICATES_WHETHER_RECRUITING_PLAYERS,
                INDICATES_JOINING_INFORMATION, includesScenarioTitle);
        final var optionalConstraints = includesCreationTime == null
                ? any(String.class)
                : includesCreationTime;
        final var textConstraints = both(universalConstraints)
                .and(optionalConstraints);
        assertAll(() -> assertThat("Body text", bodyText, textConstraints),
                () -> assertIndicatesWhetherRecruitingPlayers(body));
    }

    public void endRecruitement() {
        requireIsReady();
        final var button = getBody().findElement(END_RECRUITMENT_ELEMENT_LOCATOR);
        if (!isEnabled(button)) {
            throw new IllegalStateException(
                    "Button [" + button + "] is not enabled");
        }
        button.click();
        awaitIsReady(HAS_ENDED_RECRUITMENT);
    }

    public boolean isEndRecruitmentEnabled() {
        requireIsReady();
        return isEnabled(getBody().findElement(END_RECRUITMENT_ELEMENT_LOCATOR));
    }

    public boolean isStartingEnabled() {
        requireIsReady();
        final var elements = getBody().findElements(START_BUTTON_LOCATOR);
        if (elements.isEmpty()) {
            return false;
        } else {
            return isEnabled(elements.get(0));
        }
    }

    public boolean isStoppingEnabled() {
        requireIsReady();
        final var elements = getBody().findElements(STOP_BUTTON_LOCATOR);
        if (elements.isEmpty()) {
            return false;
        } else {
            return isEnabled(elements.get(0));
        }
    }

    @Override
    protected void assertValidPath(@Nonnull final String path) {
        assertThat("path", URI_TEMPLATE.matches(path));
    }

    public void joinGame() {
        awaitIsReady();
        awaitElementIsEnabled(JOIN_BUTTON_LOCATOR);
        final var button = getBody().findElement(JOIN_BUTTON_LOCATOR);
        button.click();
        awaitIsReady(IS_PLAYING_GAME);
    }

    public ScenarioPage navigateToScenarioPage() {
        if (scenarioPage == null) {
            throw new IllegalStateException("Unknown scenario page");
        }
        requireIsReady();
        final var link = getBody().findElement(SCENARIO_LINK_LOCATOR);
        link.click();
        scenarioPage.awaitIsReady();
        return scenarioPage;
    }

    public void startGame() {
        requireIsReady();
        final var button = getBody().findElement(START_BUTTON_LOCATOR);
        if (!isEnabled(button)) {
            throw new IllegalStateException(
                    "Button [" + button + "] is not enabled");
        }
        button.click();
        awaitIsReady(IS_RUNNING);
    }

    public void stopGame() {
        requireIsReady();
        final var button = getBody().findElement(STOP_BUTTON_LOCATOR);
        if (!isEnabled(button)) {
            throw new IllegalStateException(
                    "Button [" + button + "] is not enabled");
        }
        button.click();
        awaitIsReady(IS_NOT_RUNNING);
    }
}
