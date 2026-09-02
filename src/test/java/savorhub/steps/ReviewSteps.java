package savorhub.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import savorhub.hooks.Hooks;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for reviews.feature.
 *
 * <p>Every scenario reaches a menu item's Details page through the existing
 * menu.feature steps ("I am on the SavorHub menu page" / "I click on the
 * first menu item" from MenuSteps), so this class has no navigation logic
 * of its own - it only acts on whatever Details page is already open.
 *
 * <p>Locators come straight from SavorHub's real markup and code at
 * Pages/Customer/Menu/Details.cshtml(.cs) and Controllers/ReviewController.cs:
 *   - The "Write a Review" form's star rating is five radio inputs
 *     (id="star1".."star5", class "btn-check") paired with visible
 *     <label for="starN"> buttons - Bootstrap's toggle-button pattern hides
 *     the radio itself, so this suite clicks the label, exactly like a real
 *     user would, rather than fighting the hidden input.
 *   - The review text field is asp-for="NewReview.Content", so its id is
 *     "NewReview_Content" (same asp-for -> id convention used everywhere
 *     else in this app).
 *   - A reviewer's own review renders inside a collapsed
 *     <details><summary>Edit my review</summary>...</details> block (only
 *     for the review whose UserId matches the logged-in user), containing
 *     an UpdateReview form (a visible number input named
 *     "NewReview.Rating" and a textarea named "NewReview.Content" - not
 *     asp-for here, so no generated id) and a DeleteReview form. Since
 *     Details has no route template ("@page" with no "{id}"), asp-route-id
 *     / asp-route-reviewId render as query-string parameters on the
 *     form's own "action" attribute (e.g.
 *     "?handler=DeleteReview&id=5&reviewId=42"), which is how this suite
 *     reads a review's real database id straight off the page - there's no
 *     visible "review #42" label anywhere in the UI.
 *   - Menu Details' OnPostAddReview allows multiple reviews from the same
 *     user for the same item (no "already reviewed" rejection). Even so,
 *     this feature always starts from a clean slate via "I have no
 *     existing review on this item", which deletes any leftover review
 *     from a previous run through the same UI flow a user would use (its
 *     confirm() dialog is handled the same way FavoriteSteps handles the
 *     Remove-favorite confirmation) - that keeps exactly one review on
 *     the item at a time, which is what myReviewCard()/captureMyReviewId()
 *     below assume when locating "my" review.
 *   - Add/Edit/Delete Review all submit through the page's AJAX
 *     interceptor (site.js), which swaps the reviews card's markup in
 *     place instead of a full page reload - the old form/button still
 *     goes stale once the swap completes, so the existing
 *     stalenessOf(...)/presenceOfElementLocated(...) waits below keep
 *     working even though the browser never leaves the Details page.
 *   - ReviewController's DELETE /api/review/{id} action only requires the
 *     caller to be logged in ([Authorize], no role restriction) and
 *     performs no ownership check at all before deleting - unlike GetAll/
 *     Get/Put on that same controller, which all correctly scope to
 *     "r.UserId == userId". Nothing in the customer-facing UI links to
 *     this controller (the admin-side review management page has its own,
 *     separate, role-gated Razor Page handlers instead), but the action
 *     itself is still live and reachable by any authenticated browser
 *     session issuing the HTTP request directly - exactly what the third
 *     scenario in this feature does via a fetch() call executed in the
 *     browser, using the second (attacker) account's own login cookie.
 * If SavorHub's markup or code changes, re-check the real DOM/source
 * rather than trusting this comment.
 */
public class ReviewSteps {

    private final WebDriver driver = Hooks.driver;

    private int myReviewId = -1;
    private String lastApiResponse;

    private WebDriverWait shortWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Given("I have no existing review on this item")
    public void i_have_no_existing_review_on_this_item() {
        List<WebElement> deleteForms = driver.findElements(By.cssSelector("form[action*='handler=DeleteReview']"));
        if (deleteForms.isEmpty()) {
            return;
        }
        WebElement deleteForm = deleteForms.getFirst();
        WebElement deleteButton = deleteForm.findElement(By.tagName("button"));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", deleteButton);
        js.executeScript("arguments[0].click();", deleteButton);

        WebDriverWait wait = shortWait();
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
        wait.until(ExpectedConditions.stalenessOf(deleteForm));
    }

    // Cucumber matches step text regardless of the Given/When/Then keyword used
    // in the feature file, so a single @When registration here is all that's
    // needed - this step is used as both a "Given" (scenarios 2 and 3, setting
    // up a review to edit/delete) and a "When" (scenario 1). Stacking both
    // @Given and @When on one method registers two identical glue entries for
    // the same text, which Cucumber rejects as a DuplicateStepDefinitionException.
    @When("I submit a review with a rating of {int} and content {string}")
    public void i_submit_a_review_with_a_rating_of_and_content(int rating, String content) {
        WebDriverWait wait = shortWait();
        WebElement ratingLabel = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("label[for='star" + rating + "']")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", ratingLabel);
        js.executeScript("arguments[0].click();", ratingLabel);

        WebElement contentField = driver.findElement(By.id("NewReview_Content"));
        contentField.clear();
        contentField.sendKeys(content);

        WebElement submitButton = driver.findElement(
                By.xpath("//button[contains(normalize-space(.), 'Submit Review')]"));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", submitButton);
        js.executeScript("arguments[0].click();", submitButton);

        captureMyReviewId();
    }

    private void captureMyReviewId() {
        WebElement deleteForm = shortWait().until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("form[action*='handler=DeleteReview']")));
        String action = deleteForm.getAttribute("action");
        Matcher matcher = Pattern.compile("reviewId=(\\d+)").matcher(action);
        if (!matcher.find()) {
            throw new AssertionError("Could not find a reviewId in the delete form's action: " + action);
        }
        myReviewId = Integer.parseInt(matcher.group(1));
    }

    private WebElement myReviewCard() {
        WebElement details = shortWait().until(ExpectedConditions.presenceOfElementLocated(By.tagName("details")));
        return details.findElement(By.xpath("ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' card ')][1]"));
    }

    @Then("I should see my review listed with a rating of {int} and content {string}")
    public void i_should_see_my_review_listed_with_a_rating_of_and_content(int rating, String content) {
        WebElement card = myReviewCard();
        assertTrue(card.getText().contains(content),
                "Expected the review card to contain: " + content + " but it was: " + card.getText());
        WebElement ratingBadge = card.findElement(By.cssSelector("span.badge.bg-secondary"));
        assertEquals(rating + "/5", ratingBadge.getText());
    }

    @When("I edit my review to a rating of {int} and content {string}")
    public void i_edit_my_review_to_a_rating_of_and_content(int rating, String content) {
        WebDriverWait wait = shortWait();
        WebElement summary = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//summary[contains(normalize-space(.), 'Edit my review')]")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", summary);
        js.executeScript("arguments[0].click();", summary);

        WebElement ratingInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("form[action*='handler=UpdateReview'] input[name='NewReview.Rating']")));
        ratingInput.clear();
        ratingInput.sendKeys(String.valueOf(rating));

        WebElement contentInput = driver.findElement(
                By.cssSelector("form[action*='handler=UpdateReview'] textarea[name='NewReview.Content']"));
        contentInput.clear();
        contentInput.sendKeys(content);

        WebElement updateButton = driver.findElement(
                By.xpath("//form[contains(@action, 'handler=UpdateReview')]//button[contains(normalize-space(.), 'Update')]"));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", updateButton);
        js.executeScript("arguments[0].click();", updateButton);

        captureMyReviewId();
    }

    @When("the newly registered user deletes my review by calling the review API directly")
    public void the_newly_registered_user_deletes_my_review_via_the_api() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        lastApiResponse = (String) js.executeAsyncScript(
                "var callback = arguments[arguments.length - 1];"
                + "fetch('/api/review/' + arguments[0], { method: 'DELETE', credentials: 'same-origin' })"
                + "  .then(function(r) { return r.json(); })"
                + "  .then(function(data) { callback(JSON.stringify(data)); })"
                + "  .catch(function(err) { callback('ERROR:' + err); });",
                myReviewId);
    }

    @Then("the review API reports the delete as successful")
    public void the_review_api_reports_the_delete_as_successful() {
        assertTrue(lastApiResponse != null && lastApiResponse.contains("\"success\":true"),
                "Expected DELETE /api/review/" + myReviewId + " to report success (documenting the "
                + "missing ownership check on that endpoint), but got: " + lastApiResponse);
    }

    @Then("I should no longer see my review on that item")
    public void i_should_no_longer_see_my_review_on_that_item() {
        List<WebElement> deleteForms = driver.findElements(By.cssSelector("form[action*='handler=DeleteReview']"));
        assertTrue(deleteForms.isEmpty(),
                "Expected my review to be gone (deleted by the other account via the API), but it's still showing.");
    }
}
