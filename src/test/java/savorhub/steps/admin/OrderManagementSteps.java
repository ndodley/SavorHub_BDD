package savorhub.steps.admin;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import savorhub.steps.BaseSteps;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for order_management.feature.
 *
 * <p>Locators and scope come straight from SavorHub's real markup and code at
 * Pages/Admin/Order/{OrderList,ManageOrder,OrderDetails}.cshtml(.cs),
 * Controllers/OrderController.cs, and Utilities/SD.cs:
 *   - Unlike every other Admin suite covered so far, these three pages do NOT
 *     share one role gate. OrderList and OrderDetails are
 *     [Authorize(Roles = "Manager,Front")]; ManageOrder is
 *     [Authorize(Roles = "Manager,Kitchen")]. This suite only has a Manager
 *     test account (see config.properties), which is authorized on all
 *     three, so every "can view" scenario here uses Manager - there's no
 *     account to test the Front/Kitchen-only access paths, and SavorHub has
 *     no self-service way to create one (no Admin/Employee page exists;
 *     confirmed by searching the whole SavorHub.Web/Pages tree).
 *   - "Order List" and "Manage Order(s)" both also appear as nav dropdown
 *     links in Pages/Shared/_Layout.cshtml ("Order List" verbatim, "Manage
 *     Order" singular) - a bare text search for those strings anywhere on
 *     the page would match the nav link as well as the page's own <h2>
 *     heading, so every "page loaded" check here is scoped to the h2 tag
 *     specifically, the same landmark every other Admin page uses for its
 *     title.
 *   - OrderList.cshtml renders an empty table (id="DT_load") populated by
 *     wwwroot/js/orderList.js via AJAX to /api/order?status=..., the same
 *     DataTables-driven pattern as MenuItems/Index.cshtml - rows aren't
 *     server-rendered, so this suite waits for the table body rather than
 *     assuming a synchronous page load has real rows in the DOM.
 *   - There's no way to drive a real order into Submitted/InProcess status
 *     through this suite: checkout only gets as far as a real Stripe
 *     Checkout redirect (see CartSteps' class javadoc), and completing that
 *     would mean this suite entering payment details itself, which it
 *     deliberately does not do. That rules out testing ManageOrder's "Start
 *     Cooking" / "Order Ready" / "Cancel" buttons and OrderDetails'
 *     "Complete" / "Cancel" buttons end-to-end without a fixture this app
 *     has no way to create - so this suite covers access control for all
 *     three pages, that OrderList/ManageOrder render their shell correctly,
 *     and OrderDetails against a real order, but not the order-lifecycle
 *     transitions themselves. Revisit if SavorHub ever gets a way to seed a
 *     Submitted order without going through Stripe.
 *   - For the one scenario that does need a real order (viewing OrderDetails
 *     for an order that exists), this suite opens the OrderList page's
 *     "completed" tab and clicks into whatever the first row happens to be,
 *     rather than depending on a specific id - Completed orders don't
 *     change (OrderDetails only renders action buttons for
 *     Ready/Submitted/InProcess orders), so this is safe to reuse across
 *     runs the same way Category/FoodType management tolerates accumulating
 *     rows with nothing cleaning them up.
 *   - While researching this suite, two real bugs were found and fixed
 *     directly in SavorHub (not test issues):
 *     1. OrderDetailsModel.OnGet(int id) called
 *        _unitOfWork.OrderHeader.GetFirstOrDefault(...) with no null check,
 *        so a nonexistent id (a stale link, or a hand-edited query string)
 *        crashed with a NullReferenceException the moment OrderDetails.cshtml
 *        dereferenced Model.OrderDetailVM.OrderHeader.Id - the same class of
 *        bug fixed earlier in MenuItems/Upsert.cshtml.cs's missing
 *        objFromDb check. OnGet now returns NotFound() instead.
 *     2. OrderDetails.cshtml rendered a "Refund" button
 *        (asp-page-handler="OrderRefund") whenever a Ready/Submitted/
 *        InProcess order was viewed, but OrderDetails.cshtml.cs's
 *        OnPostOrderRefund handler was entirely commented out (it depended
 *        on a real Stripe RefundService call that was never finished) -
 *        clicking it posted to a handler name Razor Pages couldn't find and
 *        failed every time. Removed the button rather than resurrect
 *        unfinished payment-refund logic as part of a test-hardening pass.
 *     The "nonexistent order id" scenario below is a regression test for
 *     fix #1.
 * If SavorHub's markup or code changes, re-check the real DOM/source rather
 * than trusting this comment.
 */
public class OrderManagementSteps extends BaseSteps {

    private static final String BASE_URL = CONFIG.getProperty("base.url");

    private WebDriverWait shortWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    private By pageHeading(String text) {
        return By.xpath("//h2[contains(., '" + text + "')]");
    }

    @When("I visit the Admin Order List page directly")
    public void i_visit_the_admin_order_list_page_directly() {
        navigateTo(BASE_URL + "/Admin/Order/OrderList");
    }

    @Then("I should see the Order List page")
    public void i_should_see_the_order_list_page() {
        shortWait().until(ExpectedConditions.presenceOfElementLocated(pageHeading("Order List")));
    }

    @When("I visit the Manage Orders page directly")
    public void i_visit_the_manage_orders_page_directly() {
        navigateTo(BASE_URL + "/Admin/Order/ManageOrder");
    }

    @Then("I should see the Manage Orders page")
    public void i_should_see_the_manage_orders_page() {
        shortWait().until(ExpectedConditions.presenceOfElementLocated(pageHeading("Manage Orders")));
    }

    @When("I visit the Admin Order Details page directly")
    public void i_visit_the_admin_order_details_page_directly() {
        // Any id works here - [Authorize] runs before OnGet ever looks up the order, so
        // an anonymous/customer request never reaches the point where the id matters.
        navigateTo(BASE_URL + "/Admin/Order/OrderDetails?id=1");
    }

    @When("I open the details page for a completed order")
    public void i_open_the_details_page_for_a_completed_order() {
        navigateTo(BASE_URL + "/Admin/Order/OrderList?status=completed");
        WebElement detailsLink = shortWait().until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("#DT_load tbody tr a[href*='OrderDetails']")));
        scrollToCenterAndClick(detailsLink);
    }

    @Then("I should see that order's details")
    public void i_should_see_that_orders_details() {
        shortWait().until(ExpectedConditions.presenceOfElementLocated(pageHeading("Order Details")));
    }

    @When("I visit the details page for an order id that does not exist")
    public void i_visit_the_details_page_for_an_order_id_that_does_not_exist() {
        navigateTo(BASE_URL + "/Admin/Order/OrderDetails?id=999999999");
    }

    @Then("I should see a not found response")
    public void i_should_see_a_not_found_response() {
        // OnGet now returns NotFound() for an id that doesn't match a real order (see the
        // class javadoc), which renders no page content at all - no shared _Layout, no
        // "Order Details" heading. Checking that heading is absent, together with a check
        // that no unhandled-exception text made it to the page, is what actually verifies
        // the fix: this scenario exists specifically to guard against regressing back to
        // the NullReferenceException it replaced.
        assertTrue(driver.findElements(pageHeading("Order Details")).isEmpty(),
                "Expected no Order Details heading to render for a nonexistent order id");
        String bodyText = driver.findElement(By.tagName("body")).getText();
        assertFalse(bodyText.contains("Exception"),
                "Expected no unhandled exception to be shown for a nonexistent order id");
    }
}
