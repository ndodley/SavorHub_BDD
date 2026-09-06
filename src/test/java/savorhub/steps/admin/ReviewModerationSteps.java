package savorhub.steps.admin;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import savorhub.steps.BaseSteps;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for review_moderation.feature.
 *
 * <p>Locators and scope come straight from SavorHub's real markup and code at
 * Pages/Admin/Reviews/{Index,Edit,Delete}.cshtml(.cs):
 *   - Unlike every other Admin page in this suite, Reviews is
 *     [Authorize(Roles = SD.ManagerRole)] only (no Front/Kitchen split), and
 *     IndexModel/EditModel/DeleteModel all already null-check the review and
 *     return NotFound() for a bad id - this area was already defensively
 *     written before this suite touched it, unlike OrderDetails, so no
 *     app-code changes were needed here (confirmed by reading the .cs files,
 *     not assumed).
 *   - Index.cshtml is a plain server-rendered Bootstrap table (no
 *     DataTables/AJAX, unlike MenuItems/OrderList), so rows are already in
 *     the DOM on page load. Edit/Delete render as plain &lt;a&gt; tags with
 *     real hrefs (/Admin/Reviews/Edit/{id}, /Admin/Reviews/Delete/{id})
 *     rather than onclick handlers, but each still mixes an &lt;i&gt; icon
 *     with a trailing text node inside the &lt;a&gt; - the same shape that
 *     broke a plain contains(text(), 'Delete') XPath in
 *     MenuItemManagementSteps (contains() on a multi-node text() only looks
 *     at the first text-node child, which here is whitespace). Locating by
 *     the link's own href substring instead of its text sidesteps that
 *     entirely - confirmed live (document.querySelector) before writing
 *     this, same fix already applied for MenuItem's delete link.
 *   - A review has to exist before it can be moderated, and the only way to
 *     create one is as a Customer from a menu item's Details page - so the
 *     edit/delete/validation scenarios below log in as the shared Customer
 *     test account first and reuse menu.feature/reviews.feature's own steps
 *     ("I am on the SavorHub menu page" / "I click on the first menu item" /
 *     "I have no existing review on this item" / "I submit a review with a
 *     rating of {int} and content {string}") to create a fresh review
 *     tagged with unique content for that scenario, then log back in as
 *     Manager to moderate it. Every review this suite creates is looked up
 *     by its own unique content string rather than position, so it can
 *     never collide with whatever reviews.feature or another run of this
 *     suite has left behind in the shared dev database.
 *   - Edit.cshtml's Rating field has native HTML5 min="1" max="5"
 *     attributes with no equivalent server-side [Range] annotation backing
 *     it - just a plain "if (Rating &lt; 1 || Rating &gt; 5)" check in
 *     OnPost. An out-of-range value never reaches the server through the UI
 *     because Chrome's own constraint validation blocks the form submit
 *     first, so this suite doesn't attempt that path - it would be testing
 *     Chrome, not SavorHub. Content has no client-side constraint at all
 *     (no required/maxlength attribute), so a blank Content value submits
 *     for real, which is what this suite tests instead - confirmed live
 *     that the real ModelState.AddModelError("Content", ...) check fires
 *     and Edit.cshtml's existing &lt;span asp-validation-for="Content"&gt;
 *     renders it (this page already had that wired up correctly, unlike
 *     MenuItems/Upsert.cshtml's missing validation summary fixed earlier in
 *     this suite).
 *   - The real submit button on both Edit.cshtml and Delete.cshtml is
 *     scoped to ".admin-card button[type='submit']" rather than a bare
 *     "button[type=submit]" - the navbar's own Logout link is also a
 *     type="submit" button inside a form, and it renders earlier in the
 *     DOM, so an unscoped selector resolves to Logout instead of the real
 *     action button (confirmed live before writing this).
 * If SavorHub's markup or code changes, re-check the real DOM/source rather
 * than trusting this comment.
 */
public class ReviewModerationSteps extends BaseSteps {

    private static final String BASE_URL = CONFIG.getProperty("base.url");

    private WebDriverWait shortWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    private By pageHeading(String text) {
        return By.xpath("//h2[contains(., '" + text + "')]");
    }

    private WebElement findReviewRow(String content) {
        return shortWait().until(d -> {
            List<WebElement> rows = d.findElements(By.cssSelector("table tbody tr"));
            for (WebElement row : rows) {
                if (row.getText().contains(content)) {
                    return row;
                }
            }
            return null;
        });
    }

    @When("I visit the Admin Reviews page directly")
    public void i_visit_the_admin_reviews_page_directly() {
        navigateTo(BASE_URL + "/Admin/Reviews/Index");
    }

    @Then("I should see the Review List page")
    public void i_should_see_the_review_list_page() {
        shortWait().until(ExpectedConditions.presenceOfElementLocated(pageHeading("Review List")));
    }

    @When("I edit the review containing {string} to a rating of {int} and content {string}")
    public void i_edit_the_review_containing_to_a_rating_of_and_content(String matchContent, int rating, String newContent) {
        WebElement row = findReviewRow(matchContent);
        WebElement editLink = row.findElement(By.cssSelector("a[href*='/Admin/Reviews/Edit/']"));
        WebElement ratingInput = clickThenAwait(editLink, By.id("Rating"));
        ratingInput.clear();
        ratingInput.sendKeys(String.valueOf(rating));

        WebElement contentInput = driver.findElement(By.id("Content"));
        contentInput.clear();
        contentInput.sendKeys(newContent);

        WebElement submit = driver.findElement(By.cssSelector(".admin-card button[type='submit']"));
        scrollToCenterAndClick(submit);
    }

    @When("I try to clear the content of the review containing {string} and save")
    public void i_try_to_clear_the_content_of_the_review_containing_and_save(String matchContent) {
        WebElement row = findReviewRow(matchContent);
        WebElement editLink = row.findElement(By.cssSelector("a[href*='/Admin/Reviews/Edit/']"));
        WebElement contentInput = clickThenAwait(editLink, By.id("Content"));
        contentInput.clear();

        WebElement submit = driver.findElement(By.cssSelector(".admin-card button[type='submit']"));
        scrollToCenterAndClick(submit);
    }

    @Then("I should see a review in the list with a rating of {int} and content {string}")
    public void i_should_see_a_review_in_the_list_with_a_rating_of_and_content(int rating, String content) {
        WebElement row = findReviewRow(content);
        assertTrue(row.getText().contains("(" + rating + "/5)"),
                "Expected the review's row to show a rating of " + rating + "/5");
    }

    @When("I delete the review containing {string}")
    public void i_delete_the_review_containing(String matchContent) {
        WebElement row = findReviewRow(matchContent);
        WebElement deleteLink = row.findElement(By.cssSelector("a[href*='/Admin/Reviews/Delete/']"));
        WebElement confirmButton = clickThenAwait(deleteLink, By.cssSelector(".admin-card button[type='submit']"));
        scrollToCenterAndClick(confirmButton);
    }

    @Then("I should not see a review in the list with content {string}")
    public void i_should_not_see_a_review_in_the_list_with_content(String content) {
        shortWait().until(ExpectedConditions.presenceOfElementLocated(pageHeading("Review List")));
        assertTrue(driver.findElements(By.xpath("//td[contains(., '" + content + "')]")).isEmpty(),
                "Expected no review row to still contain: " + content);
    }

    @Then("I should see a validation error that content is required")
    public void i_should_see_a_validation_error_that_content_is_required() {
        WebElement error = shortWait().until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("span[data-valmsg-for='Content']")));
        assertTrue(error.getText().contains("required"),
                "Expected a validation message saying content is required, got: " + error.getText());
    }
}
