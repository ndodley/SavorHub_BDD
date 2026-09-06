package savorhub.steps.customer;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import savorhub.steps.BaseSteps;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for favorites.feature.
 *
 * <p>Locators come straight from SavorHub's real markup at
 * SavorHub.Web/Pages/Customer/Menu/Index.cshtml and
 * SavorHub.Web/Pages/Customer/Favorites/Index.cshtml:
 *   - Favorites/Index.cshtml.cs's IndexModel carries [Authorize], so this
 *     page (unlike the menu) requires being logged in - these scenarios
 *     log in first via LoginSteps' reused step text.
 *   - On the menu page, each card's favorite toggle is a plain
 *     &lt;form asp-page-handler="ToggleFavorite"&gt; POST with no
 *     confirmation - the button has no id, only title="Toggle Favourite",
 *     and its child &lt;i&gt; icon flips between "bi-heart" (not
 *     favorited) and "bi-heart-fill" (favorited), which is how favorited
 *     state is detected without guessing.
 *   - ToggleFavorite is a true toggle server-side
 *     (Index.cshtml.cs's OnPostToggleFavorite adds if absent, removes if
 *     present), so "favoriting" a menu item only clicks the button when
 *     the icon shows it isn't already favorited - clicking an
 *     already-favorited item would remove it instead.
 *   - Both the menu card and the Favorites-page card share the
 *     "hover-card" / "card-title" classes already used in menu.feature.
 *   - The Favorites page's own Remove button (title="Remove favourite")
 *     is wrapped in a form with onsubmit="return confirm(...)" - a real
 *     native JS confirm() dialog, handled here via Selenium's Alert API.
 *   - Both buttons live in a &lt;form&gt; that's a sibling of the card's
 *     &lt;a&gt;, both children of the same outer "position-relative"
 *     wrapper div - so the right button for a given card is found by
 *     walking up to that wrapper from the card/title element, then
 *     searching back down for the button by its title attribute.
 * If SavorHub's markup changes, re-check the real DOM in DevTools rather
 * than trusting this comment.
 */
public class FavoriteSteps extends BaseSteps {

    private static final String BASE_URL = CONFIG.getProperty("base.url");

    private String favoritedItemName;

    private WebElement cardWrapperFor(WebElement cardOrTitle) {
        return cardOrTitle.findElement(By.xpath("./ancestor::div[contains(@class, 'position-relative')][1]"));
    }

    @When("I favorite the first menu item")
    public void i_favorite_the_first_menu_item() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<WebElement> items = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("hover-card")));
        WebElement firstItem = items.getFirst();
        favoritedItemName = firstItem.findElement(By.className("card-title")).getText();

        WebElement wrapper = cardWrapperFor(firstItem);
        WebElement toggleButton = wrapper.findElement(By.cssSelector("button[title='Toggle Favourite']"));
        boolean alreadyFavorited = !toggleButton.findElements(By.cssSelector("i.bi-heart-fill")).isEmpty();

        if (!alreadyFavorited) {
            toggleButton.click();
            wait.until(ExpectedConditions.stalenessOf(toggleButton));
        }
    }

    @When("I go to the Favorites page")
    public void i_go_to_the_favorites_page() {
        navigateTo(BASE_URL + "/Customer/Favorites");
    }

    @Then("I should see that item on the Favorites page")
    public void i_should_see_that_item_on_the_favorites_page() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<WebElement> titles = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("card-title")));
        boolean found = titles.stream().anyMatch(t -> t.getText().equals(favoritedItemName));
        assertTrue(found);
    }

    @When("I remove that item from my favorites and confirm the prompt")
    public void i_remove_that_item_from_my_favorites_and_confirm_the_prompt() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<WebElement> titles = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("card-title")));
        WebElement title = titles.stream()
                .filter(t -> t.getText().equals(favoritedItemName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected to find \"" + favoritedItemName + "\" on the Favorites page, but it wasn't there."));

        WebElement wrapper = cardWrapperFor(title);
        WebElement removeButton = wrapper.findElement(By.cssSelector("button[title='Remove favourite']"));
        removeButton.click();

        Alert confirmDialog = wait.until(ExpectedConditions.alertIsPresent());
        confirmDialog.accept();
    }

    @Then("I should not see that item on the Favorites page")
    public void i_should_not_see_that_item_on_the_favorites_page() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(d -> {
            List<WebElement> titles = d.findElements(By.className("card-title"));
            return titles.stream().noneMatch(t -> t.getText().equals(favoritedItemName));
        });
    }
}
