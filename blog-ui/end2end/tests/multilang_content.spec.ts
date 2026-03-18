import { test, expect } from "@playwright/test";

test.describe("Translated content", () => {
  test("English navigation text", async ({ page }) => {
    await page.goto("/en/");
    await expect(page.locator("nav")).toContainText("Home");
    await expect(page.locator("nav")).toContainText("Posts");
    await expect(page.locator("nav")).toContainText("Tags");
    await expect(page.locator("nav")).toContainText("About");
  });

  test("Russian navigation text", async ({ page }) => {
    await page.goto("/ru/");
    await expect(page.locator("nav")).toContainText("Главная");
    await expect(page.locator("nav")).toContainText("Блог");
    await expect(page.locator("nav")).toContainText("Обо мне");
  });

  test("Greek navigation text", async ({ page }) => {
    await page.goto("/el/");
    await expect(page.locator("nav")).toContainText("Αρχική");
    await expect(page.locator("nav")).toContainText("Ιστολόγιο");
  });

  test("footer text is translated for Russian", async ({ page }) => {
    await page.goto("/ru/");
    await expect(page.locator("footer")).toContainText("Все права защищены");
  });
});
