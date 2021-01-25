package uk.badamson.mc.service;

import javax.annotation.Nullable;

/*
 * © Copyright Benedict Adamson 2020.
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

/**
 * <p>
 * An exception class for indicating that an operation can not be performed
 * because the current state of a game does not allow the operation.
 * </p>
 */
@SuppressWarnings("serial")
public class IllegalGameStateException extends IllegalStateException {

   private static final String DEFAULT_MESSAGE = "Illegal game state";

   public IllegalGameStateException() {
      super(DEFAULT_MESSAGE);
   }

   /**
    * <p>
    * Constructs a new exception with a given {@linkplain #getMessage() detail
    * message}.
    * </p>
    *
    * @param message
    *           the detail message
    */
   public IllegalGameStateException(final String message) {
      super(message);
   }

   /**
    * <p>
    * Constructs a new exception with a given {@linkplain #getMessage() detail
    * message} and {@linkplain Throwable#getCause() cause}.
    * </p>
    *
    * @param message
    *           the detail message
    * @param cause
    *           the cause
    */
   public IllegalGameStateException(final String message,
            final Throwable cause) {
      super(message, cause);
   }

   /**
    * <p>
    * Constructs a new exception with a given {@linkplain #getCause() cause}.
    * </p>
    * <ul>
    * <li>If the given cause is not null, this exception has the
    * {@linkplain Throwable#getMessage() detail message} of the given cause as
    * its cause.</li>
    * </ul>
    *
    * @param cause
    *           the cause
    */
   public IllegalGameStateException(@Nullable final Throwable cause) {
      super(cause);
   }

}
