Feature: Password reset

  As a SavorHub customer who forgot their password
  I want to request a password reset
  So that I can regain access to my account without revealing whether an email is registered

  Scenario: Requesting a password reset for a registered email shows the check-your-email confirmation
    Given I am on the SavorHub forgot password page
    When I request a password reset for a registered email
    Then I should see the check-your-email confirmation

  Scenario: Requesting a password reset for an email that isn't registered shows the same confirmation
    Given I am on the SavorHub forgot password page
    When I request a password reset for an email that isn't registered
    Then I should see the check-your-email confirmation

  Scenario: Reset Password refuses to load without a reset code
    When I visit the Reset Password page directly with no code
    Then I should see a message that a code must be supplied
