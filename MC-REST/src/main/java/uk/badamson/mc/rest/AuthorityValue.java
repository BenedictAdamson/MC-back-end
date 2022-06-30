package uk.badamson.mc.rest;

import uk.badamson.mc.Authority;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum AuthorityValue {
    ROLE_PLAYER,
    ROLE_MANAGE_USERS,
    ROLE_MANAGE_GAMES;

    private static final Map<Authority, AuthorityValue> CONVERT_TO_VALUE_MAP;
    static {
        CONVERT_TO_VALUE_MAP = new EnumMap<>(Authority.class);
        CONVERT_TO_VALUE_MAP.put(Authority.ROLE_PLAYER, AuthorityValue.ROLE_PLAYER);
        CONVERT_TO_VALUE_MAP.put(Authority.ROLE_MANAGE_USERS, AuthorityValue.ROLE_MANAGE_USERS);
        CONVERT_TO_VALUE_MAP.put(Authority.ROLE_MANAGE_GAMES, AuthorityValue.ROLE_MANAGE_GAMES);
    }

    private static final Map<AuthorityValue, Authority> CONVERT_FROM_VALUE_MAP;
    static {
        CONVERT_FROM_VALUE_MAP = new EnumMap<>(AuthorityValue.class);
        CONVERT_FROM_VALUE_MAP.put(AuthorityValue.ROLE_PLAYER, Authority.ROLE_PLAYER);
        CONVERT_FROM_VALUE_MAP.put(AuthorityValue.ROLE_MANAGE_USERS, Authority.ROLE_MANAGE_USERS);
        CONVERT_FROM_VALUE_MAP.put(AuthorityValue.ROLE_MANAGE_GAMES, Authority.ROLE_MANAGE_GAMES);
    }

    @Nonnull
    public static AuthorityValue convertToValue(@Nonnull Authority authority) {
        return CONVERT_TO_VALUE_MAP.get(authority);
    }

    @Nonnull
    public static  Authority convertFromValue(@Nonnull AuthorityValue authority) {
        return CONVERT_FROM_VALUE_MAP.get(authority);
    }

    @Nonnull
    public static Set<AuthorityValue> convertToValue(@Nonnull Set<Authority> authorities) {
        return authorities.stream().map(AuthorityValue::convertToValue).collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    public static Set<Authority> convertFromValue(@Nonnull Set<AuthorityValue> authorities) {
        return authorities.stream().map(AuthorityValue::convertFromValue).collect(Collectors.toUnmodifiableSet());
    }
}
