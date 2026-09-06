package savorhub.steps.customer;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import savorhub.steps.BaseSteps;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for cart_and_checkout.feature.
 *
 * <p>Locators come straight from SavorHub's real markup and code at
 * Pages/Customer/Cart/Index.cshtml(.cs), Summary.cshtml(.cs), and
 * Pages/Customer/Menu/Details.cshtml(.cs):
 *   - There is no cost-threshold logic anywhere in this app (no
 *     delivery fee, tax, discount codes, or minimum order) - confirmed
 *     by reading the real code, since SavorHub is pickup-only. The real
 *     conditional logic exercised here instead: Cart/Index.cshtml.cs's
 *     OnPostMinus removes the item entirely when its count is already 1
 *     rather than decrementing to 0; the cart renders a completely
 *     different "Please add items to shopping cart." view when empty.
 *   - "Add to Cart" lives on the Menu Details page as the default
 *     (unnamed) OnPost handler - ShoppingCart.Count defaults to 1
 *     server-side, so the quantity input never needs to be touched for
 *     a single-item add. It requires being logged in (Challenge() if
 *     not). The button submits through the page's AJAX interceptor
 *     (site.js), which swaps the item card's markup in place rather
 *     than navigating anywhere, so this suite still sees the old
 *     "Add to Cart" button go stale once the swap completes even
 *     though the browser never leaves the Details page.
 *   - The cart's Plus/Minus/Remove buttons use asp-page-handler +
 *     asp-route-cartId directly on <button type="submit">, which
 *     ASP.NET Core's Form Action Tag Helper renders as a "formaction"
 *     attribute containing "handler=plus" / "handler=minus" /
 *     "handler=remove" - since every scenario starts from an empty
 *     cart (see "my cart is empty") and adds exactly one item, there's
 *     never more than one row to disambiguate between.
 *   - Cart line quantity/price render as plain text ("$12.99 x 2"
 *     inside an <h6><strong>), not a dedicated element per value, so
 *     the count is parsed out of that text rather than read from a
 *     separate field.
 *   - The Summary page's Pickup Date/Time inputs (type="date"/"time")
 *     start empty and aren't pre-filled by Summary.cshtml.cs's OnGet,
 *     so this suite fills them via JavaScript (using the ISO value
 *     format those input types expect) rather than leaving them blank,
 *     to submit a realistic, valid order.
 *   - Checkout (Summary's OnPost) creates a real Stripe Checkout
 *     Session and redirects to it - this requires a valid Stripe
 *     secret key configured via dotnet user-secrets in the local
 *     SavorHub instance (appsettings.json's committed SecretKey is
 *     intentionally blank). If that key isn't configured locally, the
 *     final "redirected to Stripe" scenario will fail with a timeout
 *     rather than a Selenium locator problem - that's a SavorHub local
 *     setup issue, not a bug in these tests.
 * If SavorHub's markup changes, re-check the real DOM in DevTools rather
 * than trusting this comment.
 */
public class CartSteps extends BaseSteps {

    private static final String BASE_URL = CONFIG.getProperty("base.url");

    private String selectedItemName;
    private double selectedItemPrice;

    private static double parsePrice(String text) {
        String numeric = text.replaceAll("[^0-9.]", "");
        return Double.parseDouble(numeric);
    }

    // The item name is captured from the menu page and re-matched on the
    // cart/summary pages further down this class. Matching case-insensitively
    // (same translate() trick used in LoginSteps/RegisterSteps) is cheap
    // insurance against any casing difference between the two pages, rather
    // than assuming the exact same characters render identically everywhere.
    private static String caseInsensitive(String xpathExpression) {
        return "translate(" + xpathExpression + ", "
                + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')";
    }

    private WebDriverWait shortWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Given("my cart is empty")
    public void my_cart_is_empty() {
        navigateTo(BASE_URL + "/Customer/Cart");
        for (int i = 0; i < 25; i++) {
            List<WebElement> removeButtons = driver.findElements(By.cssSelector("button[formaction*='handler=remove']"));
            if (removeButtons.isEmpty()) {
                return;
            }
            WebElement removeButton = removeButtons.getFirst();
            removeButton.click();
            shortWait().until(ExpectedConditions.stalenessOf(removeButton));
        }
        throw new AssertionError("Cart still wasn't empty after 25 removal attempts.");
    }

    @When("I add the first menu item to my cart")
    public void i_add_the_first_menu_item_to_my_cart() {
        WebDriverWait wait = shortWait();
        List<WebElement> items = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("hover-card")));
        WebElement firstItem = items.getFirst();
        selectedItemName = firstItem.findElement(By.className("card-title")).getText();
        selectedItemPrice = parsePrice(firstItem.findElement(By.className("app-price")).getText());

        // firstItem is a real <a> (hover-card), so clicking it is a full-page
        // navigation to the item's Details page - exposed to the same
        // ChromeDriver/DevTools navigation flake that clickThenAwait already
        // guards against on the Admin side, just with a bigger blast radius
        // here since every scenario in this suite that adds to cart goes
        // through this exact line.
        WebElement addToCartButton = clickThenAwait(firstItem,
                By.xpath("//button[contains(normalize-space(.), 'Add to Cart')]"));
        // Plain click() here throws ElementClickInterceptedException - something
        // on the Details page (fixed navbar / a toastr, same class of thing
        // scrollToCenterAndClick exists for) transiently overlaps this button's
        // click point. Scrolling to center + a JS click sidesteps that check.
        scrollToCenterAndClick(addToCartButton);
        wait.until(ExpectedConditions.stalenessOf(addToCartButton));
    }

    @When("I go to the Cart page")
    public void i_go_to_the_cart_page() {
        navigateTo(BASE_URL + "/Customer/Cart");
    }

    @Then("I should see that item in my cart")
    public void i_should_see_that_item_in_my_cart() {
        shortWait().until(ExpectedConditions.presenceOfElementLocated(cartItemNameLocator()));
    }

    private By cartItemNameLocator() {
        String nameLower = selectedItemName.toLowerCase();
        return By.xpath("//h4/strong[contains(" + caseInsensitive("normalize-space(text())") + ", '" + nameLower + "')]");
    }

    @Then("the cart total should equal that item's price")
    public void the_cart_total_should_equal_that_items_price() {
        assertCartTotalEquals(selectedItemPrice);
    }

    @Then("the cart total should equal twice that item's price")
    public void the_cart_total_should_equal_twice_that_items_price() {
        assertCartTotalEquals(selectedItemPrice * 2);
    }

    private void assertCartTotalEquals(double expected) {
        WebElement totalElement = shortWait().until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("strong.app-price")));
        double actual = parsePrice(totalElement.getText());
        assertEquals(expected, actual, 0.01);
    }

    @When("I increase that item's quantity")
    public void i_increase_that_items_quantity() {
        clickCartHandlerButton("plus");
    }

    @When("I decrease that item's quantity")
    public void i_decrease_that_items_quantity() {
        clickCartHandlerButton("minus");
    }

    @When("I remove that item from the cart")
    public void i_remove_that_item_from_the_cart() {
        clickCartHandlerButton("remove");
    }

    private void clickCartHandlerButton(String handlerName) {
        WebElement button = shortWait().until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[formaction*='handler=" + handlerName + "']")));
        button.click();
        shortWait().until(ExpectedConditions.stalenessOf(button));
    }

    @Then("the cart should show a quantity of {int} for that item")
    public void the_cart_should_show_a_quantity_of_for_that_item(int expectedCount) {
        String nameLower = selectedItemName.toLowerCase();
        WebElement line = shortWait().until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h4/strong[contains(" + caseInsensitive("normalize-space(text())") + ", '" + nameLower + "')]"
                        + "/ancestor::div[contains(@class, 'row')][1]//h6")));
        String text = line.getText();
        // Split case-insensitively - the "x" separator sits inside an <h6>,
        // and SavorHub's theme uppercases every heading via CSS, so the
        // rendered text is "$4.99 X 2", not "$4.99 x 2".
        String[] parts = text.split("(?i)x");
        int actualCount = Integer.parseInt(parts[parts.length - 1].trim());
        assertEquals(expectedCount, actualCount);
    }

    @Then("I should see the empty cart message")
    public void i_should_see_the_empty_cart_message() {
        shortWait().until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Please add items to shopping cart')]")));
    }

    @When("I proceed to checkout")
    public void i_proceed_to_checkout() {
        // Matched by href, not link text - SavorHub's theme uppercases every
        // .btn-styled element via CSS, so the rendered text Selenium would
        // see ("SUMMARY") doesn't match the literal source text ("Summary").
        WebElement summaryLink = shortWait().until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("a[href*='/Cart/Summary']")));
        summaryLink.click();
    }

    @Then("I should see the order summary with the correct total")
    public void i_should_see_the_order_summary_with_the_correct_total() {
        WebElement totalElement = shortWait().until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("strong.text-info")));
        double actual = parsePrice(totalElement.getText());
        assertEquals(selectedItemPrice, actual, 0.01);
    }

    @When("I place the order")
    public void i_place_the_order() {
        WebDriverWait wait = shortWait();
        WebElement dateInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("OrderHeader_PickUpDate")));
        WebElement timeInput = driver.findElement(By.id("OrderHeader_PickUpTime"));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].value = arguments[1];", dateInput, LocalDate.now().toString());
        js.executeScript("arguments[0].value = arguments[1];", timeInput, "12:00");

        WebElement placeOrderButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("btnPlaceOrder")));
        // Same fix as RegisterSteps' submit button - scrolling to center clears
        // the fixed-top navbar, and clicking via JS bypasses Selenium's
        // visual-obstruction check for whatever else transiently overlaps it.
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", placeOrderButton);
        js.executeScript("arguments[0].click();", placeOrderButton);
    }

    @Then("I should be redirected to Stripe's checkout page")
    public void i_should_be_redirected_to_stripes_checkout_page() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        wait.until(ExpectedConditions.urlContains("stripe.com"));
        assertTrue(driver.getCurrentUrl().contains("stripe.com"));
    }
}
