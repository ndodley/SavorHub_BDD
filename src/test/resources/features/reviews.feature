Feature: Reviews

  As a logged-in SavorHub customer
  I want to write and edit reviews on menu items
  So that I can share my experience with other customers

  Background:
    Given I am on the SavorHub login page
    And I log in with a valid email and password
    And I am on the SavorHub menu page
    And I click on the first menu item
    And I have no existing review on this item

  Scenario: Submitting a review shows it with the correct rating and content
    When I submit a review with a rating of 5 and content "Absolutely delicious, will order again!"
    Then I should see my review listed with a rating of 5 and content "Absolutely delicious, will order again!"

  Scenario: Editing my review updates its rating and content
    Given I submit a review with a rating of 3 and content "It was okay, a bit too salty."
    When I edit my review to a rating of 5 and content "Updated: actually really good on the second try!"
    Then I should see my review listed with a rating of 5 and content "Updated: actually really good on the second try!"

  Scenario: The review API's delete endpoint has no ownership check, so any logged-in user can delete someone else's review
    Given I submit a review with a rating of 4 and content "Great flavor, would recommend."
    When I log out
    And I am on the SavorHub registration page
    And I register with a new unique email and valid details
    And the newly registered user deletes my review by calling the review API directly
    Then the review API reports the delete as successful
    When I log out
    And I am on the SavorHub login page
    And I log in with a valid email and password
    And I am on the SavorHub menu page
    And I click on the first menu item
    Then I should no longer see my review on that item
