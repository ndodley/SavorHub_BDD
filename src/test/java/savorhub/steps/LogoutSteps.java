package savorhub.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import savorhub.hooks.Hooks;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for logout.feature.
 *
 * <p>Locators come straight from Pages/Shared/_LoginPartial.cshtml:
 *   - Logout isn't a page you navigate to directly in normal use. The
 *     account dropdown toggle carries classes "dropdown-toggle d-flex"
 *     (the "d-flex" distinguishes it from the plain "dropdown-toggle" used
 *     by the staff-only Operations/Admin nav dropdowns, so this selector
 *     is safe even for an account with elevated roles).
 *   - Clicking that toggle reveals a Bootstrap dropdown menu containing a
 *     Logout button with a plain id="logout" — the menu is hidden
 *     (not clickable) until the toggle is clicked, so the toggle must be
 *     clicked first.
 *   - The Register/Login links that reappear after logout carry
 *     id="register" / id="login".
 * If SavorHub's markup changes, re-check the real DOM in DevTools rather
 * than trusting this comment.
 */
public class LogoutSteps {

    private final WebDriver driver = Hooks.driver;

    @When("I log out")
    public void i_log_out() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement accountDropdownToggle = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("a.dropdown-toggle.d-flex")));
        accountDropdownToggle.click();

        WebElement logoutButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("logout")));
        logoutButton.click();
    }

    @Then("I should see the login and register links in the navbar")
    public void i_should_see_login_and_register_links() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement loginLink = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login")));
        WebElement registerLink = driver.findElement(By.id("register"));

        assertTrue(loginLink.isDisplayed());
        assertTrue(registerLink.isDisplayed());
    }
}
