package savorhub.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Runs around every scenario: opens a fresh Chrome window before each one,
 * closes it after — so scenarios never leak state (cookies, sessions) into each other.
 */
public class Hooks {

    public static WebDriver driver;

    @Before
    public void setUp() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // SavorHub's local dev server runs on https with the ASP.NET Core dev
        // certificate, which Chrome may not trust unless you've run
        // `dotnet dev-certs https --trust`. This keeps tests from failing on
        // a cert warning page instead of the actual page under test.
        options.setAcceptInsecureCerts(true);

        // EAGER hands control back once the DOM is ready, rather than waiting
        // for every last resource (images, subresources) on the page to
        // finish loading. Admin list pages (Categories, Menu Items, Reviews)
        // accumulate rows - and their images - across test runs since only
        // the delete scenarios clean up after themselves, so the default
        // "normal" strategy (wait for the full window.onload) gets slower
        // and more failure-prone the more times this suite has run.
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        driver = new ChromeDriver(options);

        // A hard cap on driver.get()/navigation so a page that genuinely
        // won't settle fails fast with a clear TimeoutException instead of
        // hanging for minutes until the underlying HTTP client gives up on
        // its own.
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

        driver.manage().window().maximize();
    }

    @After
    public void tearDown(Scenario scenario) {
        if (driver != null) {
            // On a failure, save a screenshot + the page source + the URL the
            // browser was actually sitting on the moment the step gave up,
            // under target/failure-artifacts - nothing here is wired up to
            // Cucumber's own (unconfigured) reporting plugins, so writing
            // plain files is what actually shows up on disk to open, instead
            // of having to guess what the page looked like from the
            // exception message alone.
            if (scenario.isFailed()) {
                try {
                    Path dir = Path.of("target", "failure-artifacts");
                    Files.createDirectories(dir);
                    String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
                    String safeName = scenario.getName().replaceAll("[^a-zA-Z0-9-]+", "_");
                    String baseName = safeName + "_" + stamp;

                    byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    Files.write(dir.resolve(baseName + ".png"), screenshot);
                    Files.writeString(dir.resolve(baseName + ".html"), driver.getPageSource(), StandardCharsets.UTF_8);
                    Files.writeString(dir.resolve(baseName + ".txt"), "URL: " + driver.getCurrentUrl(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    // Best-effort - a broken capture (including a null page
                    // source, which Files.writeString would otherwise throw
                    // an uncaught NullPointerException on) should never mask
                    // the real assertion/timeout failure that already failed
                    // the scenario.
                }
            }
            driver.quit();
        }
    }
}
