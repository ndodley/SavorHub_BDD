Feature: Admin food type management

  As a SavorHub Manager
  I want to create, edit, and delete food types
  So that menu items can be categorized correctly as the menu changes

  Background:
    Given I am on the SavorHub login page
    And I log in with valid Manager credentials

  Scenario: Creating a new food type shows it in the food type list
    Given I am on the Admin Food Types page
    When I create a new food type with a unique name
    Then I should see that food type in the list

  Scenario: A blank food type name is rejected
    Given I am on the Admin Food Types page
    When I try to create a food type with a blank name
    Then I should see a validation error that the name is required

  Scenario: Editing a food type updates its name in the list
    Given I have created a food type with a unique name
    When I edit that food type to a new name
    Then I should see the updated food type in the list

  Scenario: Deleting a food type removes it from the list
    Given I have created a food type with a unique name
    When I delete that food type
    Then I should not see that food type in the list

  Scenario: The Admin Food Types page requires being logged in
    When I log out
    And I visit the Admin Food Types page directly
    Then I should be redirected to the login page

  Scenario: A plain customer cannot reach the Admin Food Types page
    When I log out
    And I am on the SavorHub login page
    And I log in with a valid email and password
    And I visit the Admin Food Types page directly
    Then I should be redirected to the access denied page
