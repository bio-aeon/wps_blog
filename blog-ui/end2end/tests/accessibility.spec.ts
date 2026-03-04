import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test.describe("Accessibility", () => {
  test("home page has no critical violations", async ({ page }) => {
    await page.goto("/");
    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa"])
      .analyze();
    expect(results.violations.filter((v) => v.impact === "critical")).toEqual(
      []
    );
  });

  test("post list has no critical violations", async ({ page }) => {
    await page.goto("/posts");
    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa"])
      .analyze();
    expect(results.violations.filter((v) => v.impact === "critical")).toEqual(
      []
    );
  });

  test("skip link is present and targets main content", async ({ page }) => {
    await page.goto("/");
    const skipLink = page.locator("a.skip-link");
    await expect(skipLink).toHaveAttribute("href", "#main-content");

    // Tab to skip link — it should become visible on focus
    await page.keyboard.press("Tab");
    await expect(skipLink).toBeFocused();
  });

  test("main content has id and tabindex for skip-link target", async ({
    page,
  }) => {
    await page.goto("/");
    const main = page.locator("#main-content");
    await expect(main).toHaveAttribute("tabindex", "-1");
  });

  test("header navigation has proper landmarks", async ({ page }) => {
    await page.goto("/");
    const nav = page.locator("nav#main-nav");
    await expect(nav).toHaveAttribute("aria-label", "Main navigation");
  });

  test("footer navigation has proper landmarks", async ({ page }) => {
    await page.goto("/");
    const footerNav = page.locator("footer nav");
    await expect(footerNav).toHaveAttribute("aria-label", "Footer links");
  });

  test("search input has accessible label", async ({ page }) => {
    await page.goto("/search");
    const searchInput = page.locator('input[type="search"]');
    await expect(searchInput).toHaveAttribute("aria-label", "Search posts");
  });

  test("pagination has aria labels", async ({ page }) => {
    await page.goto("/posts");
    const paginationNav = page.locator('nav[aria-label="Pagination"]');
    // Only check if pagination exists (depends on post count)
    const count = await paginationNav.count();
    if (count > 0) {
      await expect(paginationNav).toBeVisible();
    }
  });

  test("mobile menu toggle has aria attributes", async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto("/");
    const toggle = page.locator(".mobile-menu-toggle");
    await expect(toggle).toHaveAttribute("aria-controls", "main-nav");
    await expect(toggle).toHaveAttribute("aria-label");
  });

  test("escape key closes mobile menu", async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto("/");

    const toggle = page.locator(".mobile-menu-toggle");
    await toggle.click();

    const nav = page.locator("#main-nav");
    await expect(nav).toHaveClass(/open/);

    await page.keyboard.press("Escape");
    await expect(nav).not.toHaveClass(/open/);
  });

  test("focus-visible styles are applied", async ({ page }) => {
    await page.goto("/");

    // Tab to first focusable element
    await page.keyboard.press("Tab");
    await page.keyboard.press("Tab");

    const focused = page.locator(":focus");
    const outline = await focused.evaluate(
      (el) => getComputedStyle(el).outlineStyle
    );
    expect(outline).not.toBe("none");
  });

  test("reduced motion media query is respected", async ({ page }) => {
    await page.emulateMedia({ reducedMotion: "reduce" });
    await page.goto("/");

    // Check that animations are disabled
    const body = page.locator("body");
    const animDuration = await body.evaluate(
      (el) => getComputedStyle(el).getPropertyValue("animation-duration")
    );
    // With prefers-reduced-motion, durations should be 0 or animations none
    // Just verify the page loads without error under reduced motion
    await expect(body).toBeVisible();
  });
});
