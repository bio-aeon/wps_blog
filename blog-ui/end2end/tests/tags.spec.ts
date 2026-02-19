import { test, expect } from "@playwright/test";

test.describe("Tags", () => {
  test.describe("Tag List Page", () => {
    test.beforeEach(async ({ page }) => {
      await page.goto("/tags");
    });

    test("has correct page title", async ({ page }) => {
      await expect(page).toHaveTitle(/Tags/);
    });

    test("displays tags with post counts", async ({ page }) => {
      const tags = page.locator(".tag-list-item");
      await expect(tags.first()).toBeVisible({ timeout: 10_000 });

      const first = tags.first();
      await expect(first.locator(".tag-name")).toBeVisible();
      await expect(first.locator(".tag-count")).toBeVisible();
    });

    test("clicking tag navigates to tag posts page", async ({ page }) => {
      const tag = page.locator(".tag-list-item").first();
      await expect(tag).toBeVisible({ timeout: 10_000 });
      await tag.click();
      await expect(page).toHaveURL(/\/tags\/.+/);
    });
  });

  test.describe("Tag Posts Page", () => {
    test("shows filtered posts for a tag", async ({ page }) => {
      // First go to tags to find a valid tag
      await page.goto("/tags");
      const tag = page.locator(".tag-list-item").first();
      await expect(tag).toBeVisible({ timeout: 10_000 });
      const tagName = await tag.locator(".tag-name").textContent();
      await tag.click();

      // Should show posts for this tag
      await expect(page).toHaveURL(/\/tags\/.+/);
      const heading = page.locator("h1");
      await expect(heading).toBeVisible({ timeout: 10_000 });
    });
  });

  test.describe("Tag Cloud in Sidebar", () => {
    test("tag cloud renders in sidebar", async ({ page }) => {
      await page.goto("/");
      const tagCloud = page.locator(".sidebar .tag-cloud");
      await expect(tagCloud).toBeVisible({ timeout: 10_000 });
    });

    test("tag cloud items are clickable links", async ({ page }) => {
      await page.goto("/");
      const cloudItem = page.locator(".tag-cloud-item").first();
      await expect(cloudItem).toBeVisible({ timeout: 10_000 });
      await cloudItem.click();
      await expect(page).toHaveURL(/\/tags\/.+/);
    });
  });
});
