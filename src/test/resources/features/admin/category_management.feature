Feature: Admin category management

  As a SavorHub Manager
  I want to create, edit, and delete menu categories
  So that the menu stays organized as the business changes

  Background:
    Given I am on the SavorHub login page
    And I log in with valid Manager credentials

  Scenario: Creating a new category shows it in the category list
    Given I am on the Admin Categories page
    When I create a new category with a unique name and a display order
    Then I should see that category in the list with the correct display order

  Scenario: A category name that exactly matches its display order is rejected
    Given I am on the Admin Categories page
    When I try to create a category whose name is the same as its display order
    Then I should see a validation error that the display order cannot match the name

  Scenario: Editing a category updates its name and display order in the list
    Given I have created a category with a unique name and a display order
    When I edit that category to a new name and display order
    Then I should see the updated category in the list

  Scenario: Deleting a category removes it from the list
    Given I have created a category with a unique name and a display order
    When I delete that category
    Then I should not see that category in the list

  Scenario: The Admin Categories page requires being logged in
    When I log out
    And I visit the Admin Categories page directly
    Then I should be redirected to the login page

  Scenario: A plain customer cannot reach the Admin Categories page
    When I log out
    And I am on the SavorHub login page
    And I log in with a valid email and password
    And I visit the Admin Categories page directly
    Then I should be redirected to the access denied page
