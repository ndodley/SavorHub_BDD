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

/**
 * Step definitions for homepage_navigation.feature.
 *
 * <p>Locators come straight from Pages/Index.cshtml, Pages/Privacy.cshtml,
 * and Pages/Shared/_Layout.cshtml:
 *   - The homepage has no [Authorize] attribute, so (like Menu Browsing)
 *     it's reachable while logged out - every scenario here runs without
 *     logging in first.
 *   - IndexModel.OnGet() always populates FeaturedItems from every menu
 *     item in the database, sorted by rating (not filtered down to only
 *     highly-rated ones), so the Featured Items section renders content
 *     as long as at least one menu item exists - the same assumption
 *     menu.feature and everything downstream of it already depends on.
 *   - "Explore Menu" and "Featured Items"/"Why Choose Us" are matched by
 *     their literal source text via XPath, not by CSS classes like
 *     btn-warning that are reused by several unrelated buttons on the
 *     same page. SavorHub's Bootswatch "Lux" theme uppercases headings
 *     and .btn-styled elements via CSS, but that only affects rendered
 *     text (.getText()) - raw XPath text() / normalize-space(.)
 *     predicates read the literal, un-transformed DOM text, so matching
 *     the real mixed-case source text here is safe.
 *   - The footer's Privacy link is a plain asp-page="/Privacy" anchor
 *     inside the page's <footer> element, always rendered (not behind
 *     any login or role check).
 * If SavorHub's markup changes, re-check the real DOM in DevTools rather
 * than trusting this comment.
 */
public class HomepageSteps extends BaseSteps {

    private static final String BASE_URL = CONFIG.getProperty("base.url");

    private WebDriverWait shortWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Given("I am on the SavorHub homepage")
    public void i_am_on_the_homepage() {
        navigateTo(BASE_URL + "/");
    }

    @Then("I should see the hero section with an Explore Menu button")
    public void i_should_see_the_hero_section_with_an_explore_menu_button() {
        shortWait().until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[contains(normalize-space(.), 'Explore Menu')]")));
    }

    @Then("I should see the Featured Items section")
    public void i_should_see_the_featured_items_section() {
        shortWait().until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h2[contains(normalize-space(.), 'Featured Items')]")));
    }

    @Then("I should see the Why Choose Us section")
    public void i_should_see_the_why_choose_us_section() {
        shortWait().until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h2[contains(normalize-space(.), 'Why Choose Us')]")));
    }

    @When("I click the Explore Menu button")
    public void i_click_the_explore_menu_button() {
        WebElement exploreMenuButton = shortWait().until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(normalize-space(.), 'Explore Menu')]")));
        exploreMenuButton.click();
    }

    @When("I click the Privacy link in the footer")
    public void i_click_the_privacy_link_in_the_footer() {
        // A plain .click() here gets ElementClickIntercepted - the footer sits
        // right at the bottom of a short page, and the fixed-top navbar's
        // JS-computed body padding-top (see site.js syncNavbarOffset) can leave
        // the anchor's calculated click point misaligned with what's actually
        // on top in the stacking context. Scrolling it to the center of the
        // viewport and clicking via JS sidesteps that, same as the fix already
        // used in LogoutSteps and ReviewSteps for this exact exception.
        WebElement privacyLink = shortWait().until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("footer a[href*='/Privacy']")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", privacyLink);
        js.executeScript("arguments[0].click();", privacyLink);
    }

    @Then("I should see the Privacy Policy page")
    public void i_should_see_the_privacy_policy_page() {
        shortWait().until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h1[contains(normalize-space(.), 'Privacy Policy')]")));
    }
}
