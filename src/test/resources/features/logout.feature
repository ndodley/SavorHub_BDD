Feature: Logout

  As a logged-in SavorHub customer
  I want to log out
  So that my session ends and my account isn't left open on a shared device

  Scenario: Logging out ends the session
    Given I am on the SavorHub login page
    And I log in with a valid email and password
    When I log out
    Then I should see the login and register links in the navbar
