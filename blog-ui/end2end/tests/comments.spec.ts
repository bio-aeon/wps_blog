import { test, expect } from "@playwright/test";

async function navigateToFirstPost(page: import("@playwright/test").Page) {
  await page.goto("/posts");
  const titleLink = page.locator(".post-card h2 a").first();
  await expect(titleLink).toBeVisible({ timeout: 10_000 });
  await titleLink.click();
  await expect(page).toHaveURL(/\/posts\/\d+/);
  // Wait for comment section to load
  await expect(page.locator(".comment-section")).toBeVisible({
    timeout: 10_000,
  });
}

test.describe("Comments", () => {
  test("comment section is displayed on post detail", async ({ page }) => {
    await navigateToFirstPost(page);
    const heading = page.locator(".comments-heading");
    await expect(heading).toBeVisible();
    await expect(heading).toHaveText(/Comments/);
  });

  test("comment form is present", async ({ page }) => {
    await navigateToFirstPost(page);
    const form = page.locator(".comment-form");
    await expect(form).toBeVisible();
    await expect(form.locator("input, textarea")).toHaveCount(3); // name, email, textarea
  });

  test("submitting empty name shows validation error", async ({ page }) => {
    await navigateToFirstPost(page);
    const form = page.locator(".comment-form");
    await expect(form).toBeVisible();

    // Fill email and text but leave name empty
    await form.locator('input[type="email"]').fill("test@test.com");
    await form.locator("textarea").fill("Test comment");
    await form.locator(".comment-submit").click();

    const error = form.locator(".field-error");
    await expect(error.first()).toBeVisible();
  });

  test("submitting invalid email shows validation error", async ({
    page,
  }) => {
    await navigateToFirstPost(page);
    const form = page.locator(".comment-form");
    await expect(form).toBeVisible();

    await form.locator('input[type="text"]').fill("TestUser");
    await form.locator('input[type="email"]').fill("invalid-email");
    await form.locator("textarea").fill("Test comment");
    await form.locator(".comment-submit").click();

    const error = form.locator(".field-error");
    await expect(error.first()).toBeVisible();
  });

  test("submitting empty text shows validation error", async ({ page }) => {
    await navigateToFirstPost(page);
    const form = page.locator(".comment-form");
    await expect(form).toBeVisible();

    await form.locator('input[type="text"]').fill("TestUser");
    await form.locator('input[type="email"]').fill("test@test.com");
    // Leave textarea empty
    await form.locator(".comment-submit").click();

    const error = form.locator(".field-error");
    await expect(error.first()).toBeVisible();
  });

  test("successfully submitting comment form", async ({ page }) => {
    await navigateToFirstPost(page);
    const form = page.locator(".comment-form");
    await expect(form).toBeVisible();

    const uniqueName = `E2E Tester ${Date.now()}`;
    await form.locator('input[type="text"]').fill(uniqueName);
    await form.locator('input[type="email"]').fill("e2e@test.com");
    await form.locator("textarea").fill("This is an E2E test comment");
    await form.locator(".comment-submit").click();

    // After submission, the new comment should appear in the list
    const newComment = page.locator(".comment-author", { hasText: uniqueName });
    await expect(newComment).toBeVisible({ timeout: 10_000 });
  });

  test("comments display with author, text, and date", async ({ page }) => {
    await navigateToFirstPost(page);

    const comments = page.locator(".comment-item");
    // Comments may or may not exist, but if they do, check structure
    if ((await comments.count()) > 0) {
      const first = comments.first();
      await expect(first.locator(".comment-author")).toBeVisible();
      await expect(first.locator(".comment-body")).toBeVisible();
      await expect(first.locator(".comment-date")).toBeVisible();
    }
  });

  test("comment rating buttons are visible", async ({ page }) => {
    await navigateToFirstPost(page);

    const comments = page.locator(".comment-item");
    if ((await comments.count()) > 0) {
      const first = comments.first();
      const rating = first.locator(".comment-rating");
      await expect(rating).toBeVisible();
      await expect(rating.locator(".rate-btn")).toHaveCount(2); // upvote + downvote
      await expect(rating.locator(".rating-value")).toBeVisible();
    }
  });

  test("reply button shows reply form", async ({ page }) => {
    await navigateToFirstPost(page);

    const comments = page.locator(".comment-item");
    if ((await comments.count()) > 0) {
      const replyBtn = comments.first().locator(".reply-btn");
      await expect(replyBtn).toBeVisible();
      await replyBtn.click();

      // Reply form should appear within the comment
      const replyForm = comments.first().locator(".comment-form");
      await expect(replyForm).toBeVisible();
    }
  });
});
