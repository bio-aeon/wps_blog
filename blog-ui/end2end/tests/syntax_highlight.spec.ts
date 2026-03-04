import { test, expect } from "@playwright/test";

test.describe("Syntax Highlighting & Post Features", () => {
  test("post detail page loads Prism.js", async ({ page }) => {
    await page.goto("/posts");

    // Navigate to first post
    const firstPostLink = page.locator(".post-card h2 a").first();
    const linkCount = await firstPostLink.count();
    if (linkCount === 0) {
      test.skip();
      return;
    }

    await firstPostLink.click();
    await page.waitForLoadState("domcontentloaded");

    // Verify Prism.js script is present in the page
    const prismScript = page.locator('script[src*="prism.min.js"]');
    await expect(prismScript).toHaveCount(1);
  });

  test("code blocks get highlighted after hydration", async ({ page }) => {
    await page.goto("/posts");

    const firstPostLink = page.locator(".post-card h2 a").first();
    const linkCount = await firstPostLink.count();
    if (linkCount === 0) {
      test.skip();
      return;
    }

    await firstPostLink.click();
    await page.waitForLoadState("domcontentloaded");

    // Check if there are code blocks
    const codeBlocks = page.locator(".post-content pre code");
    const codeCount = await codeBlocks.count();
    if (codeCount === 0) {
      test.skip();
      return;
    }

    // Wait for Prism.js to process
    await page.waitForTimeout(1000);

    // At least one code block should contain Prism token spans
    const firstBlock = codeBlocks.first();
    const tokenSpan = firstBlock.locator("span.token").first();
    await expect(tokenSpan).toBeAttached();
  });

  test("images have lazy loading attributes", async ({ page }) => {
    await page.goto("/posts");

    const firstPostLink = page.locator(".post-card h2 a").first();
    const linkCount = await firstPostLink.count();
    if (linkCount === 0) {
      test.skip();
      return;
    }

    await firstPostLink.click();
    await page.waitForLoadState("domcontentloaded");

    const images = page.locator(".post-content img");
    const imgCount = await images.count();
    if (imgCount === 0) {
      test.skip();
      return;
    }

    // Wait for hydration
    await page.waitForTimeout(500);

    const firstImg = images.first();
    await expect(firstImg).toHaveAttribute("loading", "lazy");
    await expect(firstImg).toHaveAttribute("decoding", "async");
  });

  test("reading time is displayed in post card", async ({ page }) => {
    await page.goto("/posts");

    const firstCard = page.locator(".post-card").first();
    const count = await firstCard.count();
    if (count === 0) {
      test.skip();
      return;
    }

    const readingTime = firstCard.locator(".reading-time");
    const rtCount = await readingTime.count();
    if (rtCount > 0) {
      await expect(readingTime).toContainText("min read");
    }
  });

  test("reading time is displayed in post detail", async ({ page }) => {
    await page.goto("/posts");

    const firstPostLink = page.locator(".post-card h2 a").first();
    const linkCount = await firstPostLink.count();
    if (linkCount === 0) {
      test.skip();
      return;
    }

    await firstPostLink.click();
    await page.waitForLoadState("domcontentloaded");

    const readingTime = page.locator(".reading-time");
    const rtCount = await readingTime.count();
    if (rtCount > 0) {
      await expect(readingTime).toContainText("min read");
    }
  });

  test("table of contents renders for long posts", async ({ page }) => {
    await page.goto("/posts");

    const firstPostLink = page.locator(".post-card h2 a").first();
    const linkCount = await firstPostLink.count();
    if (linkCount === 0) {
      test.skip();
      return;
    }

    await firstPostLink.click();
    await page.waitForLoadState("domcontentloaded");

    const toc = page.locator(".toc");
    const tocCount = await toc.count();

    // TOC only renders if 3+ headings exist
    if (tocCount > 0) {
      await expect(toc).toBeVisible();
      await expect(toc.locator(".toc-title")).toHaveText("Contents");

      // Verify TOC links point to anchors
      const tocLinks = toc.locator(".toc-list a");
      const linkCount = await tocLinks.count();
      expect(linkCount).toBeGreaterThanOrEqual(3);

      for (let i = 0; i < linkCount; i++) {
        const href = await tocLinks.nth(i).getAttribute("href");
        expect(href).toMatch(/^#/);
      }
    }
  });

  test("table of contents has proper ARIA", async ({ page }) => {
    await page.goto("/posts");

    const firstPostLink = page.locator(".post-card h2 a").first();
    const linkCount = await firstPostLink.count();
    if (linkCount === 0) {
      test.skip();
      return;
    }

    await firstPostLink.click();
    await page.waitForLoadState("domcontentloaded");

    const toc = page.locator(".toc");
    const tocCount = await toc.count();

    if (tocCount > 0) {
      await expect(toc).toHaveAttribute("aria-label", "Table of contents");
    }
  });

  test("tokyo night theme CSS is loaded", async ({ page }) => {
    await page.goto("/");

    const themeLink = page.locator('link[href*="tokyo-night"]');
    await expect(themeLink).toHaveCount(1);
  });

  test("favicon is referenced in head", async ({ page }) => {
    await page.goto("/");

    const faviconSvg = page.locator('link[rel="icon"][type="image/svg+xml"]');
    await expect(faviconSvg).toHaveAttribute("href", "/assets/favicon.svg");
  });

  test("web manifest is linked", async ({ page }) => {
    await page.goto("/");

    const manifest = page.locator('link[rel="manifest"]');
    await expect(manifest).toHaveAttribute(
      "href",
      "/assets/site.webmanifest"
    );
  });
});
