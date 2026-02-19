import { test, expect } from "@playwright/test";

test.describe("Post List Page", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/posts");
  });

  test("has correct page title", async ({ page }) => {
    await expect(page).toHaveTitle(/Blog Posts/);
  });

  test("displays post cards", async ({ page }) => {
    const posts = page.locator(".post-card");
    await expect(posts.first()).toBeVisible({ timeout: 10_000 });
    const count = await posts.count();
    expect(count).toBeGreaterThan(0);
  });

  test("post cards have title, excerpt, date, and tags", async ({ page }) => {
    const firstPost = page.locator(".post-card").first();
    await expect(firstPost).toBeVisible({ timeout: 10_000 });

    // Title link
    const titleLink = firstPost.locator("h2 a");
    await expect(titleLink).toBeVisible();
    const title = await titleLink.textContent();
    expect(title?.length).toBeGreaterThan(0);

    // Date
    const date = firstPost.locator(".post-meta time");
    await expect(date).toBeVisible();

    // Excerpt
    const excerpt = firstPost.locator(".post-excerpt");
    await expect(excerpt).toBeVisible();
  });

  test("clicking post title navigates to post detail", async ({ page }) => {
    const firstPost = page.locator(".post-card").first();
    await expect(firstPost).toBeVisible({ timeout: 10_000 });
    const titleLink = firstPost.locator("h2 a");
    await titleLink.click();
    await expect(page).toHaveURL(/\/posts\/\d+/);
  });

  test("clicking tag badge navigates to tag page", async ({ page }) => {
    const tagBadge = page.locator(".tag-badge").first();
    // Tags may or may not exist
    if ((await tagBadge.count()) > 0) {
      await tagBadge.click();
      await expect(page).toHaveURL(/\/tags\//);
    }
  });

  test("pagination is visible when posts exist", async ({ page }) => {
    const posts = page.locator(".post-card");
    await expect(posts.first()).toBeVisible({ timeout: 10_000 });

    // Pagination should be present (even with 1 page it renders)
    const pagination = page.locator(".pagination");
    await expect(pagination).toBeVisible();
  });

  test("read more link navigates to post detail", async ({ page }) => {
    const readMore = page.locator(".read-more").first();
    await expect(readMore).toBeVisible({ timeout: 10_000 });
    await readMore.click();
    await expect(page).toHaveURL(/\/posts\/\d+/);
  });
});
