package savorhub.steps.customer;

import io.cucumber.java.en.Given;
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
 * Step definitions for account_management.feature.
 *
 * <p>Every scenario here runs against a brand-new throwaway account
 * (created via registration.feature's own reusable steps in the
 * Background), never the shared test.email account from
 * config.properties. Change Password genuinely mutates the account's
 * real password server-side - running that against the shared account
 * would risk permanently desyncing config.properties' test.password from
 * what SavorHub's database actually has, which would break every other
 * feature's login step. A fresh, disposable account has nothing to
 * protect, so it's safe to mutate freely and never needs cleanup.
 *
 * <p>Locators come straight from Areas/Identity/Pages/Account/Manage/
 * ChangePassword.cshtml(.cs) and Index.cshtml(.cs):
 *   - ChangePassword's fields are asp-for="Input.OldPassword" /
 *     "Input.NewPassword" / "Input.ConfirmPassword", so their ids are
 *     Input_OldPassword / Input_NewPassword / Input_ConfirmPassword (same
 *     asp-for -> id convention used everywhere else in this app). Its
 *     submit button has no id, just the literal text "Update password".
 *   - An incorrect current password doesn't fail DataAnnotations
 *     validation (Required/StringLength/Compare all still pass with a
 *     wrong-but-well-formed old password) - it's only caught when
 *     _userManager.ChangePasswordAsync() itself returns a failure result,
 *     which re-renders the same page (no redirect) with the error inside
 *     the standard asp-validation-summary div, rather than redirecting
 *     anywhere - this suite confirms both the error text and that the
 *     URL never left ChangePassword.
 *   - Both ChangePassword and Manage/Index show their TempData
 *     StatusMessage through the shared _StatusMessage.cshtml partial,
 *     which renders as a Bootstrap "alert-success" div (or "alert-danger"
 *     if the message text starts with "Error").
 *   - Manage/Index's phone number field is asp-for="Input.PhoneNumber"
 *     (id "Input_PhoneNumber"), pre-filled from the phone number supplied
 *     during registration; its Save button has id
 *     "update-profile-button".
 * If SavorHub's markup or code changes, re-check the real DOM/source
 * rather than trusting this comment.
 */
public class AccountManagementSteps extends BaseSteps {

    private static final String BASE_URL = CONFIG.getProperty("base.url");

    // Matches RegisterSteps' own hardcoded password for a freshly-registered
    // account. Kept in sync manually since it's a plain literal on both
    // sides, not shared config - if RegisterSteps' password ever changes,
    // update this constant too.
    private static final String FRESH_ACCOUNT_PASSWORD = "Test1234!";

    private WebDriverWait shortWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Given("I am on the SavorHub change password page")
    public void i_am_on_the_change_password_page() {
        navigateTo(BASE_URL + "/Identity/Account/Manage/ChangePassword");
    }

    @When("I change my password to a new valid password")
    public void i_change_my_password_to_a_new_valid_password() {
        submitChangePassword(FRESH_ACCOUNT_PASSWORD, "NewTest1234!");
    }

    @When("I try to change my password with the wrong current password")
    public void i_try_to_change_my_password_with_the_wrong_current_password() {
        submitChangePassword("WrongPassword1!", "NewTest1234!");
    }

    private void submitChangePassword(String oldPassword, String newPassword) {
        driver.findElement(By.id("Input_OldPassword")).sendKeys(oldPassword);
        driver.findElement(By.id("Input_NewPassword")).sendKeys(newPassword);
        driver.findElement(By.id("Input_ConfirmPassword")).sendKeys(newPassword);
        driver.findElement(By.xpath("//button[contains(normalize-space(.), 'Update password')]")).click();
    }

    @Then("I should see a status message that my password has been changed")
    public void i_should_see_a_status_message_that_my_password_has_been_changed() {
        WebElement status = shortWait().until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.alert-success")));
        assertTrue(status.getText().contains("Your password has been changed."));
    }

    @Then("I should see a password change error and remain on the change password page")
    public void i_should_see_a_password_change_error_and_remain_on_the_change_password_page() {
        WebElement errorDiv = shortWait().until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("div[role='alert'].text-danger")));
        assertFalse(errorDiv.getText().trim().isEmpty(),
                "Expected a validation error message after changing password with the wrong current "
                + "password, but the validation summary was empty.");
        assertTrue(driver.getCurrentUrl().contains("/Manage/ChangePassword"),
                "Expected to remain on the Change Password page after a failed attempt, but the URL "
                + "was: " + driver.getCurrentUrl());
    }

    @Given("I am on the SavorHub manage profile page")
    public void i_am_on_the_manage_profile_page() {
        navigateTo(BASE_URL + "/Identity/Account/Manage");
    }

    @When("I update my phone number to a new value")
    public void i_update_my_phone_number_to_a_new_value() {
        WebElement phoneField = shortWait().until(
                ExpectedConditions.presenceOfElementLocated(By.id("Input_PhoneNumber")));
        phoneField.clear();
        phoneField.sendKeys("555-123-4567");
        driver.findElement(By.id("update-profile-button")).click();
    }

    @Then("I should see a status message that my profile has been updated")
    public void i_should_see_a_status_message_that_my_profile_has_been_updated() {
        WebElement status = shortWait().until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.alert-success")));
        assertTrue(status.getText().contains("Your profile has been updated"));
    }
}
