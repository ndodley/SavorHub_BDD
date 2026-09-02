Feature: Homepage and navigation

  As any visitor to SavorHub
  I want the homepage to load with its key sections and navigation links working
  So that I know the app's front door is functioning correctly

  Scenario: The homepage shows its key sections
    Given I am on the SavorHub homepage
    Then I should see the hero section with an Explore Menu button
    And I should see the Featured Items section
    And I should see the Why Choose Us section

  Scenario: The Explore Menu button navigates to the menu page
    Given I am on the SavorHub homepage
    When I click the Explore Menu button
    Then I should see at least one menu item

  Scenario: The footer's Privacy link navigates to the Privacy page
    Given I am on the SavorHub homepage
    When I click the Privacy link in the footer
    Then I should see the Privacy Policy page
