import { test, expect } from "@playwright/test";

test.describe("About Page", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/about");
  });

  test("renders about page", async ({ page }) => {
    const article = page.locator(".about-page");
    await expect(article).toBeVisible({ timeout: 10_000 });
  });

  test("displays profile header with name and title", async ({ page }) => {
    const header = page.locator(".profile-header");
    await expect(header).toBeVisible({ timeout: 10_000 });

    const name = header.locator(".profile-name");
    await expect(name).toBeVisible();

    const title = header.locator(".profile-title");
    await expect(title).toBeVisible();
  });

  test("resume link is present when available", async ({ page }) => {
    const header = page.locator(".profile-header");
    await expect(header).toBeVisible({ timeout: 10_000 });

    const resumeLink = header.locator(".profile-resume-link");
    // Resume link may or may not exist depending on config
    if ((await resumeLink.count()) > 0) {
      await expect(resumeLink).toHaveAttribute("href", /.+/);
    }
  });

  test("displays skills section", async ({ page }) => {
    const skillsSection = page.locator(".about-section", {
      has: page.locator(".about-section-title", { hasText: /Skills/i }),
    });
    await expect(skillsSection).toBeVisible({ timeout: 10_000 });

    const skillGroups = page.locator(".skill-group");
    if ((await skillGroups.count()) > 0) {
      const firstGroup = skillGroups.first();
      await expect(firstGroup.locator(".skill-group-title")).toBeVisible();
      await expect(firstGroup.locator(".skill-bar").first()).toBeVisible();
    }
  });

  test("skill bars show name and proficiency", async ({ page }) => {
    const skillBars = page.locator(".skill-bar");
    await expect(skillBars.first()).toBeVisible({ timeout: 10_000 });

    if ((await skillBars.count()) > 0) {
      const first = skillBars.first();
      await expect(first.locator(".skill-name")).toBeVisible();
      await expect(first.locator(".skill-percent")).toBeVisible();
      await expect(first.locator(".skill-bar-track")).toBeVisible();
    }
  });

  test("displays experience timeline", async ({ page }) => {
    const timeline = page.locator(".experience-timeline");
    await expect(timeline).toBeVisible({ timeout: 10_000 });

    const items = timeline.locator(".timeline-item");
    if ((await items.count()) > 0) {
      const first = items.first();
      await expect(first.locator(".timeline-position")).toBeVisible();
      await expect(first.locator(".timeline-company")).toBeVisible();
      await expect(first.locator(".timeline-date")).toBeVisible();
    }
  });

  test("displays social links", async ({ page }) => {
    const socialLinks = page.locator(".social-links");
    await expect(socialLinks).toBeVisible({ timeout: 10_000 });

    const links = socialLinks.locator(".social-link");
    if ((await links.count()) > 0) {
      const first = links.first();
      await expect(first).toHaveAttribute("href", /^https?:\/\//);
    }
  });

  test("header and footer are present", async ({ page }) => {
    await expect(page.locator(".site-header")).toBeVisible();
    await expect(page.locator(".site-footer")).toBeVisible();
  });
});
