Feature: Cart and checkout

  As a logged-in SavorHub customer
  I want to add menu items to my cart, adjust quantities, and check out
  So that I can place a pickup order for the food I want

  Background:
    Given I am on the SavorHub login page
    And I log in with a valid email and password
    And my cart is empty

  Scenario: Adding a menu item to the cart shows it with the correct total
    Given I am on the SavorHub menu page
    When I add the first menu item to my cart
    And I go to the Cart page
    Then I should see that item in my cart
    And the cart total should equal that item's price

  Scenario: Adjusting quantity updates the total, and decreasing to zero removes the item
    Given I am on the SavorHub menu page
    And I add the first menu item to my cart
    And I go to the Cart page
    When I increase that item's quantity
    Then the cart should show a quantity of 2 for that item
    And the cart total should equal twice that item's price
    When I decrease that item's quantity
    Then the cart should show a quantity of 1 for that item
    And the cart total should equal that item's price
    When I decrease that item's quantity
    Then I should see the empty cart message

  Scenario: Removing an item from the cart empties it
    Given I am on the SavorHub menu page
    And I add the first menu item to my cart
    And I go to the Cart page
    When I remove that item from the cart
    Then I should see the empty cart message

  Scenario: Proceeding to checkout reaches the order summary and starts a Stripe checkout session
    Given I am on the SavorHub menu page
    And I add the first menu item to my cart
    And I go to the Cart page
    When I proceed to checkout
    Then I should see the order summary with the correct total
    When I place the order
    Then I should be redirected to Stripe's checkout page
