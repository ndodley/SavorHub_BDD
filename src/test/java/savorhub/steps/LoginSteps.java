package savorhub.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import savorhub.hooks.Hooks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Step definitions for login.feature.
 *
 * <p>Field IDs below (Input_Email, Input_Password, login-submit) come straight
 * from SavorHub's real markup at Areas/Identity/Pages/Account/Login.cshtml
 * (ASP.NET Core Identity's scaffolded login page, tag helpers asp-for="Input.Email"
 * / asp-for="Input.Password" render as id="Input_Email" / id="Input_Password").
 * If SavorHub's login page changes, re-check the real DOM in DevTools rather
 * than trusting this comment.
 *
 * <p>BASE_URL / VALID_EMAIL / VALID_PASSWORD come from
 * src/test/resources/config.properties, which is gitignored so real test
 * credentials never get committed. Copy config.properties.example to
 * config.properties (same folder) and fill in your local values.
 *
 * <p>MANAGER_EMAIL / MANAGER_PASSWORD come from the same file's manager.email
 * / manager.password keys - a pre-existing account with the Manager role,
 * needed by the Admin test suite. SavorHub has no self-registration path
 * to that role, so this account must already exist in your local database.
 */
public class LoginSteps {

    private final WebDriver driver = Hooks.driver;

    private static final Properties CONFIG = loadConfig();

    private static final String BASE_URL = CONFIG.getProperty("base.url");
    private static final String VALID_EMAIL = CONFIG.getProperty("test.email");
    private static final String VALID_PASSWORD = CONFIG.getProperty("test.password");
    private static final String MANAGER_EMAIL = CONFIG.getProperty("manager.email");
    private static final String MANAGER_PASSWORD = CONFIG.getProperty("manager.password");

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = LoginSteps.class.getClassLoader().getResourceAsStream("config.properties")) {
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

    @When("I log in with valid Manager credentials")
    public void i_log_in_with_valid_manager_credentials() {
        assertNotNull(MANAGER_EMAIL, "Missing manager.email in config.properties - see "
                + "config.properties.example. The Admin test suite needs a pre-existing "
                + "account with the Manager role; SavorHub has no self-registration path to one.");
        assertNotNull(MANAGER_PASSWORD, "Missing manager.password in config.properties - see "
                + "config.properties.example.");
        driver.findElement(By.id("Input_Email")).sendKeys(MANAGER_EMAIL);
        driver.findElement(By.id("Input_Password")).sendKeys(MANAGER_PASSWORD);
        driver.findElement(By.id("login-submit")).click();
    }

    @Then("I should see the account menu in the navbar")
    public void i_should_see_the_account_menu() {
        // SavorHub's navbar doesn't contain the literal word "Account" - once
        // logged in it shows the current user's email (as a dropdown toggle,
        // uppercased via CSS). Rather than hardcode that markup, this checks
        // that the logged-in test account's own email appears somewhere on
        // the page, matched case-insensitively since the display is
        // uppercase but the underlying text/value is normal case.
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        String emailLower = VALID_EMAIL.toLowerCase();
        // wait.until(...) either returns the found element or throws
        // TimeoutException - there's nothing left to assert afterward.
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(translate(., "
                        + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '"
                        + emailLower + "')]")));
    }

    @Then("I should see a login error message")
    public void i_should_see_a_login_error() {
        // Login.cshtml renders <div asp-validation-summary="ModelOnly" class="text-danger" role="alert">
        // on a failed login, so this waits for that element rather than any
        // specific error text (which can change without the behavior changing).
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        // Same reasoning as i_should_see_the_account_menu() above.
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("div[role='alert'].text-danger")));
    }
}
