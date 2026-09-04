Feature: Admin menu item management

  As a SavorHub Manager
  I want to create, edit, and delete menu items
  So that customers always see an accurate, up-to-date menu

  Background:
    Given I am on the SavorHub login page
    And I log in with valid Manager credentials

  Scenario: Creating a new menu item shows it in the menu item list
    Given I am on the Admin Menu Items page
    When I create a new menu item with a unique name, a price, and an image
    Then I should see that menu item in the list

  Scenario: Creating a menu item without an image is rejected
    Given I am on the Admin Menu Items page
    When I try to create a menu item without choosing an image
    Then I should see a validation error that an image is required

  Scenario: Editing a menu item updates its name in the list
    Given I have created a menu item with a unique name, a price, and an image
    When I edit that menu item to a new name
    Then I should see the updated menu item in the list

  Scenario: Deleting a menu item removes it from the list
    Given I have created a menu item with a unique name, a price, and an image
    When I delete that menu item
    Then I should not see that menu item in the list

  Scenario: The Admin Menu Items page requires being logged in
    When I log out
    And I visit the Admin Menu Items page directly
    Then I should be redirected to the login page

  Scenario: A plain customer cannot reach the Admin Menu Items page
    When I log out
    And I am on the SavorHub login page
    And I log in with a valid email and password
    And I visit the Admin Menu Items page directly
    Then I should be redirected to the access denied page
