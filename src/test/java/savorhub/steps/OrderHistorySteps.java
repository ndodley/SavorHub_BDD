package savorhub.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import savorhub.hooks.Hooks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for order_history.feature.
 *
 * <p>Reuses CartSteps' menu/cart/checkout step text to place a real order
 * (Cucumber matches steps by text across every glue class, regardless of
 * which .java file defines them) - this class only adds the My
 * Orders / Order Details steps on top.
 *
 * <p>Locators come straight from SavorHub's real markup and code at
 * Pages/Customer/Order/MyOrders.cshtml(.cs) and OrderDetails.cshtml(.cs):
 *   - MyOrdersModel is [Authorize]-protected and returns every order for
 *     the current user with no status filter and no explicit ordering -
 *     so "the most recent order" is found here by parsing every row's
 *     Order ID column and taking the maximum, not by assuming row order.
 *   - Summary.cshtml.cs's OnPost (see CartSteps) saves the order with
 *     Status = SD.StatusPending *before* creating the Stripe session, so
 *     a Pending row shows up in My Orders even though checkout never
 *     completes a real payment - that's exactly what these scenarios
 *     check for.
 *   - OrderDetails' route is "{id:int}" (a path segment, e.g.
 *     /Customer/Order/OrderDetails/5), not a query string, so the
 *     captured order ID doubles as a direct URL for the last two
 *     scenarios.
 *   - OrderDetailsModel is now [Authorize]-protected and its OnGet(id)
 *     scopes to the order's own OrderHeader.UserId (or a Manager/Front
 *     Desk/Kitchen staff role) before rendering anything, returning
 *     NotFound() otherwise - same posture as a genuinely missing id, so
 *     a stranger can't tell a given order id exists at all. The third
 *     scenario confirms a logged-out visit gets redirected to the login
 *     page (the normal [Authorize] challenge, same as every other
 *     [Authorize] page in this app); the fourth confirms a different
 *     logged-in customer (a second, freshly registered account) still
 *     can't see the first customer's order details.
 * If SavorHub's markup changes, re-check the real DOM in DevTools rather
 * than trusting this comment.
 */
public class OrderHistorySteps {

    private final WebDriver driver = Hooks.driver;

    private static final Properties CONFIG = loadConfig();
    private static final String BASE_URL = CONFIG.getProperty("base.url");

    private int lastOrderId;
    private String lastOrderStatus;
    private double lastOrderTotal;

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = OrderHistorySteps.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "Missing src/test/resources/config.properties. Copy "
                        + "config.properties.example to config.properties in that "
                        + "same folder and fill in your local test account details.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load config.properties", e);
        }
        return props;
    }

    private static double parsePrice(String text) {
        return Double.parseDouble(text.replaceAll("[^0-9.]", ""));
    }

    private WebDriverWait shortWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @SuppressWarnings("unchecked")
    @When("I go to my orders page")
    public void i_go_to_my_orders_page() {
        driver.get(BASE_URL + "/Customer/Order/MyOrders");
        shortWait().until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table tbody tr")));

        // Read every row in one browser-side pass instead of one Selenium
        // round-trip per cell per row. Checkout never completes a real
        // payment (see CartSteps), so every test run leaves behind a real
        // Pending order that nothing ever cleans up - this account now has
        // thousands of rows in My Orders, and looping through them one
        // WebDriver command at a time was slow enough to look like the
        // browser had hung, even though it always finished and passed.
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<List<String>> rows = (List<List<String>>) js.executeScript(
                "return Array.from(document.querySelectorAll('table tbody tr')).map(row => {"
                + "  const cells = row.querySelectorAll('td');"
                + "  return [cells[0].innerText.trim(), cells[2].innerText.trim(), cells[3].innerText.trim()];"
                + "});");
        assertNotNull(rows, "Expected the browser-side row extraction script to return a list, but got null.");
        assertFalse(rows.isEmpty(), "Expected at least one order in My Orders, but the table was empty.");

        int highestId = -1;
        List<String> latestRow = null;
        for (List<String> row : rows) {
            int rowId = Integer.parseInt(row.getFirst());
            if (rowId > highestId) {
                highestId = rowId;
                latestRow = row;
            }
        }

        assertNotNull(latestRow, "Expected to find a row with the highest order id, but none was found.");
        lastOrderId = highestId;
        lastOrderTotal = parsePrice(latestRow.get(1));
        lastOrderStatus = latestRow.get(2);
    }

    @Then("I should see my most recently placed order with a status of {string}")
    public void i_should_see_my_most_recently_placed_order_with_a_status_of(String expectedStatus) {
        assertEquals(expectedStatus.toLowerCase(), lastOrderStatus.toLowerCase());
    }

    @When("I view that order's details")
    public void i_view_that_orders_details() {
        // Ends-with, not contains - order IDs are the URL's last path
        // segment, and a "contains" match on e.g. "OrderDetails/12" would
        // also match a completely different order at id 112.
        WebElement detailsLink = driver.findElement(
                By.cssSelector("a[href$='OrderDetails/" + lastOrderId + "']"));
        // Same scroll-to-center + JS-click fix as CartSteps' submit buttons -
        // with thousands of rows in this account's order history, a plain
        // click can land on a row that isn't actually clickable yet.
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", detailsLink);
        js.executeScript("arguments[0].click();", detailsLink);
    }

    @Then("the order details should show the same order ID and total as My Orders")
    public void the_order_details_should_show_the_same_order_id_and_total_as_my_orders() {
        WebDriverWait wait = shortWait();
        WebElement idBadge = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("span.badge.bg-secondary")));
        assertTrue(idBadge.getText().contains(String.valueOf(lastOrderId)));

        WebElement totalElement = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("strong.text-primary.fs-4")));
        assertEquals(lastOrderTotal, parsePrice(totalElement.getText()), 0.01);
    }

    @Then("I should see at least one item listed in the order")
    public void i_should_see_at_least_one_item_listed_in_the_order() {
        List<WebElement> items = driver.findElements(By.cssSelector("ul.list-group li.list-group-item"));
        assertFalse(items.isEmpty());
    }

    @When("I visit that order's details page directly")
    public void i_visit_that_orders_details_page_directly() {
        driver.get(BASE_URL + "/Customer/Order/OrderDetails/" + lastOrderId);
    }

    @Then("I should be redirected to the login page")
    public void i_should_be_redirected_to_the_login_page() {
        // SavorHub's cookie auth is configured with LoginPath =
        // "/Identity/Account/Login" (Program.cs), so an [Authorize] page
        // challenges an anonymous request with a redirect there rather
        // than a bare 401 - the same thing every other [Authorize] page
        // in this app does.
        try {
            shortWait().until(ExpectedConditions.urlContains("/Identity/Account/Login"));
        } catch (org.openqa.selenium.TimeoutException e) {
            throw new AssertionError(
                    "Expected to be redirected to the login page after visiting the order details URL "
                    + "while logged out, but wasn't. Current URL: " + driver.getCurrentUrl(), e);
        }
    }

    @Then("I should not see that order's details")
    public void i_should_not_see_that_orders_details() {
        List<WebElement> idBadges = driver.findElements(By.cssSelector("span.badge.bg-secondary"));
        assertTrue(idBadges.isEmpty(),
                "Expected NOT to see order #" + lastOrderId + "'s details as a different logged-in "
                + "customer (the ownership check should have returned NotFound), but the order ID "
                + "badge was present. Current URL: " + driver.getCurrentUrl());
    }
}
