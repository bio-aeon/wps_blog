(function() {
    if (!("PerformanceObserver" in window)) return;

    var metrics = {};

    new PerformanceObserver(function(list) {
        var entries = list.getEntries();
        metrics.lcp = entries[entries.length - 1].startTime;
    }).observe({ type: "largest-contentful-paint", buffered: true });

    var clsValue = 0;
    new PerformanceObserver(function(list) {
        var entries = list.getEntries();
        for (var i = 0; i < entries.length; i++) {
            if (!entries[i].hadRecentInput) clsValue += entries[i].value;
        }
        metrics.cls = clsValue;
    }).observe({ type: "layout-shift", buffered: true });

    new PerformanceObserver(function(list) {
        var entry = list.getEntries()[0];
        metrics.fid = entry.processingStart - entry.startTime;
    }).observe({ type: "first-input", buffered: true });

    var navEntry = performance.getEntriesByType("navigation")[0];
    if (navEntry) metrics.ttfb = navEntry.responseStart;

    document.addEventListener("visibilitychange", function() {
        if (document.visibilityState === "hidden" && Object.keys(metrics).length > 0) {
            metrics.url = location.pathname;
            metrics.ts = Date.now();
            navigator.sendBeacon("/api/rum", JSON.stringify(metrics));
        }
    });
})();
