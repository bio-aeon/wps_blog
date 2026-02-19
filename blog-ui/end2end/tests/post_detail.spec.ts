import { test, expect } from "@playwright/test";

test.describe("Post Detail Page", () => {
  test("renders post title and content", async ({ page }) => {
    // Navigate to posts first to find a valid post ID
    await page.goto("/posts");
    const firstPost = page.locator(".post-card").first();
    await expect(firstPost).toBeVisible({ timeout: 10_000 });
    const titleLink = firstPost.locator("h2 a");
    const postTitle = await titleLink.textContent();
    await titleLink.click();

    // Verify post detail page
    await expect(page).toHaveURL(/\/posts\/\d+/);
    const heading = page.locator(".post-detail .post-title");
    await expect(heading).toBeVisible({ timeout: 10_000 });
    await expect(heading).toHaveText(postTitle!);
  });

  test("displays post content", async ({ page }) => {
    await page.goto("/posts");
    const titleLink = page.locator(".post-card h2 a").first();
    await expect(titleLink).toBeVisible({ timeout: 10_000 });
    await titleLink.click();

    const content = page.locator(".post-content");
    await expect(content).toBeVisible({ timeout: 10_000 });
  });

  test("displays post meta with date and tags", async ({ page }) => {
    await page.goto("/posts");
    const titleLink = page.locator(".post-card h2 a").first();
    await expect(titleLink).toBeVisible({ timeout: 10_000 });
    await titleLink.click();

    const meta = page.locator(".post-meta");
    await expect(meta).toBeVisible({ timeout: 10_000 });
  });

  test("has comment section", async ({ page }) => {
    await page.goto("/posts");
    const titleLink = page.locator(".post-card h2 a").first();
    await expect(titleLink).toBeVisible({ timeout: 10_000 });
    await titleLink.click();

    const commentSection = page.locator(".comment-section");
    await expect(commentSection).toBeVisible({ timeout: 10_000 });
  });

  test("back link navigates to post list", async ({ page }) => {
    await page.goto("/posts");
    const titleLink = page.locator(".post-card h2 a").first();
    await expect(titleLink).toBeVisible({ timeout: 10_000 });
    await titleLink.click();

    const backLink = page.locator(".back-link");
    await expect(backLink).toBeVisible({ timeout: 10_000 });
    await backLink.click();
    await expect(page).toHaveURL(/\/posts$/);
  });

  test("non-existent post shows error", async ({ page }) => {
    await page.goto("/posts/999999");
    const error = page.locator(".error-display");
    await expect(error).toBeVisible({ timeout: 10_000 });
  });
});
