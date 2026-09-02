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
            MenuSteps.java    Step definitions for menu.feature
            LogoutSteps.java  Step definitions for logout.feature
            RegisterSteps.java Step definitions for registration.feature
            FavoriteSteps.java Step definitions for favorites.feature
            CartSteps.java    Step definitions for cart_and_checkout.feature
            OrderHistorySteps.java  Step definitions for order_history.feature
            ReviewSteps.java  Step definitions for reviews.feature
            PasswordResetSteps.java  Step definitions for password_reset.feature
            AccountManagementSteps.java  Step definitions for account_management.feature
            HomepageSteps.java  Step definitions for homepage_navigation.feature

src/test/resources/
  features/login.feature          The Gherkin scenarios themselves
  features/menu.feature           The Gherkin scenarios themselves
  features/logout.feature         The Gherkin scenarios themselves
  features/registration.feature   The Gherkin scenarios themselves
  features/favorites.feature      The Gherkin scenarios themselves
  features/cart_and_checkout.feature  The Gherkin scenarios themselves
  features/order_history.feature  The Gherkin scenarios themselves
  features/reviews.feature        The Gherkin scenarios themselves
  features/password_reset.feature The Gherkin scenarios themselves
  features/account_management.feature  The Gherkin scenarios themselves
  features/homepage_navigation.feature  The Gherkin scenarios themselves
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
- `menu.feature`
  - Browsing the menu shows available dishes
  - Viewing a menu item's details
- `logout.feature`
  - Logging out ends the session and returns the navbar to its logged-out state
- `registration.feature`
  - Registering a new account signs the user in automatically (a fresh,
    random email is generated per test run to avoid colliding with an
    already-registered address in the local dev database)
- `favorites.feature`
  - Favoriting a menu item shows it on the Favorites page
  - Removing a favorite asks for confirmation before it disappears
- `cart_and_checkout.feature`
  - Adding a menu item to the cart shows it with the correct total
  - Adjusting quantity updates the total, and decreasing to zero removes
    the item
  - Removing an item from the cart empties it
  - Proceeding to checkout reaches the order summary and starts a real
    Stripe checkout session (stops at the redirect - does not complete
    a payment, so it needs a Stripe secret key configured locally via
    `dotnet user-secrets` to pass)
- `order_history.feature`
  - A placed order appears in My Orders with the correct status
  - Viewing an order's details shows the same order and total as My
    Orders
  - An order's details are reachable by URL even after logging out
    (documents a real gap in SavorHub: OrderDetails has no
    [Authorize] attribute and no ownership check at all)
- `reviews.feature`
  - Submitting a review shows it with the correct rating and content
  - Editing my review updates its rating and content
  - The review API's DELETE endpoint has no ownership check, so any
    logged-in user can delete someone else's review (documents a
    real gap in SavorHub: ReviewController's Delete action isn't
    reachable from any page in the UI, but is still live and has no
    ownership check at all - unlike its own GetAll/Get/Put actions)
- `password_reset.feature`
  - Requesting a password reset for a registered email, and for one
    that isn't registered, both show the identical confirmation page
    (SavorHub deliberately never reveals whether an email exists)
  - Reset Password refuses to load without a reset code
  - Does not cover the actual click-the-link-and-reset-your-password
    journey - SavorHub's EmailSender is a no-op stub (it doesn't send
    an email or log anything), so there's no way for these tests to
    ever see a real reset link
- `account_management.feature`
  - Changing my password with the correct current password succeeds
  - Changing my password with an incorrect current password shows an
    error and doesn't navigate away
  - Updating my phone number on the profile page succeeds
  - Every scenario runs against a fresh, disposable registered
    account (never the shared test.email account), since Change
    Password genuinely mutates a real password and a failed run
    could otherwise desync config.properties from the database
  - Does not cover changing your email address - like password reset,
    it needs a confirmation link that SavorHub's EmailSender silently
    discards
- `homepage_navigation.feature`
  - The homepage shows its hero, Featured Items, and Why Choose Us
    sections
  - The Explore Menu button navigates to the menu page
  - The footer's Privacy link navigates to the Privacy page

## Planned next steps

- The Admin suite (category/food type/menu item/order management,
  review moderation) is the next major phase, once a Manager-role
  test persona is set up. A follow-up to Cart & Checkout that
  completes a real Stripe test payment end-to-end is also possible,
  but is a bigger, more fragile lift than the current coverage.
- Convert `Hooks.driver` to a `ThreadLocal<WebDriver>` so scenarios can eventually run in
  parallel without sharing a single static driver.
- Add a screenshot-on-failure hook to make failures easier to diagnose from CI or a
  teammate's machine.
- A `junit-platform.properties` file for reporting and parallel-execution configuration.
