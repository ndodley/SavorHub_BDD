Feature: Account management

  As a logged-in SavorHub customer
  I want to change my password and update my profile
  So that I can keep my account secure and up to date

  Background:
    Given I am on the SavorHub registration page
    And I register with a new unique email and valid details

  Scenario: Changing my password with the correct current password succeeds
    Given I am on the SavorHub change password page
    When I change my password to a new valid password
    Then I should see a status message that my password has been changed

  Scenario: Changing my password with an incorrect current password shows an error
    Given I am on the SavorHub change password page
    When I try to change my password with the wrong current password
    Then I should see a password change error and remain on the change password page

  Scenario: Updating my phone number on the profile page succeeds
    Given I am on the SavorHub manage profile page
    When I update my phone number to a new value
    Then I should see a status message that my profile has been updated
