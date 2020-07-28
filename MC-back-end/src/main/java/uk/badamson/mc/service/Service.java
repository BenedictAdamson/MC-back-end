package uk.badamson.mc.service;
/*
 * © Copyright Benedict Adamson 2019-20.
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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.badamson.mc.Authority;
import uk.badamson.mc.Player;

/**
 * <p>
 * The service layer of the Mission Command game.
 * </p>
 */
public interface Service extends ReactiveUserDetailsService {

   /**
    * <p>
    * Add a player to the {@linkplain #getPlayers() list of players}.
    * </p>
    * <ul>
    * <li>Always returns a (non null) publisher.</li>
    * <li>As for all publishers, the returned publisher will not
    * {@linkplain Subscriber#onNext(Object) provide} a null element to a
    * subscriber.</li>
    * <li>A subsequently retrieved {@linkplain #getPlayers() sequence of the
    * players} will include a {@linkplain Player player}
    * {@linkplain Player#equals(Object) equivalent to} the given player.</li>
    * <li>Subsequently {@linkplain #findByUsername(String) finding user details}
    * using the {@linkplain Player#getUsername() username} of the given player
    * will retrieve {@linkplain UserDetails user details} equivalent to the user
    * details of the given player. However, the
    * {@linkplain UserDetails#getPassword() password} will have been encrypted
    * using the {@linkplain #getPasswordEncoder() password encoder} of this
    * service.</li>
    * </ul>
    *
    * @param player
    *           The player to add, with an unencrypted
    *           {@linkplain Player#getPassword() password}.
    * @return a {@linkplain Publisher publisher} that
    *         {@linkplain Subscriber#onComplete() completes} on addition of the
    *         player or {@linkplain Subscriber#onError(Throwable) publishes an
    *         error condition} if the addition fails.
    * @throws NullPointerException
    *            If {@code player} is null
    * @throws IllegalArgumentException
    *            If the {@linkplain Player#getUsername() username} of
    *            {@code player} indicates it is the
    *            {@linkplain Player#ADMINISTRATOR_USERNAME administrator}.
    */
   @Secured("ROLE_ADMIN")
   Mono<Void> add(final Player player);

   /**
    * {@inheritDoc}
    *
    * <ul>
    * <li>Always have user details for the
    * {@linkplain Player#ADMINISTRATOR_USERNAME administrator}.</li>
    * <li>The {@linkplain Player#ADMINISTRATOR_USERNAME administrator} has a
    * complete {@linkplain Authority set} of
    * {@linkplain UserDetails#getAuthorities() authorities}.</li>
    * </ul>
    */
   @Override
   Mono<UserDetails> findByUsername(String username);

   /**
    * <p>
    * The encoder that this service uses to encrypt passwords.
    * </p>
    * <ul>
    * <li>Always have a non-null password encoder.</li>
    * </ul>
    *
    * @return the encoder.
    */
   PasswordEncoder getPasswordEncoder();

   /**
    * <p>
    * Retrieve a publisher of the list of the current players of this instance
    * of the Mission Command game.
    * </p>
    * <ul>
    * <li>Always returns a (non null) publisher.</li>
    * <li>As for all publishers, the returned publisher will not
    * {@linkplain Subscriber#onNext(Object) provide} a null element to a
    * subscriber.</li>
    * <li>The list of players always {@linkplain Flux#hasElement(Object) has a}
    * player with the {@linkplain Player#ADMINISTRATOR_USERNAME administrator
    * username} as its {@linkplain Player#getUsername() username}.</li>
    * <li>Does not contain players with duplicate
    * {@linkplain Player#getUsername() usernames}.</li>
    * </ul>
    *
    * @return a {@linkplain Publisher publisher} of the players.
    */
   Flux<Player> getPlayers();

}
