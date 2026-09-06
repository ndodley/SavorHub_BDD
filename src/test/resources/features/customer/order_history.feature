Feature: Order history

  As a logged-in SavorHub customer
  I want to see my past orders and their details
  So that I can track what I've ordered

  Background:
    Given I am on the SavorHub login page
    And I log in with a valid email and password
    And my cart is empty

  Scenario: A placed order appears in My Orders with the correct status
    Given I am on the SavorHub menu page
    And I add the first menu item to my cart
    And I go to the Cart page
    And I proceed to checkout
    And I place the order
    When I go to my orders page
    Then I should see my most recently placed order with a status of "Pending_Payment"

  Scenario: Viewing an order's details shows the same order and total as My Orders
    Given I am on the SavorHub menu page
    And I add the first menu item to my cart
    And I go to the Cart page
    And I proceed to checkout
    And I place the order
    And I go to my orders page
    When I view that order's details
    Then the order details should show the same order ID and total as My Orders
    And I should see at least one item listed in the order

  Scenario: An order's details require being logged back in after logging out
    Given I am on the SavorHub menu page
    And I add the first menu item to my cart
    And I go to the Cart page
    And I proceed to checkout
    And I place the order
    And I go to my orders page
    When I log out
    And I should see the login and register links in the navbar
    And I visit that order's details page directly
    Then I should be redirected to the login page

  Scenario: Another logged-in customer cannot view someone else's order details
    Given I am on the SavorHub menu page
    And I add the first menu item to my cart
    And I go to the Cart page
    And I proceed to checkout
    And I place the order
    And I go to my orders page
    When I log out
    And I am on the SavorHub registration page
    And I register with a new unique email and valid details
    And I visit that order's details page directly
    Then I should not see that order's details
