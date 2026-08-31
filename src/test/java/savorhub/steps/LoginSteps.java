package savorhub.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import savorhub.hooks.Hooks;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for login.feature.
 *
 * Field IDs below (Input_Email, Input_Password, login-submit) come straight
 * from SavorHub's real markup at Areas/Identity/Pages/Account/Login.cshtml
 * (ASP.NET Core Identity's scaffolded login page, tag helpers asp-for="Input.Email"
 * / asp-for="Input.Password" render as id="Input_Email" / id="Input_Password").
 * If SavorHub's login page changes, re-check the real DOM in DevTools rather
 * than trusting this comment.
 */
public class LoginSteps {

    private final WebDriver driver = Hooks.driver;

    // TODO: confirm this matches the port dotnet run / Visual Studio prints
    // for the "https" launch profile (Properties/launchSettings.json).
    private static final String BASE_URL = "https://localhost:44325";

    // TODO: replace with a real account you've registered against your local
    // SavorHub database (via the Register page) before running these tests.
    private static final String VALID_EMAIL = "your-test-user@example.com";
    private static final String VALID_PASSWORD = "YourTestPassword1!";

    @Given("I am on the SavorHub login page")
    public void i_am_on_the_login_page() {
        driver.get(BASE_URL + "/Identity/Account/Login");
    }

    @When("I log in with a valid email and password")
    public void i_log_in_with_valid_credentials() {
        driver.findElement(By.id("Input_Email")).sendKeys(VALID_EMAIL);
        driver.findElement(By.id("Input_Password")).sendKeys(VALID_PASSWORD);
        driver.findElement(By.id("login-submit")).click();
    }

    @When("I log in with an incorrect password")
    public void i_log_in_with_incorrect_password() {
        driver.findElement(By.id("Input_Email")).sendKeys(VALID_EMAIL);
        driver.findElement(By.id("Input_Password")).sendKeys("wrong-password");
        driver.findElement(By.id("login-submit")).click();
    }

    @Then("I should see the account menu in the navbar")
    public void i_should_see_the_account_menu() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        boolean present = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.partialLinkText("Account"))) != null;
        assertTrue(present);
    }

    @Then("I should see a login error message")
    public void i_should_see_a_login_error() {
        // Login.cshtml renders <div asp-validation-summary="ModelOnly" class="text-danger" role="alert">
        // on a failed login, so this waits for that element rather than any
        // specific error text (which can change without the behavior changing).
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        boolean present = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("div[role='alert'].text-danger"))) != null;
        assertTrue(present);
    }
}
