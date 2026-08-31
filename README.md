# SavorHub BDD Tests

Automated end-to-end tests for [SavorHub](https://github.com/ndodley/SavorHub), written as
Gherkin feature files and run through Selenium, Cucumber, and JUnit 5.

## Stack

| Component | Version |
|---|---|
| Java | 25 |
| Selenium | 4.48.0 |
| Cucumber | 7.34.7 |
| JUnit Platform / Jupiter | 5.14.2 (via `junit-bom`) |
| WebDriverManager | 6.3.4 |

`pom.xml` pins `maven.compiler.release` to 25, so building this project (from IntelliJ or
the command line) requires **JDK 25 or newer** — the compiler itself has to be at least as
new as the release it's targeting. WebDriverManager downloads and wires up the matching
ChromeDriver automatically — there's no browser driver binary to install or manage by hand.

## Project layout

```
src/test/java/savorhub/
  hooks/    Hooks.java        Opens a fresh Chrome window before each scenario, closes it after
  runners/  TestRunner.java   JUnit 5 entry point that discovers and runs the .feature files
  steps/    LoginSteps.java   Step definitions for login.feature

src/test/resources/
  features/login.feature          The Gherkin scenarios themselves
  config.properties.example       Tracked template — copy this, don't edit it directly
  config.properties               Your real local config — gitignored, never committed
```

Each scenario gets its own Chrome window (opened in `Hooks`' `@Before` and closed in
`@After`), so scenarios never leak cookies or session state into one another.

## One-time setup

1. **SavorHub itself must be running locally.** These tests drive a real browser against a
   real running instance — they don't mock or stub anything. Start SavorHub from Visual
   Studio using the **IIS Express** run profile, which serves it at `https://localhost:44325`.
2. **Chrome must trust the ASP.NET Core dev certificate**, or every test will fail on a
   certificate warning page instead of the page actually under test. If you haven't already,
   run:
   ```
   dotnet dev-certs https --trust
   ```
3. **Register a real test account** in SavorHub (the normal "Register" flow on the site).
   Don't reuse a personal or production-like account — this account's credentials will sit in
   plain text in a local config file.
4. **Create your local config file.** In `src/test/resources/`, copy
   `config.properties.example` to a new file named `config.properties` in the same folder,
   then fill in your test account's real email and password:
   ```properties
   base.url=https://localhost:44325
   test.email=your-real-test-account@example.com
   test.password=YourRealTestPassword1!
   ```
   `config.properties` is listed in `.gitignore` and will never be committed — that's the
   whole point of the split between the tracked `.example` template and this real file. Do
   not remove it from `.gitignore`, and don't paste real credentials anywhere else in the
   project.

## Running the tests

From IntelliJ IDEA: open `src/test/java/savorhub/runners/TestRunner.java`, right-click it in
the Project panel (or use the ▶ gutter icon), and choose **Run 'TestRunner'**. A real Chrome
window will open and step through each scenario; results show in the Run panel at the
bottom, with a pass/fail breakdown per scenario.

From the command line, with SavorHub already running:

```
mvn test
```

### Troubleshooting

- **Connection refused / page never loads** — SavorHub isn't running, or it's running on a
  different port than `base.url` in `config.properties`. Confirm the IIS Express profile is
  active and check the actual URL in a normal browser tab first.
- **`IllegalStateException: Missing src/test/resources/config.properties`** — you haven't
  created the file yet (see setup step 4), or it wasn't picked up as a test resource. Try a
  Maven rebuild if it's already there.
- **Login step fails but you're sure the credentials are right** — try logging in manually
  with the same email/password in a normal browser tab to rule out a typo or an unconfirmed
  account.

## Current coverage

- `login.feature`
  - Successful login with valid credentials
  - Login fails with an incorrect password

## Planned next steps

- Additional feature files covering menu browsing, ordering, and account management.
- Convert `Hooks.driver` to a `ThreadLocal<WebDriver>` so scenarios can eventually run in
  parallel without sharing a single static driver.
- Add a screenshot-on-failure hook to make failures easier to diagnose from CI or a
  teammate's machine.
- A `junit-platform.properties` file for reporting and parallel-execution configuration.
