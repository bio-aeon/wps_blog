import { test, expect } from "@playwright/test";

test.describe("Search Page", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/search");
  });

  test("displays search input", async ({ page }) => {
    const input = page.locator(".search-input");
    await expect(input).toBeVisible();
  });

  test("shows prompt when no query entered", async ({ page }) => {
    const prompt = page.locator(".search-prompt");
    await expect(prompt).toBeVisible({ timeout: 10_000 });
    await expect(prompt).toHaveText(/Enter a search term/);
  });

  test("typing query updates URL", async ({ page }) => {
    const input = page.locator(".search-input");
    await input.fill("rust");
    await input.press("Enter");
    // After form submission or debounce, URL should include q param
    await expect(page).toHaveURL(/[?&]q=rust/, { timeout: 10_000 });
  });

  test("search results appear as post cards", async ({ page }) => {
    const input = page.locator(".search-input");
    await input.fill("test");
    await input.press("Enter");

    // Wait for results or no-results message
    await page.waitForSelector(".post-card, .search-no-results", {
      timeout: 10_000,
    });
  });

  test("no results message for unknown query", async ({ page }) => {
    const input = page.locator(".search-input");
    await input.fill("zzznonexistentqueryzzzz");
    await input.press("Enter");

    const noResults = page.locator(".search-no-results");
    await expect(noResults).toBeVisible({ timeout: 10_000 });
  });

  test("direct navigation with query param pre-fills input", async ({
    page,
  }) => {
    await page.goto("/search?q=rust");
    const input = page.locator(".search-input");
    await expect(input).toHaveValue("rust");
  });
});
