package uk.badamson.mc.presentation;

import uk.badamson.mc.spring.SpringAuthority;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

final class Fixtures {


    static final SpringUser ADMINISTRATOR = SpringUser
            .createAdministrator("password");

    private static String createUserName(@Nonnull final UUID id) {
        return "jeff-" + id;
    }

    static String createUserName() {
        return createUserName(UUID.randomUUID());
    }

    static SpringUser createUserWithAllRoles() {
        final var id = UUID.randomUUID();
        return new SpringUser(id, createUserName(id),"secret",
                SpringAuthority.ALL,
                true, true, true, true);
    }

    static SpringUser createUserWithPlayerRole() {
        final var id = UUID.randomUUID();
        return new SpringUser(id, createUserName(UUID.randomUUID()),"secret",
                Set.of(SpringAuthority.ROLE_PLAYER),
                true, true, true, true);
    }

    static SpringUser createUserWithManageGamesRole() {
        final var id = UUID.randomUUID();
        return new SpringUser(id, createUserName(UUID.randomUUID()),"secret",
                Set.of(SpringAuthority.ROLE_MANAGE_GAMES),
                true, true, true, true);
    }

}
