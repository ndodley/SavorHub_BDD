package savorhub.steps.admin;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import savorhub.steps.BaseSteps;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for menu_item_management.feature.
 *
 * <p>Locators come straight from SavorHub's real markup and code at
 * Pages/Admin/MenuItems/{Index,Upsert}.cshtml(.cs), Controllers/MenuItemController.cs,
 * wwwroot/js/menuItem.js, and Models/MenuItem.cs:
 *   - Both pages, and the api/MenuItem controller, are
 *     [Authorize(Roles = SD.ManagerRole)]-protected - unauthenticated redirects to
 *     the login page, and an authenticated non-Manager redirects to
 *     /Identity/Account/AccessDenied.
 *   - Unlike Categories/FoodTypes, Index.cshtml does NOT server-render its rows -
 *     it's an empty table (id="DT_load") populated client-side by a DataTables
 *     instance (menuItem.js's loadList()) that AJAX-GETs /api/MenuItem. DataTables
 *     paginates client-side (10 rows/page by default) and only renders the current
 *     page's <tr> into the DOM, so a row can be genuinely absent from the DOM
 *     without being "not found yet" - unlike Category/FoodType's plain
 *     server-rendered tables, presence-checking a cell by text alone is NOT
 *     reliable here once the table exceeds one page. Every step below instead
 *     types the item's unique name into DataTables' own search box
 *     (input[type='search'], present regardless of DataTables version-specific
 *     wrapper class names) to filter down to that one row first.
 *   - MenuItem.Name/Price use the standard asp-for -> id convention on Upsert.cshtml,
 *     so their ids are "MenuItem_Name" / "MenuItem_Price". Description has no
 *     [Required] attribute (and renders as a TinyMCE rich-text editor, not a plain
 *     textarea), so this suite never touches it - leaving it untouched submits an
 *     empty (valid) value.
 *   - Category/FoodType are <select asp-for="MenuItem.CategoryId"/"MenuItem.FoodTypeId">
 *     populated from whatever Categories/FoodTypes exist in the local database, with
 *     a disabled "-Select Category-"/"-Select Food Type-" placeholder as option 0 -
 *     this suite always selects option 1 (the first real choice), rather than a
 *     specific name, since it doesn't control what's seeded locally.
 *   - The Image field is a plain <input type="file" id="uploadBox">, not covered by
 *     a server-side [Required] on MenuItem.Image - the create handler
 *     (Upsert.cshtml.cs OnPost) unconditionally reads files[0] with no null/empty
 *     check, so submitting without a file would throw server-side. The ONLY thing
 *     preventing that today is client-side JS (Upsert.cshtml's ValidateInput(),
 *     wired to the Create button's onclick), which blocks the submit and shows a
 *     SweetAlert2 "Oops... Please upload an Image!" popup - the "without an image"
 *     scenario exercises exactly that client-side block, not a server response.
 *   - The Create/Update submit buttons are located via
 *     ".admin-card button[type='submit']", not a bare "button[type='submit']" -
 *     the shared _Layout's account dropdown (Pages/Shared/_LoginPartial.cshtml)
 *     renders its own hidden button[type=submit] ("Logout") earlier in the DOM,
 *     inside the fixed header, so an unscoped selector matches that invisible
 *     button first and throws ElementNotInteractableException (same issue already
 *     found in CategoryManagementSteps/FoodTypeManagementSteps).
 *   - Edit is a link to /Admin/MenuItems/upsert?id={id}, same Upsert page as Create.
 *     Delete is NOT a link to a confirmation page like Category/FoodType - it's
 *     an <a onClick="Delete(...)"> that opens a SweetAlert2 "Are you sure?" modal
 *     and, on confirm, fires an AJAX DELETE to /api/MenuItem/{id} directly (no page
 *     navigation). The confirm button is SweetAlert2's default ".swal2-confirm".
 *   - Both row links, and the Create/Update submit button, use a scroll-then-JS-click,
 *     not a plain WebElement.click() - same reasoning as
 *     CategoryManagementSteps/FoodTypeManagementSteps (a toastr notification appears
 *     after Create/Edit/Delete, and this table can grow long over runs, so the
 *     button/link is not always guaranteed to be both on-screen and unobstructed for
 *     a native click).
 * If SavorHub's markup or code changes, re-check the real DOM/source rather than
 * trusting this comment.
 */
public class MenuItemManagementSteps extends BaseSteps {

    private static final String BASE_URL = CONFIG.getProperty("base.url");

    private String menuItemName;

    private WebDriverWait shortWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // sendKeys() on <input type="file"> needs a real filesystem path, not a
    // classpath stream - resolves src/test/resources/testdata/sample-menu-item.png
    // to its absolute path on disk via the test classpath, the same way it'll be
    // laid out under target/test-classes once Maven copies test resources.
    private String sampleImagePath() {
        try {
            java.net.URL resource = MenuItemManagementSteps.class.getClassLoader()
                    .getResource("testdata/sample-menu-item.png");
            Objects.requireNonNull(resource, "Missing test resource testdata/sample-menu-item.png");
            Path path = Paths.get(resource.toURI());
            return path.toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not resolve sample-menu-item.png to a file path", e);
        }
    }

    private void searchForMenuItem(String name) {
        WebElement searchBox = shortWait().until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='search']")));
        searchBox.clear();
        searchBox.sendKeys(name);
    }

    private By menuItemNameCell() {
        return By.xpath("//td[normalize-space(text())='" + menuItemName + "']");
    }

    private void fillCommonFields(String name) {
        WebElement nameInput = shortWait().until(ExpectedConditions.presenceOfElementLocated(By.id("MenuItem_Name")));
        nameInput.clear();
        nameInput.sendKeys(name);

        WebElement priceInput = driver.findElement(By.id("MenuItem_Price"));
        priceInput.clear();
        priceInput.sendKeys("25");

        new Select(driver.findElement(By.id("MenuItem_CategoryId"))).selectByIndex(1);
        new Select(driver.findElement(By.id("MenuItem_FoodTypeId"))).selectByIndex(1);
    }

    private void submit() {
        // Scoped to .admin-card, not a bare "button[type='submit']" selector - see
        // the class javadoc for why an unscoped selector matches the shared
        // layout's hidden Logout button instead of this page's own button. Also uses
        // scroll-then-JS-click, not a plain WebElement.click() - on the Edit flow this
        // button can be scrolled out of view or transiently overlapped by a toastr
        // notification from the page load, which throws
        // ElementClickInterceptedException on a native click.
        WebElement submitButton = driver.findElement(By.cssSelector(".admin-card button[type='submit']"));
        scrollToCenterAndClick(submitButton);
    }

    @Given("I am on the Admin Menu Items page")
    public void i_am_on_the_admin_menu_items_page() {
        navigateTo(BASE_URL + "/Admin/MenuItems");
    }

    @When("I visit the Admin Menu Items page directly")
    public void i_visit_the_admin_menu_items_page_directly() {
        navigateTo(BASE_URL + "/Admin/MenuItems");
    }

    // Registered as a single @When and reused verbatim as a Given in the
    // edit/delete scenarios - Cucumber matches steps by text regardless of
    // which keyword introduces them (same technique used throughout this suite).
    @When("I create a new menu item with a unique name, a price, and an image")
    public void i_create_a_new_menu_item_with_a_unique_name_a_price_and_an_image() {
        menuItemName = "Test Menu Item " + UUID.randomUUID();
        navigateTo(BASE_URL + "/Admin/MenuItems/Upsert");
        fillCommonFields(menuItemName);
        driver.findElement(By.id("uploadBox")).sendKeys(sampleImagePath());
        submit();
    }

    @Given("I have created a menu item with a unique name, a price, and an image")
    public void i_have_created_a_menu_item_with_a_unique_name_a_price_and_an_image() {
        i_create_a_new_menu_item_with_a_unique_name_a_price_and_an_image();
    }

    @Then("I should see that menu item in the list")
    public void i_should_see_that_menu_item_in_the_list() {
        i_am_on_the_admin_menu_items_page();
        searchForMenuItem(menuItemName);
        shortWait().until(ExpectedConditions.presenceOfElementLocated(menuItemNameCell()));
    }

    @When("I try to create a menu item without choosing an image")
    public void i_try_to_create_a_menu_item_without_choosing_an_image() {
        navigateTo(BASE_URL + "/Admin/MenuItems/Upsert");
        fillCommonFields("Test Menu Item " + UUID.randomUUID());
        submit();
    }

    @Then("I should see a validation error that an image is required")
    public void i_should_see_a_validation_error_that_an_image_is_required() {
        // This is the client-side check: the Create button's onclick="return
        // ValidateInput()" (Upsert.cshtml) runs before the browser ever submits the
        // form, so submit()'s click never reaches the server here - it shows a
        // SweetAlert2 popup instead of posting. Locating the popup by
        // "//*[contains(text(), 'Please upload an Image')]" looked reasonable but was
        // wrong: that XPath also matches the <script> tag holding ValidateInput()'s own
        // source (which contains that literal string), and since the script tag comes
        // first in the DOM, findElement always resolved to it - an invisible element
        // that stays invisible forever, so the wait either failed isDisplayed() or, once
        // switched to a visibility wait, just timed out. SweetAlert2's own message
        // container class can't collide with page script content, so locate that
        // directly instead of text-matching broadly.
        WebElement error = shortWait().until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".swal2-html-container")));
        assertTrue(error.getText().contains("Please upload an Image"));
    }

    @When("I edit that menu item to a new name")
    public void i_edit_that_menu_item_to_a_new_name() {
        i_am_on_the_admin_menu_items_page();
        searchForMenuItem(menuItemName);
        WebElement nameCell = shortWait().until(ExpectedConditions.presenceOfElementLocated(menuItemNameCell()));
        WebElement editLink = nameCell.findElement(
                By.xpath("following-sibling::td//a[contains(@href, 'upsert')]"));
        WebElement nameInput = clickThenAwait(editLink, By.id("MenuItem_Name"));

        menuItemName = "Updated Menu Item " + UUID.randomUUID();
        nameInput.clear();
        nameInput.sendKeys(menuItemName);
        submit();
    }

    @Then("I should see the updated menu item in the list")
    public void i_should_see_the_updated_menu_item_in_the_list() {
        i_should_see_that_menu_item_in_the_list();
    }

    @When("I delete that menu item")
    public void i_delete_that_menu_item() {
        i_am_on_the_admin_menu_items_page();
        searchForMenuItem(menuItemName);
        WebElement nameCell = shortWait().until(ExpectedConditions.presenceOfElementLocated(menuItemNameCell()));
        // Matched on the onclick attribute, not link text - the Delete <a> renders as
        // <a onclick="Delete(...)"><i class="bi bi-trash-fill"></i>Delete</a>, and the
        // whitespace text node between the opening tag and the icon sorts before the
        // "Delete" text node, so contains(text(), 'Delete') was matching against that
        // leading whitespace and never finding it (confirmed live - 0 matches,
        // regardless of any wait). Same reasoning as the Edit link's @href match above.
        WebElement deleteLink = shortWait().until(d -> nameCell.findElement(
                By.xpath("following-sibling::td//a[contains(@onclick, 'Delete')]")));
        scrollToCenterAndClick(deleteLink);

        WebElement confirmButton = shortWait().until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(".swal2-confirm")));
        scrollToCenterAndClick(confirmButton);
    }

    @Then("I should not see that menu item in the list")
    public void i_should_not_see_that_menu_item_in_the_list() {
        shortWait().until(d -> d.findElements(menuItemNameCell()).isEmpty());
    }
}
