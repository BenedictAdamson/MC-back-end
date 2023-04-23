package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2020-23.
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.badamson.mc.Fixtures;
import uk.badamson.mc.service.UserSpringService;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nullable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests {@link SecurityConfiguration}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
public class LogoutRESTTest {

    private static final String PATH = "/logout";

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(Fixtures.MONGO_DB_IMAGE);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private UserSpringService service;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void noAuthentication() throws Exception {
        final var session = new MockHttpSession();

        final var response = logout(session, null, true);

        response.andExpect(status().isNoContent());
    }

    @Test
    public void noCsrfToken() throws Exception {
        final var user = createUser();
        final var session = new MockHttpSession();

        final var response = logout(session, user, false);

        assertAll(() -> response.andExpect(status().isForbidden()),
                () -> assertThat("session is still valid",
                        !session.isInvalid()));
    }

    @Test
    public void noSession() throws Exception {
        final var user = createUser();

        final var response = logout(null, user, true);

        response.andExpect(status().isNoContent());
    }

    private SpringUser createUser() {
        return SpringUser.convertToSpring(service.add(Fixtures.createBasicUserDetailsWithAllRoles()));
    }

    @Test
    public void withSession() throws Exception {
        final var user = createUser();
        final var session = new MockHttpSession();

        final var response = logout(session, user, true);

        assertAll(() -> response.andExpect(status().isNoContent()),
                () -> assertThat("session is no longer valid",
                        session.isInvalid()));
    }

    private ResultActions logout(@Nullable MockHttpSession session, @Nullable SpringUser user, boolean withCsrfToken) throws Exception {
        var request = post(PATH);
        if (session != null) {
            request = request.session(session);
        }
        if (user != null) {
            service.add(Fixtures.createBasicUserDetailsWithAllRoles());
            request = request.with(user(user));
        }
        if (withCsrfToken) {
            request = request.with(csrf());
        }

        return mockMvc.perform(request);
    }

}
