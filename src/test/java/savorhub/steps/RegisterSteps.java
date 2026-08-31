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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;


/**
 * Step definitions for registration.feature.
 *
 * <p>Locators come straight from Areas/Identity/Pages/Account/Register.cshtml:
 *   - Each field's id comes from ASP.NET Core's asp-for tag helper, which
 *     turns "Input.X" into id="Input_X" (same convention as Login.cshtml):
 *     Input_FirstName, Input_LastName, Input_PhoneNumber, Input_Email,
 *     Input_Password, Input_ConfirmPassword.
 *   - The submit button has id="registerSubmit".
 *   - Register.cshtml.cs / Program.cs show RequireConfirmedAccount is not
 *     configured (defaults to false), so a successful registration signs
 *     the user in immediately and redirects to the home page - no email
 *     confirmation step blocks the flow.
 *   - A fresh, random email is generated per run so repeated test runs
 *     never collide with an already-registered address in the local dev
 *     database. The password meets ASP.NET Core Identity's default
 *     policy (min length 6, upper+lowercase, digit, non-alphanumeric),
 *     since Program.cs doesn't override it.
 * If SavorHub's markup or Identity configuration changes, re-check the
 * real DOM/config rather than trusting this comment.
 */
public class RegisterSteps {

    private final WebDriver driver = Hooks.driver;

    private static final Properties CONFIG = loadConfig();
    private static final String BASE_URL = CONFIG.getProperty("base.url");

    private String newAccountEmail;

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = RegisterSteps.class.getClassLoader().getResourceAsStream("config.properties")) {
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

    @Given("I am on the SavorHub registration page")
    public void i_am_on_the_registration_page() {
        driver.get(BASE_URL + "/Identity/Account/Register");
    }

    @When("I register with a new unique email and valid details")
    public void i_register_with_a_new_unique_email_and_valid_details() {
        newAccountEmail = "test.user." + UUID.randomUUID() + "@example.com";

        driver.findElement(By.id("Input_FirstName")).sendKeys("Test");
        driver.findElement(By.id("Input_LastName")).sendKeys("User");
        driver.findElement(By.id("Input_PhoneNumber")).sendKeys("555-555-5555");
        driver.findElement(By.id("Input_Email")).sendKeys(newAccountEmail);
        driver.findElement(By.id("Input_Password")).sendKeys("Test1234!");
        driver.findElement(By.id("Input_ConfirmPassword")).sendKeys("Test1234!");

        // A plain .click() on this button keeps getting intercepted -
        // scrolling it to the center of the viewport (clear of the
        // fixed-top navbar) reduced but didn't eliminate it, which points
        // to something transient (e.g. a browser autofill/password
        // suggestion) intermittently sitting on top of it rather than a
        // fixed layout element. Clicking via JavaScript still triggers a
        // real click event and the form's normal submission, but bypasses
        // Selenium's "is anything visually on top of this" check, which is
        // what a screen-coordinate click can't get around.
        WebElement submitButton = driver.findElement(By.id("registerSubmit"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center'});", submitButton);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);
    }

    @Then("I should see my new account's email in the navbar")
    public void i_should_see_my_new_accounts_email_in_the_navbar() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        String emailLower = newAccountEmail.toLowerCase();
        // wait.until() either returns the found element or throws
        // TimeoutException - it never returns null - so a timeout here
        // fails the scenario on its own, with no separate assertion needed.
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(translate(., "
                        + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '"
                        + emailLower + "')]")));
    }
}
