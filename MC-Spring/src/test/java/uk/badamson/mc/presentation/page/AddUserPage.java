package uk.badamson.mc.presentation.page;
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

import org.openqa.selenium.By;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * <p>
 * A <i>page object</i> for the add user page.
 * </p>
 */
@Immutable
public final class AddUserPage extends Page {

    private static final String PATH = "/user?add";

    private final UsersPage usersPage;

    /**
     * <p>
     * Construct a user page associated with a given users page.
     * </p>
     *
     * @param usersPage The users page.
     * @throws NullPointerException If {@code usersPage} is null.
     */
    public AddUserPage(final UsersPage usersPage) {
        super(usersPage);
        this.usersPage = usersPage;
    }

    @Override
    protected void assertValidPath(@Nonnull final String path) {
        assertThat("path", path, is(PATH));
    }

    public Page submitForm(final String user, final String password) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(password, "password");
        requireIsReady();

        getBody().findElement(By.name("username")).sendKeys(user);
        getBody().findElement(By.xpath("//input[@type='password']"))
                .sendKeys(password);
        getBody().findElement(By.xpath("//button[@type='submit']")).submit();

        /* Must either transition to the Users' Page, or report an error. */
        usersPage.awaitIsReady();
        if (usersPage.isCurrentPath()) {
            return usersPage;
        } else {
            return this;
        }
    }

}
