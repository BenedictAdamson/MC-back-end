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

import org.hamcrest.Matcher;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * <p>
 * A <i>page object</i> for a user page.
 * </p>
 */
@Immutable
public final class UserPage extends Page {

    private static final String BASE = "/user/";
    private static final By USERNAME_LOCATOR = By.id("username");
    private static final By ROLES_LIST_LOCATOR = By.id("roles");

    private final Matcher<String> containsDisplayName;

    /**
     * <p>
     * Construct a user page associated with a given users page and having a
     * given (expected) display name..
     * </p>
     *
     * @param usersPage   The users page.
     * @param displayName The expected display name.
     * @throws NullPointerException <ul>
     *                                         <li>If {@code usersPage} is null.</li>
     *                                         <li>If {@code displayName} is null.</li>
     *                                         </ul>
     */
    public UserPage(final UsersPage usersPage, final String displayName) {
        super(usersPage);
        Objects.requireNonNull(displayName, "displayName");
        containsDisplayName = containsString(displayName);
    }

    public void assertIncludesUserName() {
        assertIncludesUserName(getBody());
    }

    private void assertIncludesUserName(final WebElement body) {
        final var element = assertHasElement("Has a username element", body,
                USERNAME_LOCATOR);
        assertThat("Username element text includes the user name",
                element.getText(), containsDisplayName);
    }

    public void assertListsRolesOfUser() {
        assertListsRolesOfUser(getBody());
    }

    private void assertListsRolesOfUser(final WebElement body) {
        assertHasElement(body, ROLES_LIST_LOCATOR);
    }

    @Override
    protected void assertValidBody(@Nonnull final WebElement body) {
        assertAll(() -> super.assertValidBody(body),
                () -> assertIncludesUserName(body),
                () -> assertListsRolesOfUser(body));
    }

    @Override
    protected void assertValidPath(@Nonnull final String path) {
        assertThat("path", path, startsWith(BASE));
    }

}
