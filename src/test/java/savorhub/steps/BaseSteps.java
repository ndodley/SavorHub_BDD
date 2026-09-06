package savorhub.steps;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import savorhub.hooks.Hooks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Shared base class for every step-definition class in this suite.
 *
 * <p>Holds the handful of things every Steps class previously copy-pasted
 * identically: the shared WebDriver from Hooks, the loaded
 * config.properties, and the scroll-then-JS-click helper used to dodge
 * ElementClickInterceptedException from things transiently overlapping a
 * target (a toastr notification, the fixed navbar, etc.).
 *
 * <p>Deliberately carries zero Cucumber step definitions (no {@code Given},
 * {@code When}, or {@code Then} methods here) - Cucumber-JVM discovers step methods across a glue class's
 * full type hierarchy, so an actual step method living here would get
 * registered once per subclass that extends it, producing duplicate/
 * ambiguous step definition errors at runtime. This class exists purely as
 * plain Java superclass state and helper methods, which is always safe to
 * inherit regardless of how many step-definition subclasses extend it.
 */
public abstract class BaseSteps {

    protected final WebDriver driver = Hooks.driver;

    protected static final Properties CONFIG = loadConfig();

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = BaseSteps.class.getClassLoader().getResourceAsStream("config.properties")) {
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

    // Selenium's own click() throws ElementClickInterceptedException whenever
    // something else transiently overlaps the target - a toastr notification,
    // the fixed navbar, etc. Scrolling the element to center first and then
    // clicking it via JS sidesteps that occlusion check entirely - the same
    // pattern every admin management page in this suite relies on.
    protected void scrollToCenterAndClick(WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
        js.executeScript("arguments[0].click();", element);
    }

    // driver.get() occasionally throws "Timed out receiving message from
    // renderer" - a long-standing ChromeDriver/DevTools-protocol flake
    // (see SeleniumHQ/selenium#6630) that's independent of the
    // pageLoadTimeout capability set in Hooks and isn't fixed by any driver
    // config; a couple of retries reliably works around it. Every step
    // class should call this instead of driver.get(...) directly.
    protected void navigateTo(String url) {
        final int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                driver.get(url);
                return;
            } catch (org.openqa.selenium.TimeoutException e) {
                if (attempt == maxAttempts) {
                    throw e;
                }
            }
        }
    }

    // A handful of Admin pages (Categories, Food Types, Reviews, and one spot
    // in MenuItems) render their Edit/Delete links as plain <a href> tags
    // rather than the DataTables/AJAX pattern MenuItems/OrderList mostly use
    // (see those classes' javadoc), so clicking one triggers a real
    // full-page navigation - exposed to the same ChromeDriver/DevTools
    // "Timed out receiving message from renderer" flake navigateTo() works
    // around above. It surfaces differently here: the JS click itself
    // returns immediately, so it's the *following* wait for the destination
    // page's content that times out. Retrying the click-then-wait pair
    // together (not just the wait) recovers from it the same way.
    protected WebElement clickThenAwait(WebElement linkToClick, org.openqa.selenium.By destinationLocator) {
        final int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                scrollToCenterAndClick(linkToClick);
                return new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                        .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(destinationLocator));
            } catch (org.openqa.selenium.TimeoutException e) {
                if (attempt == maxAttempts) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("unreachable");
    }
}
