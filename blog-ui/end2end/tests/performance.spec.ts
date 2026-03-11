import { test, expect } from "@playwright/test";

test.describe("Performance Optimizations", () => {
  test.describe("Resource Hints", () => {
    test("includes dns-prefetch for CDN", async ({ page }) => {
      await page.goto("/");
      const dnsPrefetch = page.locator(
        'link[rel="dns-prefetch"][href*="cdnjs"]'
      );
      await expect(dnsPrefetch).toBeAttached();
    });

    test("includes preconnect for CDN", async ({ page }) => {
      await page.goto("/");
      const preconnect = page.locator(
        'link[rel="preconnect"][href*="cdnjs"]'
      );
      await expect(preconnect).toBeAttached();
    });

    test("preloads critical CSS", async ({ page }) => {
      await page.goto("/");
      const preloadCss = page.locator(
        'link[rel="preload"][href="/pkg/blog-ui.css"]'
      );
      await expect(preloadCss).toBeAttached();
    });

    test("preloads WASM binary", async ({ page }) => {
      await page.goto("/");
      const preloadWasm = page.locator(
        'link[rel="preload"][href="/pkg/blog-ui.wasm"]'
      );
      await expect(preloadWasm).toBeAttached();
    });
  });

  test.describe("Conditional Prism.js Loading", () => {
    test("homepage does not load Prism.js scripts", async ({ page }) => {
      await page.goto("/");
      const prismScripts = page.locator('script[src*="prism"]');
      await expect(prismScripts).toHaveCount(0);
    });

    test("tags page does not load Prism.js scripts", async ({ page }) => {
      await page.goto("/tags");
      const prismScripts = page.locator('script[src*="prism"]');
      await expect(prismScripts).toHaveCount(0);
    });
  });

  test.describe("Service Worker", () => {
    test("registration script is present", async ({ page }) => {
      await page.goto("/");
      const swScript = page.locator("script", {
        hasText: "serviceWorker",
      });
      await expect(swScript).toBeAttached();
    });
  });

  test.describe("Image Optimization", () => {
    test("post images get lazy loading attributes", async ({ page }) => {
      await page.goto("/posts/1", { timeout: 10_000 });
      const images = page.locator(".post-content img");
      const count = await images.count();
      if (count > 0) {
        await expect(images.first()).toHaveAttribute("loading", "lazy");
        await expect(images.first()).toHaveAttribute("decoding", "async");
      }
    });
  });
});
