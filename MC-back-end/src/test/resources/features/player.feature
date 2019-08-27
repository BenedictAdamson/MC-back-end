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
Feature: Player
  Mission Command is a multi-player game.
  To conserve resources, play on a server is restricted to only known (and presumably trusted) users.

  Scenario: Get players of fresh instance
    Given a fresh instance of MC
    And not logged in
    And not presenting a CSRF token
    When getting the players
    # The path of the players resource is /player
    Then MC serves the resource
    # And there is only one player, the administrator, with the default name
    And the response message is a list of players
    And the list of players has one player
    And the list of players includes the administrator
    And the list of players includes a player named "Administrator"
    
  Scenario Outline: Login
    Given that player "<player>" exists with  password "<password>"
    And not logged in
    And presenting a valid CSRF token
    When log in as "<player>" using password "<password>"
    Then MC accepts the login
    
    Examples:
      |player|password|
      |John|letmein|
      |Jeff|pasword123|
    
  Scenario Outline: Add player
    Given user authenticated as Administrator
    And presenting a valid CSRF token
    When adding a player named "<name>" with  password "<password>"
    Then MC accepts the addition
    And can get the list of players
    And the list of players includes a player named "<name>"
    
    Examples:
      |name|password|
      |John|letmein|
      |Jeff|password123|
    
  Scenario Outline: Only administrator may add player
    Given logged in as "<name>"
    And presenting a valid CSRF token
    When adding a player named "<new-name>" with  password "<password>"
    Then MC forbids the request
    
    Examples:
      |name|new-name|password|
      |John|Jeff|letmein|
      |Jeff|John|password123|