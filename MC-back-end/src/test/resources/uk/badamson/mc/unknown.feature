# © Copyright Benedict Adamson 2019.
#
# This file is part of MC.
#
# MC is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# MC is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with MC.  If not, see <https://www.gnu.org/licenses/>.
#
Feature: Unknown
  Attempts to access unknown or incorrectly named things should give useful error responses.

  Scenario Outline: Get unknown resource
    Given a fresh instance of MC
    And not logged in
    And not presenting a CSRF token
    When getting the unknown resource at "<path>"
    Then MC replies with Not Found

    Examples: 
      |path|
      |/xxxxx|
      |/players|

  Scenario Outline: Modify unknown resource
    Given a fresh instance of MC
    And not logged in
    And not presenting a CSRF token
    When modifying the unknown resource with a "<verb>" at "<path>"
    Then MC replies with Forbidden

    Examples: 
      |verb|path|
      |POST|/xxxxx|
      |PUT|/players|
      |DELETE|/players|
      