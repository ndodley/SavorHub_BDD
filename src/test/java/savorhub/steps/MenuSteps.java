package savorhub.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for menu.feature.
 *
 * <p>Locators below come straight from SavorHub's real markup at
 * SavorHub.Web/Pages/Customer/Menu/Index.cshtml and Details.cshtml:
 *   - The menu page's URL is "/Customer/Menu" (ASP.NET Core Razor Pages
 *     drops the trailing "/Index" from an Index.cshtml page's default route).
 *   - Each menu item card carries the project-specific CSS class
 *     "hover-card" (not a Bootstrap utility class), wrapped in a clickable
 *     <a> tag, with the item's name in a nested ".card-title" element.
 *   - The details page's header carries the project-specific class
 *     "details-item-header".
 *   - Neither page has an [Authorize] attribute, so both are reachable
 *     while logged out.
 * If SavorHub's markup changes, re-check the real DOM in DevTools rather
 * than trusting this comment.
 */
public class MenuSteps {

    private final WebDriver driver = Hooks.driver;

    private static final Properties CONFIG = loadConfig();
    private static final String BASE_URL = CONFIG.getProperty("base.url");

    private String selectedItemName;

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = MenuSteps.class.getClassLoader().getResourceAsStream("config.properties")) {
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

    @Given("I am on the SavorHub menu page")
    public void i_am_on_the_menu_page() {
        driver.get(BASE_URL + "/Customer/Menu");
    }

    @Then("I should see at least one menu item")
    public void i_should_see_at_least_one_menu_item() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<WebElement> items = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("hover-card")));
        assertFalse(items.isEmpty());
    }

    @When("I click on the first menu item")
    public void i_click_on_the_first_menu_item() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<WebElement> items = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("hover-card")));
        WebElement firstItem = items.getFirst();
        selectedItemName = firstItem.findElement(By.className("card-title")).getText();
        firstItem.click();
    }

    @Then("I should see that item's details page")
    public void i_should_see_that_items_details_page() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement header = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.className("details-item-header")));
        assertTrue(header.getText().contains(selectedItemName));
    }
}
