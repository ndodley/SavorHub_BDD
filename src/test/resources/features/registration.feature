Feature: Registration

  As a new visitor to SavorHub
  I want to create an account
  So that I can log in and place orders

  Scenario: Registering a new account signs the user in automatically
    Given I am on the SavorHub registration page
    When I register with a new unique email and valid details
    Then I should see my new account's email in the navbar
