Feature: Menu browsing

  As a SavorHub customer
  I want to browse the menu
  So that I can see what dishes are available and view details on a dish

  Scenario: Browsing the menu shows available dishes
    Given I am on the SavorHub menu page
    Then I should see at least one menu item

  Scenario: Viewing a menu item's details
    Given I am on the SavorHub menu page
    When I click on the first menu item
    Then I should see that item's details page
