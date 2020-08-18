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
import uk.badamson.mc.User;

/**
 * <p>
 * The service layer of the Mission Command game.
 * </p>
 */
public interface Service extends ReactiveUserDetailsService {

   /**
    * <p>
    * Add a user to the {@linkplain #getUsers() list of users}.
    * </p>
    * <ul>
    * <li>Always returns a (non null) publisher.</li>
    * <li>As for all publishers, the returned publisher will not
    * {@linkplain Subscriber#onNext(Object) provide} a null element to a
    * subscriber.</li>
    * <li>A subsequently retrieved {@linkplain #getUsers() sequence of the
    * users} will include a {@linkplain User user}
    * {@linkplain User#equals(Object) equivalent to} the given user.</li>
    * <li>Subsequently {@linkplain #findByUsername(String) finding user details}
    * using the {@linkplain User#getUsername() username} of the given user
    * will retrieve {@linkplain UserDetails user details} equivalent to the user
    * details of the given user. However, the
    * {@linkplain UserDetails#getPassword() password} will have been encrypted
    * using the {@linkplain #getPasswordEncoder() password encoder} of this
    * service.</li>
    * </ul>
    *
    * @param user
    *           The user to add, with an unencrypted
    *           {@linkplain User#getPassword() password}.
    * @return a {@linkplain Publisher publisher} that
    *         {@linkplain Subscriber#onComplete() completes} on addition of the
    *         user or {@linkplain Subscriber#onError(Throwable) publishes an
    *         error condition} if the addition fails.
    * @throws NullPointerException
    *            If {@code user} is null
    * @throws IllegalArgumentException
    *            If the {@linkplain User#getUsername() username} of
    *            {@code user} indicates it is the
    *            {@linkplain User#ADMINISTRATOR_USERNAME administrator}.
    */
   @Secured("ROLE_ADMIN")
   Mono<Void> add(final User user);

   /**
    * {@inheritDoc}
    *
    * <ul>
    * <li>Always have user details for the
    * {@linkplain User#ADMINISTRATOR_USERNAME administrator}.</li>
    * <li>The {@linkplain User#ADMINISTRATOR_USERNAME administrator} has a
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
    * Retrieve a publisher of the list of the current users of this instance
    * of the Mission Command game.
    * </p>
    * <ul>
    * <li>Always returns a (non null) publisher.</li>
    * <li>As for all publishers, the returned publisher will not
    * {@linkplain Subscriber#onNext(Object) provide} a null element to a
    * subscriber.</li>
    * <li>The list of users always {@linkplain Flux#hasElement(Object) has a}
    * user with the {@linkplain User#ADMINISTRATOR_USERNAME administrator
    * username} as its {@linkplain User#getUsername() username}.</li>
    * <li>Does not contain users with duplicate
    * {@linkplain User#getUsername() usernames}.</li>
    * </ul>
    *
    * @return a {@linkplain Publisher publisher} of the users.
    */
   Flux<User> getUsers();

}
