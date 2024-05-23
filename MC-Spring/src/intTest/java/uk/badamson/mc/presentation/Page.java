package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2019-24.
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

import org.hamcrest.*;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.opentest4j.AssertionFailedError;

import javax.annotation.Nonnull;
import java.net.URI;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * <p>
 * A <i>page object</i>.
 * </p>
 * <p>
 * This is a Strategy class for accessing a web page.
 * </p>
 */
public abstract class Page {

    private static final Duration WAIT_UNTIL_READY_TIMEOUT = Duration.ofSeconds(17);
    private static final Duration WAIT_UNTIL_READY_POLL_INTERVAL = Duration.ofMillis(317);
    private static final Duration READY_QUERY_PAUSE = Duration.ofMillis(10);

    public static final class NotReadyException extends IllegalStateException {

        public NotReadyException(final String message) {
            super(message);
        }

        public NotReadyException(@Nonnull Page page, final Throwable cause) {
            super(page.createNotReadyMessage(), cause);
        }

    }

    private static final class IsReadyMatcher extends TypeSafeDiagnosingMatcher<WebElement> {
        @Nonnull
        private final By elementLocator;

        public IsReadyMatcher(@Nonnull By elementLocator) {
            this.elementLocator = Objects.requireNonNull(elementLocator);
        }

        @Override
        protected boolean matchesSafely(WebElement body, Description mismatchDescription) {
            final var element = body.findElement(elementLocator);
            if (isEnabled(element)) {
                return true;
            } else {
                mismatchDescription.appendText("Element [" + element + "] is not enabled");
                return false;
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Require element to be ready");
        }

    }

    protected static abstract class WebElementMatcher
            extends TypeSafeDiagnosingMatcher<WebElement> {

        @Override
        public void describeTo(final Description description) {
            description.appendText("Element matches");
        }
    }

    /**
     * <p>
     * How error messages are expected to be marked up in pages.
     * </p>
     */
    public static final By ERROR_LOCATOR = By.className("error");

    private static final By BODY_LOCATOR = By.tagName("body");

    private static final Matcher<WebElement> HAS_ERROR_ELEMENT = new WebElementMatcher() {

        @Override
        public void describeTo(final Description description) {
            description.appendText("Has an element with the error class");
        }

        @Override
        protected boolean matchesSafely(final WebElement item,
                                        final Description mismatchDescription) {
            return !item.findElements(ERROR_LOCATOR).isEmpty();
        }
    };

    /**
     * <p>
     * Require that a parent element contains an element.
     * </p>
     *
     * @param message An assertion message to use if reporting that this assertion
     *                failed
     * @param parent  The element to search
     * @param by      A matcher for the required element
     * @throws NullPointerException <ul>
     *                                         <li>if {@code message} is null</li>
     *                                         <li>if {@code parent} is null</li>
     *                                         <li>if {@code by} is null</li>
     *                                         </ul>
     * @throws AssertionFailedError if no such element is present.
     */
    protected static WebElement assertHasElement(final String message,
                                                 final WebElement parent, final By by) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(by, "by");

        try {
            return parent.findElement(by);
        } catch (final NoSuchElementException e) {
            throw new AssertionFailedError(message, e);
        }
    }

    /**
     * <p>
     * Require that a parent element contains an element.
     * </p>
     *
     * @param parent The element to search
     * @param by     A matcher for the required element
     * @throws NullPointerException <ul>
     *                                         <li>if {@code parent} is null</li>
     *                                         <li>if {@code by} is null</li>
     *                                         </ul>
     * @throws AssertionFailedError if no such element is present.
     */
    protected static WebElement assertHasElement(final WebElement parent,
                                                 final By by) {
        return assertHasElement("Has element " + by, parent, by);
    }

    private static String getPathOfUrl(final String url) {
        return URI.create(url).getPath();
    }

    /**
     * <p>
     * Whether a (control or navigation) element is <i>enabled</i>
     * </p>
     * <p>
     * Enabled elements respond to mouse clicks.
     * </p>
     *
     * @param element The element to examine
     * @return whether the {@code element} is enabled
     * @throws NullPointerException If {@code element} is null.
     */
    protected static boolean isEnabled(final WebElement element) {
        Objects.requireNonNull(element, "element");
        return element.getAttribute("disabled") == null;
    }

    private final WebDriver webDriver;
    private final URI baseUri;

    private final Matcher<String> IS_VALID_PATH = new CustomTypeSafeMatcher<>("Has valid path") {

        @Override
        protected boolean matchesSafely(final String actual) {
            return Page.this.isValidPath(actual);
        }

    };

    /**
     * <p>
     * Construct a page object associated with an existing page.
     * </p>
     *
     * @param page The existing page.
     * @throws NullPointerException If {@code page} is null.
     */
    protected Page(@Nonnull final Page page) {
        Objects.requireNonNull(page, "page");
        this.baseUri = page.baseUri;
        this.webDriver = page.webDriver;
    }

    /**
     * <p>
     * Construct a page object not associated with an existing page.
     * </p>
     */
    protected Page(
            @Nonnull URI baseUri,
            @Nonnull final WebDriver webDriver
    ) {
        this.webDriver = Objects.requireNonNull(webDriver, "webDriver");
        this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
    }

    public final void assertHasErrorMessage() {
        assertThat("Error message(s)", getBody().findElements(ERROR_LOCATOR),
                not(empty()));
    }

    /**
     * <p>
     * Check that the current page conforms to all the invariants (general
     * expectations) of this type of page.
     * </p>
     */
    public final void assertInvariants() {
        assertAll(() -> assertValidPath(getCurrentPath()),
                () -> assertValidTitle(getTitle()),
                () -> assertValidBody(getBody()));
    }

    public final void assertNoErrorMessages() {
        final var elements = getBody().findElements(ERROR_LOCATOR);
        // Report the error messages, to provide better diagnostics
        final var errorMessages = elements.stream().map(WebElement::getText).toList();
        assertThat("No error messages", errorMessages, is(empty()));
    }

    /**
     * <p>
     * Assert that a given page body is a valid body for this type of page.
     * </p>
     * <p>
     * The provided implementation does nothing; it allows all bodies.
     * </p>
     *
     * @param body the body to examine
     * @throws NullPointerException If {@code body} is null.
     * @throws AssertionError       if, and only iff, {@code body} is not valid
     */
    protected void assertValidBody(@Nonnull final WebElement body) {
        // Do nothing
    }

    /**
     * <p>
     * Assert that a given URI {@linkplain URI#getPath() path component} of a URI
     * is a valid value for this type of page.
     * </p>
     * <p>
     * The provided implementation uses the {@link #isValidPath(String)}
     * predicate. That implementation should be useful for most cases, which do
     * not need complex diagnostic information for invalid paths.
     * </p>
     *
     * @param path the path component to examine
     * @throws NullPointerException If {@code path} is null.
     * @throws AssertionError       if, and only iff, {@code path} is not valid
     * @see #isValidPath(String)
     */
    protected void assertValidPath(@Nonnull final String path) {
        assertThat(path, IS_VALID_PATH);
    }

    protected final boolean isValidPath(@Nonnull final String path) {
        try {
            assertValidPath(path);
        } catch (AssertionError e) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * Assert that a given page (window) title is a valid title for this type of
     * page.
     * </p>
     * <p>
     * The provided implementation does nothing; it allows all titles.
     * </p>
     *
     * @param title the title to examine
     * @throws NullPointerException If {@code title} is null.
     * @throws AssertionError       if, and only iff, {@code title} is not valid
     */
    protected void assertValidTitle(@Nonnull final String title) {
        // Do nothing
    }

    public final void awaitIsReady() throws NotReadyException {
        awaitIsReady(isA(WebElement.class));
    }

    protected final void awaitElementIsEnabled(@Nonnull By elementLocator) {
        awaitIsReady(new IsReadyMatcher(elementLocator));
    }

    protected final void awaitIsReady(
            @Nonnull final Matcher<WebElement> additionalBodyConstraints)
            throws IllegalStateException {
        try {
            new WebDriverWait(webDriver, WAIT_UNTIL_READY_TIMEOUT, WAIT_UNTIL_READY_POLL_INTERVAL)
                    .until(driver -> isReady(driver, additionalBodyConstraints));
        } catch (final TimeoutException e) {
            requireIsReady(); // throws NotReadyException, with good diagnostics,
            // if still not ready
            // OK, the final check was OK, so *just* became ready in time
        } catch (final Exception e) {// give better diagnostics
            throw new NotReadyException(this, e);
        }
    }

    public final void awaitIsReadyAndErrorMessage() throws IllegalStateException {
        try {
            new WebDriverWait(webDriver, WAIT_UNTIL_READY_TIMEOUT, WAIT_UNTIL_READY_POLL_INTERVAL)
                    .until(driver -> isReady(driver, isA(WebElement.class))
                            && HAS_ERROR_ELEMENT
                            .matches(driver.findElement(BODY_LOCATOR)));
        } catch (final Exception e) {// give better diagnostics
            throw new NotReadyException(this, e);
        }
    }

    private String createNotReadyMessage() {
        return "Page " + getClass().getSimpleName() + " not ready\nPath "
                + getCurrentPath() + "\nTitle " + getTitle() + "\nBody\n"
                + getBody().getText();
    }

    /**
     * <p>
     * Directly load this page, using an HTTP GET operation.
     * </p>
     * <p>
     * The method blocks until the load completes. It follows redirects.
     * </p>
     *
     * @throws UnsupportedOperationException If direct loading is impossible because the page does not have
     *                                       a {@linkplain #getValidPath() fixed path}.
     */
    public final void get() throws UnsupportedOperationException {
        webDriver.get(getUri().toASCIIString());
    }

    @Nonnull
    private URI getUri() throws UnsupportedOperationException {
        final var path = getValidPath();
        if (path.isEmpty()) {
            throw new UnsupportedOperationException("No path to get");
        }
        return baseUri.resolve(path.get());
    }

    @Nonnull
    protected final WebElement getBody() {
        return webDriver.findElement(BODY_LOCATOR);
    }

    @Nonnull
    private String getCurrentPath() {
        return getPathOfUrl(webDriver.getCurrentUrl());
    }

    @Nonnull
    protected final String getTitle() {
        return webDriver.getTitle();
    }

    /**
     * <p>
     * The path component of a valid fixed URI for a page of this type.
     * </p>
     * <p>
     * All pages have a URI, but not all have an easily predictable fixed
     * (constant) URI, so the value is {@link Optional} to allow for cases that
     * there is not a fixed URI or is difficult to predict.
     * </p>
     * <p>
     * The provided implementation {@linkplain Optional#isEmpty() is empty}.
     * </p>
     *
     * @return the optional path; not null.
     */
    @Nonnull
    protected Optional<String> getValidPath() {
        return Optional.empty();
    }

    public final boolean isCurrentPath() {
        try {
            assertValidPath(getCurrentPath());
        } catch (AssertionError e) {
            return false;
        }
        return true;
    }

    private boolean isReady(final WebDriver driver,
                            final Matcher<WebElement> additionalRequirements) {
        try {
            requireIsReady(driver);
            requireAdditionalBodyConstraintsForReady(driver.findElement(BODY_LOCATOR), additionalRequirements);
        } catch (final NotReadyException e) {
            return false;
        }
        return true;
    }

    private void requireIsReady(@Nonnull WebDriver webDriver) throws NotReadyException {
        try {
            assertValidPath(getPathOfUrl(webDriver.getCurrentUrl()));
            Thread.sleep(READY_QUERY_PAUSE.toMillis());
            assertValidTitle(webDriver.getTitle());
            Thread.sleep(READY_QUERY_PAUSE.toMillis());
            assertValidBody(webDriver.findElement(BODY_LOCATOR));
            Thread.sleep(READY_QUERY_PAUSE.toMillis());
        } catch (AssertionError | InterruptedException e) {
            throw new NotReadyException(this, e);
        }
    }

    public final void requireIsReady() throws NotReadyException {
        requireIsReady(webDriver);
    }

    private void requireAdditionalBodyConstraintsForReady(final WebElement value, final Matcher<WebElement> requirement)
            throws NotReadyException {
        if (!requirement.matches(value)) {
            final Description description = new StringDescription();
            description.appendText("Not ready because requires ")
                    .appendText(System.lineSeparator())
                    .appendText("Additional body constraints")
                    .appendText(System.lineSeparator())
                    .appendDescriptionOf(requirement)
                    .appendText(System.lineSeparator()).appendText("     but: ");
            requirement.describeMismatch(value, description);
            throw new NotReadyException(description.toString());
        }
    }
}
