package uk.badamson.mc.presentation;

import uk.badamson.mc.Authority;
import uk.badamson.mc.User;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

final class Fixtures {


    static final User ADMINISTRATOR = User
            .createAdministrator("password");

    private static String createUserName(@Nonnull final UUID id) {
        return "jeff-" + id;
    }

    static String createUserName() {
        return createUserName(UUID.randomUUID());
    }

    static User createUserWithAllRoles() {
        final var id = UUID.randomUUID();
        return new User(id, createUserName(id),
                "secret", Authority.ALL, true, true, true, true);
    }

    static User createUserWithPlayerRole() {
        final var id = UUID.randomUUID();
        return new User(id, createUserName(id),
                "password1", Set.of(Authority.ROLE_PLAYER), false, false, false,
                false);
    }

    static User createUserWithManageGamesRole() {
        final var id = UUID.randomUUID();
        return new User(id, createUserName(id),
                "password2", Set.of(Authority.ROLE_MANAGE_GAMES), true, true, true,
                true);
    }

}
