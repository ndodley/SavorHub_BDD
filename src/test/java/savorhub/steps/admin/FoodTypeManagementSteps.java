package savorhub.steps.admin;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import savorhub.steps.BaseSteps;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for food_type_management.feature.
 *
 * <p>Locators come straight from SavorHub's real markup and code at
 * Pages/Admin/FoodTypes/{Index,Create,Edit,Delete}.cshtml(.cs) and
 * Models/FoodType.cs:
 *   - All four pages are [Authorize(Roles = SD.ManagerRole)]-protected -
 *     unauthenticated redirects to the login page, and an authenticated
 *     non-Manager redirects to /Identity/Account/AccessDenied.
 *   - FoodType only has one field, Name (asp-for -> id "FoodType_Name"),
 *     with a plain [Required] attribute and no other validation rule -
 *     unlike Category there's no DisplayOrder and no handler-level
 *     cross-field rule, so a blank Name is the only rejection case, and
 *     jQuery's unobtrusive validation catches it client-side (no server
 *     round trip) with the default message "The Name field is required."
 *   - Index.cshtml's table has Food Type Name in column 1 and Edit/Delete
 *     links (asp-route-id, rendered as a "?id=" query string since none
 *     of these pages declare a route template) in column 2 - a food type
 *     is located by matching its name text in column 1, then walking to
 *     the Actions column via following-sibling, rather than assuming row
 *     order (food types accumulate across test runs since names are
 *     randomized and nothing but the "delete" scenario ever cleans one
 *     up, same tolerance this suite already has for Category management
 *     and Cart & Checkout's leftover Pending orders).
 *   - The Create/Edit/Delete submit buttons are located via
 *     ".admin-card button[type='submit']", not a bare
 *     "button[type='submit']" - the shared _Layout's account dropdown
 *     (Pages/Shared/_LoginPartial.cshtml) renders its own hidden
 *     button[type=submit] ("Logout") earlier in the DOM, inside the
 *     fixed header, so an unscoped selector matches that invisible
 *     button first and throws ElementNotInteractableException.
 *   - Edit/Delete row-link clicks use a scroll-then-JS-click, not a plain
 *     WebElement.click() - Create/Edit/Delete all set TempData["success"],
 *     which _Layout.cshtml shows as a toastr notification on the next
 *     page load, and a native click on a row far down this accumulating
 *     table is prone to ElementClickInterceptedException from that toast
 *     (same fix already applied in CategoryManagementSteps).
 * If SavorHub's markup or code changes, re-check the real DOM/source
 * rather than trusting this comment.
 */
public class FoodTypeManagementSteps extends BaseSteps {

    private static final String BASE_URL = CONFIG.getProperty("base.url");

    private String foodTypeName;

    private WebDriverWait shortWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    private By foodTypeNameCell() {
        return By.xpath("//td[normalize-space(text())='" + foodTypeName + "']");
    }

    private void fillAndSubmitFoodTypeForm(String name) {
        WebElement nameInput = shortWait().until(ExpectedConditions.presenceOfElementLocated(By.id("FoodType_Name")));
        nameInput.clear();
        nameInput.sendKeys(name);

        // Scoped to .admin-card, not a bare "button[type='submit']" selector - see
        // the class javadoc for why an unscoped selector matches the shared
        // layout's hidden Logout button instead of this page's own button.
        driver.findElement(By.cssSelector(".admin-card button[type='submit']")).click();
    }

    @Given("I am on the Admin Food Types page")
    public void i_am_on_the_admin_food_types_page() {
        navigateTo(BASE_URL + "/Admin/FoodTypes");
    }

    @When("I visit the Admin Food Types page directly")
    public void i_visit_the_admin_food_types_page_directly() {
        navigateTo(BASE_URL + "/Admin/FoodTypes");
    }

    // Registered as a single @When and reused verbatim as a Given in the
    // edit/delete scenarios - Cucumber matches steps by text regardless of
    // which keyword introduces them (same technique CategoryManagementSteps
    // and ReviewSteps use).
    @When("I create a new food type with a unique name")
    public void i_create_a_new_food_type_with_a_unique_name() {
        foodTypeName = "Test Food Type " + UUID.randomUUID();
        navigateTo(BASE_URL + "/Admin/FoodTypes/Create");
        fillAndSubmitFoodTypeForm(foodTypeName);
    }

    @Given("I have created a food type with a unique name")
    public void i_have_created_a_food_type_with_a_unique_name() {
        i_create_a_new_food_type_with_a_unique_name();
    }

    @Then("I should see that food type in the list")
    public void i_should_see_that_food_type_in_the_list() {
        shortWait().until(ExpectedConditions.presenceOfElementLocated(foodTypeNameCell()));
    }

    @When("I try to create a food type with a blank name")
    public void i_try_to_create_a_food_type_with_a_blank_name() {
        navigateTo(BASE_URL + "/Admin/FoodTypes/Create");
        fillAndSubmitFoodTypeForm("");
    }

    @Then("I should see a validation error that the name is required")
    public void i_should_see_a_validation_error_that_the_name_is_required() {
        WebElement error = shortWait().until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'field is required')]")));
        assertTrue(error.isDisplayed());
    }

    @When("I edit that food type to a new name")
    public void i_edit_that_food_type_to_a_new_name() {
        WebElement nameCell = shortWait().until(ExpectedConditions.presenceOfElementLocated(foodTypeNameCell()));
        WebElement editLink = nameCell.findElement(
                By.xpath("following-sibling::td//a[contains(@href, 'Edit')]"));
        clickThenAwait(editLink, By.id("FoodType_Name"));

        foodTypeName = "Updated Food Type " + UUID.randomUUID();
        fillAndSubmitFoodTypeForm(foodTypeName);
    }

    @Then("I should see the updated food type in the list")
    public void i_should_see_the_updated_food_type_in_the_list() {
        i_should_see_that_food_type_in_the_list();
    }

    @When("I delete that food type")
    public void i_delete_that_food_type() {
        WebElement nameCell = shortWait().until(ExpectedConditions.presenceOfElementLocated(foodTypeNameCell()));
        WebElement deleteLink = nameCell.findElement(
                By.xpath("following-sibling::td//a[contains(@href, 'Delete')]"));

        // Same .admin-card scoping as fillAndSubmitFoodTypeForm() above, and for the
        // same reason - Pages/Admin/FoodTypes/Delete.cshtml's own Delete button would
        // otherwise lose to the hidden Logout button rendered earlier in the header.
        WebElement deleteButton = clickThenAwait(deleteLink, By.cssSelector(".admin-card button[type='submit']"));
        deleteButton.click();
    }

    @Then("I should not see that food type in the list")
    public void i_should_not_see_that_food_type_in_the_list() {
        shortWait().until(d -> d.findElements(foodTypeNameCell()).isEmpty());
    }
}
