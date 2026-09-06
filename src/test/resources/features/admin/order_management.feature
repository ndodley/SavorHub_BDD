Feature: Admin order management

  As a SavorHub Manager
  I want to view the order list, manage in-progress orders, and view order details
  So that I can track and fulfill customer orders as they come in

  Background:
    Given I am on the SavorHub login page
    And I log in with valid Manager credentials

  Scenario: A Manager can view the Order List page
    When I visit the Admin Order List page directly
    Then I should see the Order List page

  Scenario: The Order List page requires being logged in
    When I log out
    And I visit the Admin Order List page directly
    Then I should be redirected to the login page

  Scenario: A plain customer cannot reach the Order List page
    When I log out
    And I am on the SavorHub login page
    And I log in with a valid email and password
    And I visit the Admin Order List page directly
    Then I should be redirected to the access denied page

  Scenario: A Manager can view the Manage Orders page
    When I visit the Manage Orders page directly
    Then I should see the Manage Orders page

  Scenario: The Manage Orders page requires being logged in
    When I log out
    And I visit the Manage Orders page directly
    Then I should be redirected to the login page

  Scenario: A plain customer cannot reach the Manage Orders page
    When I log out
    And I am on the SavorHub login page
    And I log in with a valid email and password
    And I visit the Manage Orders page directly
    Then I should be redirected to the access denied page

  Scenario: A Manager can view a completed order's details
    When I open the details page for a completed order
    Then I should see that order's details

  Scenario: Requesting details for a nonexistent order does not crash the page
    When I visit the details page for an order id that does not exist
    Then I should see a not found response

  Scenario: The Order Details page requires being logged in
    When I log out
    And I visit the Admin Order Details page directly
    Then I should be redirected to the login page

  Scenario: A plain customer cannot reach the Order Details page
    When I log out
    And I am on the SavorHub login page
    And I log in with a valid email and password
    And I visit the Admin Order Details page directly
    Then I should be redirected to the access denied page
