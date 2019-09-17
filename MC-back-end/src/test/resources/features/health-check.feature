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
Feature: Health-check
The MC server should provide a health-check suitable for use by Kubernetes.

  Scenario: Health-check indicates that health
    Given a fresh instance of MC
    And not logged in
    And not presenting a CSRF token
    When getting the unknown resource at "/actuator/health"
    Then MC serves the resource
