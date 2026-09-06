Feature: Favorites

  As a logged-in SavorHub customer
  I want to save and remove menu items from my favorites
  So that I can quickly find dishes I like without searching the whole menu

  Scenario: Favoriting a menu item shows it on the Favorites page
    Given I am on the SavorHub login page
    And I log in with a valid email and password
    And I am on the SavorHub menu page
    When I favorite the first menu item
    And I go to the Favorites page
    Then I should see that item on the Favorites page

  Scenario: Removing a favorite asks for confirmation before it disappears
    Given I am on the SavorHub login page
    And I log in with a valid email and password
    And I am on the SavorHub menu page
    And I favorite the first menu item
    When I go to the Favorites page
    And I remove that item from my favorites and confirm the prompt
    Then I should not see that item on the Favorites page
