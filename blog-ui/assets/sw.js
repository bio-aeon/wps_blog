var CACHE_NAME = "wps-blog-v1";
var STATIC_ASSETS = [
    "/pkg/blog-ui.css",
    "/pkg/blog-ui.js",
    "/assets/favicon.svg",
    "/assets/vendor/tokyo-night.css"
];

self.addEventListener("install", function(event) {
    event.waitUntil(
        caches.open(CACHE_NAME).then(function(cache) {
            return cache.addAll(STATIC_ASSETS);
        })
    );
    self.skipWaiting();
});

self.addEventListener("activate", function(event) {
    event.waitUntil(
        caches.keys().then(function(keys) {
            return Promise.all(
                keys.filter(function(k) { return k !== CACHE_NAME; })
                    .map(function(k) { return caches.delete(k); })
            );
        })
    );
    self.clients.claim();
});

self.addEventListener("fetch", function(event) {
    var url = new URL(event.request.url);

    // Cache-first for static assets (/pkg/ and /assets/)
    if (url.pathname.startsWith("/pkg/") || url.pathname.startsWith("/assets/")) {
        event.respondWith(
            caches.match(event.request).then(function(cached) {
                return cached || fetch(event.request);
            })
        );
        return;
    }

    // Network-first for everything else (HTML pages, API calls)
    event.respondWith(
        fetch(event.request).catch(function() {
            return caches.match(event.request);
        })
    );
});
