package savorhub.steps.common;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import savorhub.steps.BaseSteps;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for password_reset.feature.
 *
 * <p>Confirmed by reading SavorHub.Utilities/EmailSender.cs: its
 * SendEmailAsync is a no-op stub ("return Task.CompletedTask;") - no email
 * is ever sent and nothing is logged anywhere, so there is no way for this
 * suite to ever capture a real reset link or exercise the actual
 * click-the-link-and-reset-your-password journey. What IS fully testable
 * without it, and is what this feature covers:
 *   - ForgotPasswordModel.OnPostAsync() deliberately redirects to the same
 *     ForgotPasswordConfirmation page whether the submitted email belongs
 *     to a real account or not ("Don't reveal that the user does not
 *     exist or is not confirmed") - real conditional logic worth its own
 *     scenario, even without ever seeing the resulting email.
 *   - ResetPassword's OnGet(string code) returns a plain
 *     BadRequest("A code must be supplied for password reset.") when no
 *     code is present in the URL at all. That response bypasses the
 *     app's normal Razor layout entirely (it's a raw ContentResult, not a
 *     rendered page), so this suite checks the raw page body text rather
 *     than a styled element for that one assertion.
 *
 * <p>Locators come straight from Areas/Identity/Pages/Account/
 * ForgotPassword.cshtml and ForgotPasswordConfirmation.cshtml:
 *   - ForgotPassword's email field is asp-for="Input.Email", so its id is
 *     "Input_Email" (same asp-for -> id convention used everywhere else
 *     in this app). Its submit button has no id, just the literal text
 *     "Reset Password".
 *   - ForgotPasswordConfirmation's message is a plain <p>, not wrapped in
 *     anything project-specific.
 * If SavorHub's markup or code changes, re-check the real DOM/source
 * rather than trusting this comment.
 */
public class PasswordResetSteps extends BaseSteps {

    private static final String BASE_URL = CONFIG.getProperty("base.url");
    private static final String VALID_EMAIL = CONFIG.getProperty("test.email");

    private WebDriverWait shortWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Given("I am on the SavorHub forgot password page")
    public void i_am_on_the_forgot_password_page() {
        navigateTo(BASE_URL + "/Identity/Account/ForgotPassword");
    }

    @When("I request a password reset for a registered email")
    public void i_request_a_password_reset_for_a_registered_email() {
        requestReset(VALID_EMAIL);
    }

    @When("I request a password reset for an email that isn't registered")
    public void i_request_a_password_reset_for_an_email_that_isnt_registered() {
        requestReset("nonexistent.user." + UUID.randomUUID() + "@example.com");
    }

    private void requestReset(String email) {
        driver.findElement(By.id("Input_Email")).sendKeys(email);
        driver.findElement(By.xpath("//button[contains(normalize-space(.), 'Reset Password')]")).click();
    }

    @Then("I should see the check-your-email confirmation")
    public void i_should_see_the_check_your_email_confirmation() {
        WebElement message = shortWait().until(ExpectedConditions.presenceOfElementLocated(By.tagName("p")));
        assertTrue(message.getText().contains("Please check your email to reset your password."));
    }

    @When("I visit the Reset Password page directly with no code")
    public void i_visit_the_reset_password_page_directly_with_no_code() {
        navigateTo(BASE_URL + "/Identity/Account/ResetPassword");
    }

    @Then("I should see a message that a code must be supplied")
    public void i_should_see_a_message_that_a_code_must_be_supplied() {
        String bodyText = shortWait().until(
                ExpectedConditions.presenceOfElementLocated(By.tagName("body"))).getText();
        assertTrue(bodyText.contains("A code must be supplied for password reset."));
    }
}
