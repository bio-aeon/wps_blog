import { test, expect } from "@playwright/test";

test.describe("Language switcher", () => {
  test("language switcher is visible", async ({ page }) => {
    await page.goto("/en/");
    const switcher = page.locator(".language-switcher");
    await expect(switcher).toBeVisible();
  });

  test("shows all three languages", async ({ page }) => {
    await page.goto("/en/");
    const links = page.locator(".language-switcher a");
    await expect(links).toHaveCount(3);
  });

  test("current language is highlighted", async ({ page }) => {
    await page.goto("/en/");
    const activeLink = page.locator(".language-switcher a.active");
    await expect(activeLink).toHaveCount(1);
    await expect(activeLink).toHaveText("EN");
  });

  test("Russian language is highlighted on /ru/", async ({ page }) => {
    await page.goto("/ru/");
    const activeLink = page.locator(".language-switcher a.active");
    await expect(activeLink).toHaveText("RU");
  });
});
