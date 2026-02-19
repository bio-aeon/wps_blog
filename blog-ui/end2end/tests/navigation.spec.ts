import { test, expect } from "@playwright/test";

test.describe("Navigation", () => {
  test("header is visible on all pages", async ({ page }) => {
    for (const path of ["/", "/posts", "/tags", "/search"]) {
      await page.goto(path);
      await expect(page.locator(".site-header")).toBeVisible();
    }
  });

  test("footer is visible on all pages", async ({ page }) => {
    for (const path of ["/", "/posts", "/tags", "/search"]) {
      await page.goto(path);
      await expect(page.locator(".site-footer")).toBeVisible();
    }
  });

  test("nav links navigate correctly", async ({ page }) => {
    await page.goto("/");

    // Click Blog link
    await page.locator(".main-nav a", { hasText: "Blog" }).click();
    await expect(page).toHaveURL(/\/posts$/);

    // Click Tags link
    await page.locator(".main-nav a", { hasText: "Tags" }).click();
    await expect(page).toHaveURL(/\/tags$/);

    // Click Home link
    await page.locator(".main-nav a", { hasText: "Home" }).click();
    await expect(page).toHaveURL(/\/$/);

    // Click About link
    await page.locator(".main-nav a", { hasText: "About" }).click();
    await expect(page).toHaveURL(/\/pages\/about$/);
  });

  test("logo navigates to home", async ({ page }) => {
    await page.goto("/posts");
    await page.locator(".logo").click();
    await expect(page).toHaveURL(/\/$/);
  });

  test("404 page for non-existent route", async ({ page }) => {
    const response = await page.goto("/nonexistent-route");
    expect(response?.status()).toBe(404);

    const notFound = page.locator(".not-found-page");
    await expect(notFound).toBeVisible();
    await expect(notFound.locator("h1")).toHaveText(/404/);
  });

  test("404 page has link back to home", async ({ page }) => {
    await page.goto("/nonexistent-route");
    const backLink = page.locator(".not-found-page .back-link");
    await expect(backLink).toBeVisible();
    await backLink.click();
    await expect(page).toHaveURL(/\/$/);
  });

  test("mobile menu toggle works at small viewport", async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto("/");

    const menuToggle = page.locator(".mobile-menu-toggle");
    await expect(menuToggle).toBeVisible();

    // Click to open menu
    await menuToggle.click();
    const nav = page.locator(".main-nav");
    await expect(nav).toHaveClass(/open/);
  });

  test("search trigger navigates to search page", async ({ page }) => {
    await page.goto("/");
    const searchLink = page.locator(".search-trigger");
    await expect(searchLink).toBeVisible();
    await searchLink.click();
    await expect(page).toHaveURL(/\/search$/);
  });
});
