Feature: Admin review moderation

  As a SavorHub Manager
  I want to view, edit, and delete customer reviews
  So that I can moderate inappropriate or incorrect review content

  Background:
    Given I am on the SavorHub login page
    And I log in with valid Manager credentials

  Scenario: A Manager can view the Review List page
    When I visit the Admin Reviews page directly
    Then I should see the Review List page

  Scenario: The Review List page requires being logged in
    When I log out
    And I visit the Admin Reviews page directly
    Then I should be redirected to the login page

  Scenario: A plain customer cannot reach the Review List page
    When I log out
    And I am on the SavorHub login page
    And I log in with a valid email and password
    And I visit the Admin Reviews page directly
    Then I should be redirected to the access denied page

  Scenario: A Manager can edit a customer's review
    Given I log out
    And I am on the SavorHub login page
    And I log in with a valid email and password
    And I am on the SavorHub menu page
    And I click on the first menu item
    And I have no existing review on this item
    And I submit a review with a rating of 3 and content "Admin moderation check: needs editing"
    And I log out
    And I am on the SavorHub login page
    And I log in with valid Manager credentials
    And I visit the Admin Reviews page directly
    When I edit the review containing "Admin moderation check: needs editing" to a rating of 5 and content "Admin moderation check: edited by manager"
    Then I should see a review in the list with a rating of 5 and content "Admin moderation check: edited by manager"

  Scenario: A Manager can delete a customer's review
    Given I log out
    And I am on the SavorHub login page
    And I log in with a valid email and password
    And I am on the SavorHub menu page
    And I click on the first menu item
    And I have no existing review on this item
    And I submit a review with a rating of 2 and content "Admin moderation check: needs deleting"
    And I log out
    And I am on the SavorHub login page
    And I log in with valid Manager credentials
    And I visit the Admin Reviews page directly
    When I delete the review containing "Admin moderation check: needs deleting"
    Then I should not see a review in the list with content "Admin moderation check: needs deleting"

  Scenario: Editing a review with blank content is rejected
    Given I log out
    And I am on the SavorHub login page
    And I log in with a valid email and password
    And I am on the SavorHub menu page
    And I click on the first menu item
    And I have no existing review on this item
    And I submit a review with a rating of 4 and content "Admin moderation check: blank content attempt"
    And I log out
    And I am on the SavorHub login page
    And I log in with valid Manager credentials
    And I visit the Admin Reviews page directly
    When I try to clear the content of the review containing "Admin moderation check: blank content attempt" and save
    Then I should see a validation error that content is required
