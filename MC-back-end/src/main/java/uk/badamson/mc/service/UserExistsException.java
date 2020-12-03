package uk.badamson.mc.service;
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
 * because a user already exists.
 * </p>
 */
@SuppressWarnings("serial")
public final class UserExistsException extends IllegalPlayerStateException {

   public UserExistsException() {
      super("User exists");
   }

}
