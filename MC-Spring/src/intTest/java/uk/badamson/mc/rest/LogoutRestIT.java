package uk.badamson.mc.rest;
/*
 * © Copyright Benedict Adamson 2020-24.
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.McBackEndClient;
import uk.badamson.mc.McBackEndProcess;
import uk.badamson.mc.ProcessFixtures;
import uk.badamson.mc.presentation.SecurityConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.concurrent.TimeoutException;

/**
 * Tests {@link SecurityConfiguration}
 */
public class LogoutRestIT {
    private static final String MONGO_DB_PASSWORD = "LetMeIn1";
    private static final String ADMINISTRATOR_PASSWORD = ProcessFixtures.ADMINISTRATOR.getPassword();
    private static final String PATH = "/logout";

    private static MongoDBContainer MONGO_DB_CONTAINER;
    private static McBackEndProcess MC_BACK_END_PROCESS;
    private static McBackEndClient MC_BACK_END_CLIENT;

    @BeforeAll
    public static void setUp() throws TimeoutException {
        MONGO_DB_CONTAINER = new MongoDBContainer(ProcessFixtures.MONGO_DB_IMAGE);
        MONGO_DB_CONTAINER.start();
        final var mongoDBPath = MONGO_DB_CONTAINER.getReplicaSetUrl();
        MC_BACK_END_PROCESS = new McBackEndProcess(mongoDBPath, MONGO_DB_PASSWORD, ADMINISTRATOR_PASSWORD);
        MC_BACK_END_CLIENT = new McBackEndClient(
                "localhost", MC_BACK_END_PROCESS.getServerPort(), ADMINISTRATOR_PASSWORD
        );
    }

    @AfterAll
    public static void tearDown() {
        MC_BACK_END_PROCESS.close();
        MONGO_DB_CONTAINER.close();
    }

    @Test
    public void noAuthentication() {
        final var response = test(ProcessFixtures.ADMINISTRATOR, null, true, true);

        response.expectStatus().isNoContent();
    }

    @Test
    public void noCsrfToken() {
        final var response = test(ProcessFixtures.ADMINISTRATOR, ProcessFixtures.ADMINISTRATOR, true, false);

        response.expectStatus().isForbidden();
    }

    @Test
    public void noSession() {
        final var response = test(ProcessFixtures.ADMINISTRATOR, ProcessFixtures.ADMINISTRATOR, false, true);

        response.expectStatus().isNoContent();
    }

    @Test
    public void bareRequest() {
        final var response = test(ProcessFixtures.ADMINISTRATOR, null, false, false);

        response.expectStatus().isForbidden();
    }

    @Test
    public void fullRequestAdministrator() {
        final var response = test(ProcessFixtures.ADMINISTRATOR, ProcessFixtures.ADMINISTRATOR, true, true);

        response.expectStatus().isNoContent();
    }

    @Test
    public void fullRequestNonAdministrator() {
        final var requestingUser = ProcessFixtures.createBasicUserDetailsWithAllRoles();
        MC_BACK_END_CLIENT.addUser(requestingUser);

        final var response = test(requestingUser, requestingUser, true, true);

        response.expectStatus().isNoContent();
    }

    private WebTestClient.ResponseSpec test(
            @Nonnull final BasicUserDetails loggedInUser,
            @Nullable final BasicUserDetails authenticatingUser,
            final boolean includeSessionCookie,
            final boolean includeXsrfToken
    ) {
        final var cookies = MC_BACK_END_CLIENT.login(loggedInUser);
        final var request = MC_BACK_END_CLIENT.connectWebTestClient().post().uri(PATH);
        McBackEndClient.secure(request, authenticatingUser, cookies, includeSessionCookie, includeXsrfToken);
        try {
            return request.exchange();
        } finally {
            MC_BACK_END_CLIENT.logout(loggedInUser, cookies);
        }
    }

}
