import { test, expect } from "@playwright/test";

test.describe("Home Page", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/");
  });

  test("has correct page title", async ({ page }) => {
    await expect(page).toHaveTitle("WPS Blog");
  });

  test("displays hero section with site name", async ({ page }) => {
    const hero = page.locator(".home-hero h1");
    await expect(hero).toBeVisible();
    await expect(hero).toHaveText("WPS Blog");
  });

  test("displays subtitle", async ({ page }) => {
    const subtitle = page.locator(".home-subtitle");
    await expect(subtitle).toBeVisible();
  });

  test("shows recent posts", async ({ page }) => {
    const posts = page.locator(".post-card");
    await expect(posts.first()).toBeVisible({ timeout: 10_000 });
    const count = await posts.count();
    expect(count).toBeGreaterThan(0);
    expect(count).toBeLessThanOrEqual(5);
  });

  test("view all posts link navigates to /posts", async ({ page }) => {
    const link = page.locator(".view-all-link");
    await expect(link).toBeVisible({ timeout: 10_000 });
    await expect(link).toHaveText(/View all posts/);
    await link.click();
    await expect(page).toHaveURL(/\/posts$/);
  });

  test("sidebar is visible", async ({ page }) => {
    const sidebar = page.locator("aside.sidebar");
    await expect(sidebar).toBeVisible();
  });
});
