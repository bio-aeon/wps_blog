import { test, expect } from "@playwright/test";

test.describe("Multi-language accessibility", () => {
  test("language switcher has aria-label", async ({ page }) => {
    await page.goto("/en/");
    const switcher = page.locator(".language-switcher");
    await expect(switcher).toHaveAttribute("aria-label", "Language");
  });

  test("active language link has aria-current", async ({ page }) => {
    await page.goto("/en/");
    const active = page.locator(".language-switcher a.active");
    await expect(active).toHaveAttribute("aria-current", "true");
  });

  test("skip-to-content link is present", async ({ page }) => {
    await page.goto("/en/");
    const skipLink = page.locator("a.skip-link");
    await expect(skipLink).toBeAttached();
  });
});
