Feature: Customer login

  As a returning SavorHub customer
  I want to log into my account
  So that I can order food and manage my account

  Scenario: Successful login with valid credentials
    Given I am on the SavorHub login page
    When I log in with a valid email and password
    Then I should see the account menu in the navbar

  Scenario: Login fails with an incorrect password
    Given I am on the SavorHub login page
    When I log in with an incorrect password
    Then I should see a login error message
