import { test, expect } from "@playwright/test";

test.describe("Static Pages", () => {
  test("about page renders title and content", async ({ page }) => {
    await page.goto("/pages/about");
    const article = page.locator(".static-page");
    await expect(article).toBeVisible({ timeout: 10_000 });

    const heading = article.locator("h1");
    await expect(heading).toBeVisible();

    const content = page.locator(".static-page-content");
    await expect(content).toBeVisible();
  });

  test("non-existent static page shows error", async ({ page }) => {
    await page.goto("/pages/this-page-does-not-exist");
    const error = page.locator(".error-display");
    await expect(error).toBeVisible({ timeout: 10_000 });
  });

  test("header and footer are present on static pages", async ({ page }) => {
    await page.goto("/pages/about");
    await expect(page.locator(".site-header")).toBeVisible();
    await expect(page.locator(".site-footer")).toBeVisible();
  });
});
