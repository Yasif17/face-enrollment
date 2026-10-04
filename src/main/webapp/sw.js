
const CACHE_NAME = "face-attendance-v1";

const STATIC_ASSETS = [
    "/",
    "/enroll",
    "/live",
    "/live-enroll"
];


/*
 * INSTALL
 *
 * Cache the basic application pages.
 */
self.addEventListener("install", event => {

    console.log("Service Worker: installing");

    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => {
                return cache.addAll(STATIC_ASSETS);
            })
            .then(() => {
                return self.skipWaiting();
            })
    );
});


/*
 * ACTIVATE
 *
 * Remove old cache versions.
 */
self.addEventListener("activate", event => {

    console.log("Service Worker: activated");

    event.waitUntil(
        caches.keys()
            .then(cacheNames => {

                return Promise.all(
                    cacheNames
                        .filter(name =>
                            name !== CACHE_NAME
                        )
                        .map(name =>
                            caches.delete(name)
                        )
                );
            })
            .then(() => {
                return self.clients.claim();
            })
    );
});


/*
 * FETCH
 *
 * For now:
 *
 * API requests → network only
 *
 * Static/application pages →
 * network first, cache fallback
 */
self.addEventListener("fetch", event => {

    const request =
        event.request;

    const url =
        new URL(request.url);


    /*
     * Never cache API requests.
     */
    if (url.pathname.startsWith("/api/")) {
        return;
    }


    /*
     * Only handle GET requests.
     */
    if (request.method !== "GET") {
        return;
    }


    event.respondWith(

        fetch(request)
            .then(response => {

                /*
                 * Store a copy of successful
                 * responses in cache.
                 */

                const responseClone =
                    response.clone();

                caches.open(CACHE_NAME)
                    .then(cache => {
                        cache.put(
                            request,
                            responseClone
                        );
                    });

                return response;
            })
            .catch(() => {

                /*
                 * If network fails,
                 * try cache.
                 */

                return caches.match(request);
            })
    );
});