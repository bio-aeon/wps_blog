import { test, expect } from "@playwright/test";

test.describe("Contact Page", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/contact");
  });

  test("renders contact page", async ({ page }) => {
    const article = page.locator(".contact-page");
    await expect(article).toBeVisible({ timeout: 10_000 });
  });

  test("contact form is present with all fields", async ({ page }) => {
    const form = page.locator(".contact-form");
    await expect(form).toBeVisible({ timeout: 10_000 });

    await expect(form.locator('input[type="text"]')).toBeVisible();
    await expect(form.locator('input[type="email"]')).toBeVisible();
    await expect(form.locator("textarea")).toBeVisible();
    await expect(form.locator(".contact-submit")).toBeVisible();
  });

  test("submitting empty form shows validation errors", async ({ page }) => {
    const form = page.locator(".contact-form");
    await expect(form).toBeVisible({ timeout: 10_000 });

    await form.locator(".contact-submit").click();

    const errors = form.locator(".field-error");
    await expect(errors.first()).toBeVisible();
    expect(await errors.count()).toBeGreaterThanOrEqual(1);
  });

  test("submitting with invalid email shows error", async ({ page }) => {
    const form = page.locator(".contact-form");
    await expect(form).toBeVisible({ timeout: 10_000 });

    await form.locator('input[type="text"]').fill("Test User");
    await form.locator('input[type="email"]').fill("not-an-email");
    // Fill subject field if present (may be a separate input)
    const inputs = form.locator("input");
    const count = await inputs.count();
    if (count > 2) {
      // name, email, subject inputs
      await inputs.nth(2).fill("Test Subject");
    }
    await form.locator("textarea").fill("This is a test message body");
    await form.locator(".contact-submit").click();

    const error = form.locator(".field-error");
    await expect(error.first()).toBeVisible();
  });

  test("honeypot field is hidden", async ({ page }) => {
    const honeypot = page.locator(".contact-honeypot");
    if ((await honeypot.count()) > 0) {
      await expect(honeypot).toBeHidden();
    }
  });

  test("successful submission shows success message", async ({ page }) => {
    const form = page.locator(".contact-form");
    await expect(form).toBeVisible({ timeout: 10_000 });

    const uniqueName = `E2E Tester ${Date.now()}`;
    await form.locator('input[type="text"]').fill(uniqueName);
    await form.locator('input[type="email"]').fill("e2e@test.com");
    // Fill subject field if present
    const inputs = form.locator("input");
    const count = await inputs.count();
    if (count > 2) {
      await inputs.nth(2).fill("E2E Test Subject");
    }
    await form
      .locator("textarea")
      .fill("This is an E2E test message for the contact form");
    await form.locator(".contact-submit").click();

    const success = page.locator(".contact-success");
    await expect(success).toBeVisible({ timeout: 10_000 });
  });

  test("header and footer are present", async ({ page }) => {
    await expect(page.locator(".site-header")).toBeVisible();
    await expect(page.locator(".site-footer")).toBeVisible();
  });
});
