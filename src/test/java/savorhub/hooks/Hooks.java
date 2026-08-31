package savorhub.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

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

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
