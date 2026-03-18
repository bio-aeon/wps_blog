import { test, expect } from "@playwright/test";

test.describe("Multi-language routing", () => {
  test("root redirects to a language-prefixed URL", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveURL(/^\/(en|ru|el)\//);
  });

  test("English home page loads at /en/", async ({ page }) => {
    await page.goto("/en/");
    await expect(page.locator("main")).toBeVisible();
  });

  test("Russian home page loads at /ru/", async ({ page }) => {
    await page.goto("/ru/");
    await expect(page.locator("main")).toBeVisible();
  });

  test("Greek home page loads at /el/", async ({ page }) => {
    await page.goto("/el/");
    await expect(page.locator("main")).toBeVisible();
  });

  test("language prefix preserved in post list URL", async ({ page }) => {
    await page.goto("/ru/posts");
    await expect(page).toHaveURL(/\/ru\/posts/);
  });

  test("language prefix preserved in tags URL", async ({ page }) => {
    await page.goto("/el/tags");
    await expect(page).toHaveURL(/\/el\/tags/);
  });
});
