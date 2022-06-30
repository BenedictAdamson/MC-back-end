package uk.badamson.mc.presentation;

import uk.badamson.mc.Authority;
import uk.badamson.mc.BasicUserDetails;
import uk.badamson.mc.spring.SpringAuthority;
import uk.badamson.mc.spring.SpringUser;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.UUID;

final class Fixtures {


    static final BasicUserDetails ADMINISTRATOR = BasicUserDetails
            .createAdministrator("password");

    private static String createUserName(@Nonnull final UUID id) {
        return "jeff-" + id;
    }

    static String createUserName() {
        return createUserName(UUID.randomUUID());
    }

    static BasicUserDetails createBasicUserDetailsWithAllRoles() {
        final var id = UUID.randomUUID();
        return new BasicUserDetails(createUserName(id),"secret",
                Authority.ALL,
                true, true, true, true);
    }

    static BasicUserDetails createBasicUserDetailsWithPlayerRole() {
        final var id = UUID.randomUUID();
        return new BasicUserDetails(createUserName(id),"secret",
                EnumSet.of(Authority.ROLE_PLAYER),
                true, true, true, true);
    }

    static BasicUserDetails createBasicUserDetailsWithManageGamesRole() {
        final var id = UUID.randomUUID();
        return new BasicUserDetails(createUserName(id),"secret",
                EnumSet.of(Authority.ROLE_MANAGE_GAMES),
                true, true, true, true);
    }

    static SpringUser createUserWithAllRoles() {
        final var id = UUID.randomUUID();
        return new SpringUser(id, createUserName(id),"secret",
                SpringAuthority.ALL,
                true, true, true, true);
    }

}
