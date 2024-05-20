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
import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * <p>
 * A <i>page object</i> for the users page.
 * </p>
 */
@Immutable
public final class UsersPage extends Page {

    private static final By USER_LINK_LOCATOR = By.tagName("a");

    private static final By USER_LIST_LOCATOR = By.tagName("ul");

    private static final String PATH = "/user";

    private static final By ADD_USER_LINK_LOCATOR = By
            .xpath("//a[@id='add-user']");

    /**
     * <p>
     * Construct a user page associated with a given home page.
     * </p>
     *
     * @param homePage The web home page.
     * @throws NullPointerException If {@code homePage} is null.
     */
    public UsersPage(final HomePage homePage) {
        super(homePage);
    }

    private void assertHasHeadingSayingUsers(final WebElement body) {
        final var heading = assertHasElement(body, By.tagName("h2"));
        assertThat("Has a heading saying \"Users\"", heading.getText(),
                containsString("Users"));
    }

    public void assertHasListOfUsers() {
        assertHasListOfUsers(getBody());
    }

    private void assertHasListOfUsers(final WebElement body) {
        assertHasElement(body, USER_LIST_LOCATOR);
    }

    public void assertListOfUsersIncludes(final String name) {
        Objects.requireNonNull(name, "name");
        final var list = assertHasElement(getBody(), USER_LIST_LOCATOR);
        assertThat(list.getText(), containsString(name));
    }

    public void assertListOfUsersNotEmpty() {
        final var list = assertHasElement(getBody(), USER_LIST_LOCATOR);
        assertThat(list.findElements(By.tagName("li")), not(empty()));
    }

    @Override
    protected void assertValidBody(@Nonnull final WebElement body) {
        assertAll(() -> assertHasHeadingSayingUsers(body),
                () -> assertHasListOfUsers(body));
    }

    private List<WebElement> findUserListEntries() {
        final var list = getBody().findElement(USER_LIST_LOCATOR);
        return list.findElements(By.tagName("li"));
    }

    public int getNumberOfUserLinks() {
        requireIsReady();
        return (int) findUserListEntries().stream().filter(
                        entry -> !entry.findElements(USER_LINK_LOCATOR).isEmpty())
                .count();
    }

    public boolean hasAddUserLink() {
        return !getBody().findElements(ADD_USER_LINK_LOCATOR).isEmpty();
    }

    @Override
    protected void assertValidPath(@Nonnull final String path) {
        assertThat("path", path, is(PATH));
    }

    public AddUserPage navigateToAddUserPage() {
        requireIsReady();
        final var link = getBody().findElement(ADD_USER_LINK_LOCATOR);
        link.click();
        final var addUserPage = new AddUserPage(this);
        addUserPage.awaitIsReady();
        return addUserPage;
    }

    public UserPage navigateToUserPage(final int index) {
        if (index < 0) {
            throw new IllegalArgumentException("negative index");
        }
        requireIsReady();
        final var entries = findUserListEntries();
        final WebElement entry;
        try {
            entry = entries.get(index);
        } catch (final IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("index too large", e);
        }
        final var link = entry.findElement(USER_LINK_LOCATOR);
        final var displayName = link.getText();
        link.click();
        final var userPage = new UserPage(this, displayName);
        userPage.awaitIsReady();
        return userPage;
    }

}
