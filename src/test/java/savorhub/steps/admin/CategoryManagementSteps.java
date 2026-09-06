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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for category_management.feature.
 *
 * <p>Locators come straight from SavorHub's real markup and code at
 * Pages/Admin/Categories/{Index,Create,Edit,Delete}.cshtml(.cs) and
 * Models/Category.cs:
 *   - All four pages are [Authorize(Roles = SD.ManagerRole)]-protected -
 *     unauthenticated redirects to the login page, and an authenticated
 *     non-Manager redirects to /Identity/Account/AccessDenied.
 *   - Category.Name and Category.DisplayOrder use the standard asp-for ->
 *     id convention, so their ids are "Category_Name" / "Category_DisplayOrder"
 *     on both Create and Edit (Delete renders the same two inputs but
 *     disabled, read-only).
 *   - DisplayOrder is a string property with [Range(1, 100)], not an int -
 *     .NET's RangeAttribute still validates a numeric string, so this
 *     suite always sends a plain digit string within that range.
 *   - Create/EditModel.OnPost has a handler-level rule beyond the model's
 *     own attributes: it rejects the form (error on Category.Name) when
 *     Name is exactly equal to DisplayOrder's string value - the
 *     "matches its display order" scenario exercises exactly that by
 *     sending the same digit string for both fields.
 *   - Index.cshtml's table has Category Name in column 1, Display Order
 *     in column 2, and Edit/Delete links (asp-route-id, rendered as a
 *     "?id=" query string since none of these pages declare a route
 *     template) in column 3 - a category is located by matching its name
 *     text in column 1, then walking to the Actions column via
 *     following-sibling, rather than assuming row order (categories
 *     accumulate across test runs since names are randomized and nothing
 *     but the "delete" scenario ever cleans one up, same tolerance this
 *     suite already has for Cart & Checkout's leftover Pending orders).
 *   - The Create/Edit/Delete submit buttons are located via
 *     ".admin-card button[type='submit']", not a bare
 *     "button[type='submit']" - the shared _Layout's account dropdown
 *     (Pages/Shared/_LoginPartial.cshtml) renders its own hidden
 *     button[type=submit] ("Logout") earlier in the DOM, inside the
 *     fixed header, so an unscoped selector matches that invisible
 *     button first and throws ElementNotInteractableException.
 * If SavorHub's markup or code changes, re-check the real DOM/source
 * rather than trusting this comment.
 */
public class CategoryManagementSteps extends BaseSteps {

    private static final String BASE_URL = CONFIG.getProperty("base.url");

    private String categoryName;
    private String categoryDisplayOrder;

    private WebDriverWait shortWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    private By categoryNameCell() {
        return By.xpath("//td[normalize-space(text())='" + categoryName + "']");
    }

    private void fillAndSubmitCategoryForm(String name, String displayOrder) {
        WebElement nameInput = shortWait().until(ExpectedConditions.presenceOfElementLocated(By.id("Category_Name")));
        nameInput.clear();
        nameInput.sendKeys(name);

        WebElement displayOrderInput = driver.findElement(By.id("Category_DisplayOrder"));
        displayOrderInput.clear();
        displayOrderInput.sendKeys(displayOrder);

        // Scoped to .admin-card, not a bare "button[type='submit']" selector - the
        // shared _Layout's account dropdown (_LoginPartial.cshtml) renders its own
        // hidden button[type=submit] ("Logout") earlier in the DOM, inside the fixed
        // header, so an unscoped selector matches that invisible button first and
        // throws ElementNotInteractableException instead of clicking this page's own
        // Create/Update button in Pages/Admin/Categories/{Create,Edit}.cshtml.
        driver.findElement(By.cssSelector(".admin-card button[type='submit']")).click();
    }

    @Given("I am on the Admin Categories page")
    public void i_am_on_the_admin_categories_page() {
        navigateTo(BASE_URL + "/Admin/Categories");
    }

    @When("I visit the Admin Categories page directly")
    public void i_visit_the_admin_categories_page_directly() {
        navigateTo(BASE_URL + "/Admin/Categories");
    }

    // Registered as a single @When and reused verbatim as a Given in the
    // edit/delete scenarios - Cucumber matches steps by text regardless of
    // which keyword introduces them (same technique ReviewSteps uses for
    // "I submit a review with a rating of {int} and content {string}").
    @When("I create a new category with a unique name and a display order")
    public void i_create_a_new_category_with_a_unique_name_and_a_display_order() {
        categoryName = "Test Category " + UUID.randomUUID();
        categoryDisplayOrder = "50";
        navigateTo(BASE_URL + "/Admin/Categories/Create");
        fillAndSubmitCategoryForm(categoryName, categoryDisplayOrder);
    }

    @Given("I have created a category with a unique name and a display order")
    public void i_have_created_a_category_with_a_unique_name_and_a_display_order() {
        i_create_a_new_category_with_a_unique_name_and_a_display_order();
    }

    @Then("I should see that category in the list with the correct display order")
    public void i_should_see_that_category_in_the_list_with_the_correct_display_order() {
        WebElement nameCell = shortWait().until(ExpectedConditions.presenceOfElementLocated(categoryNameCell()));
        WebElement displayOrderCell = nameCell.findElement(By.xpath("following-sibling::td[1]"));
        assertEquals(categoryDisplayOrder, displayOrderCell.getText().trim());
    }

    @When("I try to create a category whose name is the same as its display order")
    public void i_try_to_create_a_category_whose_name_is_the_same_as_its_display_order() {
        navigateTo(BASE_URL + "/Admin/Categories/Create");
        fillAndSubmitCategoryForm("77", "77");
    }

    @Then("I should see a validation error that the display order cannot match the name")
    public void i_should_see_a_validation_error_that_the_display_order_cannot_match_the_name() {
        WebElement error = shortWait().until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'cannot exactly match')]")));
        assertTrue(error.isDisplayed());
    }

    @When("I edit that category to a new name and display order")
    public void i_edit_that_category_to_a_new_name_and_display_order() {
        WebElement nameCell = shortWait().until(ExpectedConditions.presenceOfElementLocated(categoryNameCell()));
        WebElement editLink = nameCell.findElement(
                By.xpath("following-sibling::td//a[contains(@href, 'Edit')]"));
        clickThenAwait(editLink, By.id("Category_Name"));

        categoryName = "Updated Category " + UUID.randomUUID();
        categoryDisplayOrder = "60";
        fillAndSubmitCategoryForm(categoryName, categoryDisplayOrder);
    }

    @Then("I should see the updated category in the list")
    public void i_should_see_the_updated_category_in_the_list() {
        i_should_see_that_category_in_the_list_with_the_correct_display_order();
    }

    @When("I delete that category")
    public void i_delete_that_category() {
        WebElement nameCell = shortWait().until(ExpectedConditions.presenceOfElementLocated(categoryNameCell()));
        WebElement deleteLink = nameCell.findElement(
                By.xpath("following-sibling::td//a[contains(@href, 'Delete')]"));

        // Same .admin-card scoping as fillAndSubmitCategoryForm() above, and for the
        // same reason - Pages/Admin/Categories/Delete.cshtml's own Delete button would
        // otherwise lose to the hidden Logout button rendered earlier in the header.
        WebElement deleteButton = clickThenAwait(deleteLink, By.cssSelector(".admin-card button[type='submit']"));
        deleteButton.click();
    }

    @Then("I should not see that category in the list")
    public void i_should_not_see_that_category_in_the_list() {
        shortWait().until(d -> d.findElements(categoryNameCell()).isEmpty());
    }

    @Then("I should be redirected to the access denied page")
    public void i_should_be_redirected_to_the_access_denied_page() {
        shortWait().until(ExpectedConditions.urlContains("/Identity/Account/AccessDenied"));
    }
}
