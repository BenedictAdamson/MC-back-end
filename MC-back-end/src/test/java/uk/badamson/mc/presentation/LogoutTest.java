package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2020,22.
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import uk.badamson.mc.TestConfiguration;
import uk.badamson.mc.service.UserSpringService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class LogoutTest {

    private static final String PATH = "/logout";

    @Autowired
    private UserSpringService service;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void logout_noAuthentication() throws Exception {
        service.add(Fixtures.createUserWithAllRoles());
        final var request = post(PATH).with(csrf());

        final var response = mockMvc.perform(request);

        response.andExpect(status().isNoContent());
    }

    @Test
    public void logout_noCsrfToken() throws Exception {
        final var user = Fixtures.createUserWithAllRoles();
        service.add(user);
        final var session = new MockHttpSession();
        final var request = post(PATH).with(user(user)).session(session);

        final var response = mockMvc.perform(request);

        assertAll(() -> response.andExpect(status().isForbidden()),
                () -> assertThat("session is still valid",
                        !session.isInvalid()));
    }

    @Test
    public void logout_noSession() throws Exception {
        service.add(Fixtures.createUserWithAllRoles());
        final var request = post(PATH).with(user(Fixtures.createUserWithAllRoles())).with(csrf());

        final var response = mockMvc.perform(request);

        response.andExpect(status().isNoContent());
    }

    @Test
    public void logout_withSession() throws Exception {
        service.add(Fixtures.createUserWithAllRoles());
        final var session = new MockHttpSession();
        final var request = post(PATH).with(user(Fixtures.createUserWithAllRoles())).with(csrf())
                .session(session);

        final var response = mockMvc.perform(request);

        assertAll(() -> response.andExpect(status().isNoContent()),
                () -> assertThat("session is no longer valid",
                        session.isInvalid()));
    }

}
