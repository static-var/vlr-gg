# Mobile Sentry bridge

This package sends Android and iOS SDK envelopes through a hostname you own. The route is mobile SDK → your HTTPS ingress → Nginx allowlist → Sentry Relay → Sentry's original ingest host. It uses the server's ordinary datacenter egress.

Relay runs in `proxy` mode. This mode needs no Sentry auth token, Relay credentials, or organization registration. Sentry's free Developer plan can use this forwarding path. Registered, managed Relays require Business or Enterprise. Proxy mode leaves project filtering, quotas, and server-side scrubbing to Sentry. It does not grant paid product features or additional quota. See [Relay modes](https://docs.sentry.io/product/relay/modes/), [Relay setup](https://docs.sentry.io/product/relay/getting-started/), and the [trusted Relay plan restriction](https://docs.sentry.io/api/organizations/update-an-organization/).

The pinned Relay version is [26.8.0](https://github.com/getsentry/relay/releases/tag/26.8.0). The pinned Nginx image is `1.30.4-alpine`. Review and update these versions with normal server maintenance.

## Configure

1. Copy `.env.example` to `.env`. Fill in the original Sentry ingest hostname, one or two project id/public key pairs, your public hostname, and the server's existing ingress network. Do not include a scheme or path in either hostname. Project ids and public keys come from each original DSN.
2. Run `python3 configure.py`. It validates the values and writes `.generated/relay/config.yml`, `.generated/nginx.conf`, and `.generated/Caddyfile.fragment`. Both `.env` and `.generated/` are ignored by Git. The renderer sets `.env` to mode 600. Generated files contain public routing identifiers and must be readable by the unprivileged container users; never add an auth token to them.
3. Run `python3 -m unittest -v test_configure.py` and `docker compose config --quiet` in this directory.
4. Review the configuration and obtain deployment approval before starting it on a server. After approval, run `docker compose up -d` from this directory.
5. Route your hostname through the existing TLS ingress to `http://vlr-sentry-ingress:8080` on the configured shared network. Preserve the request body, `Content-Encoding`, `Content-Type`, and `X-Sentry-Auth`. Allow a 200 MiB request body and a 60 second upstream timeout. Do not cache responses. Do not log request bodies, DSN keys, query strings, or auth headers. Only forward the configured project envelope paths and `/healthz` to this service.
6. Validate the running Nginx configuration with `docker compose exec ingress nginx -t`, check `docker compose ps`, and request `https://YOUR_HOST/healthz`. Then send an explicitly approved diagnostic event from each app build and confirm receipt in the intended Sentry project.

Nothing in this package opens a host port or configures DNS/TLS. It expects the existing server ingress to terminate HTTPS. Nexus normally uses `appsnet`; choose the actual ingress network on Atlas if deploying there. Relay connects only to the dedicated `backend` Docker network, which has normal egress. Its external DNS resolvers are `1.1.1.1` and `1.0.0.1`, because Nexus's default NextDNS configuration blocks Sentry ingest domains. This setting applies only to the Relay container; the host keeps its existing DNS configuration. Nginx joins the backend and shared ingress networks. No household egress gateway is involved.

## App configuration

Change only the host in the original DSN. For example, `https://PUBLIC_KEY@o123.ingest.us.sentry.io/PROJECT_ID` becomes `https://PUBLIC_KEY@YOUR_HOST/PROJECT_ID`. Keep the original hostname in this package's `SENTRY_UPSTREAM_HOST`. This is the integration described in Sentry's [Relay setup documentation](https://docs.sentry.io/product/relay/getting-started/).

Use the rewritten DSN for both the common KMP initialization and any native Android/iOS initialization. KMP does not expose a JavaScript-style `tunnel` option. Replacing the DSN host keeps the normal SDK transport, compression, cache, and retries.

Current Android and Cocoa SDK transports send error events, native crash envelopes, session health, transactions, profiles, logs, replay recordings, attachments, and client reports through the envelope endpoint. This bridge retains complete binary request bodies and forwards response status and rate-limit headers. Breadcrumbs travel inside their associated events. See the [envelope format](https://develop.sentry.dev/sdk/data-model/envelopes/) and [envelope item rules](https://develop.sentry.dev/sdk/data-model/envelope-items/).

The ingress intentionally allows only `POST /api/PROJECT_ID/envelope/` for the configured projects with a matching `X-Sentry-Auth` public key or `sentry_key` query parameter. It does not support envelope-header-only authentication or separate minidump/store/unreal endpoints. The app's Android and Cocoa SDKs use HTTP authentication and envelope transport. Verify both native crash paths on devices after changing SDK versions; add a route only if evidence shows a required transport change. Debug symbols, source bundles, release management, and Sentry API requests continue from build machines directly to Sentry with private build credentials.

## Limits and operation

Nginx accepts a maximum 200 MiB body, streams it to Relay, limits the whole service to 20 requests per second with a burst of 100, and permits eight concurrent uploads. The limit is global, so it does not trust client-supplied forwarding headers. It returns 429 when throttling. Adjust these values with measured traffic. The public key is an application routing identifier and is extractable from shipped apps; the allowlist prevents forwarding to arbitrary projects, but it cannot authenticate legitimate installations.

Relay has two CPU cores, 2 GiB of memory, eight concurrent upstream requests, and a 128-envelope memory queue. It marks itself unhealthy above 1.5 GiB of memory. Nginx has a 128 MiB memory limit. No persistent spool is configured; queued data can be lost if Relay restarts or exhausts its buffer. SDK buffering and retries still apply to requests that have not been accepted. Health is exposed at `/healthz`; only critical Nginx errors and Relay warnings/errors are logged, with bounded Docker log rotation. Forwarded IP, cookie, and authorization headers are removed.

Sentry recommends at least 2 GiB RAM per Relay and two instances for availability. This package starts one instance for a small mobile app; it is an additional dependency in event delivery. Add a second instance and load balancing if uptime requires it. See [Relay operating guidelines](https://docs.sentry.io/product/relay/operating-guidelines/).

A first-party host can avoid DNS rules aimed at Sentry's domains, but a user or network can also block this hostname. Keep the app's telemetry preference effective before enqueueing events. This transport must not override a user opting out.

## Validation status

Deployed on Nexus on September 8, 2026, under `/home/ubuntu/apps/vlr-sentry-relay`. The public health endpoint is [valorant-app.staticvar.dev/healthz](https://valorant-app.staticvar.dev/healthz). The existing Caddyfile at `/home/ubuntu/apps/site/Caddyfile` contains the added hostname route. Its prior contents were preserved in a protected backup on Nexus. The existing Netlify credential stayed on Nexus and was used with explicit approval to create the hostname's A record.

Validation passed for the Python configuration tests, Compose configuration, the running Nginx parser, Caddy validation/reload, public DNS, and HTTPS certificate verification. Both containers run with read-only filesystems; the ingress health check is healthy. Relay, ingress, and the host each reported the normal Nexus egress IP, `80.225.216.54`.

Live requests returned the expected 404 for an unconfigured project and unrelated API paths, 403 for missing/wrong keys, and 405 for GET on the envelope route. Initial testing found that Nexus's default NextDNS resolver sent the Sentry hostname to a blockpage. Relay correctly rejected its untrusted TLS certificate. Dedicated DNS resolvers for Relay fixed that upstream lookup without changing host DNS or disabling certificate checks.

After the DNS correction, a gzip-compressed synthetic envelope labelled `VLR relay transport verification: bridge-public-dns` returned HTTP 200 and event id `f8bb11b783c0411799391e710b2cb309` in the `relay-verification` environment. It was confirmed in Sentry as `VLR-MOBILE-2`. A direct-upstream control also arrived as `VLR-MOBILE-1`. The iOS simulator then delivered native and Kotlin handled errors, an uncaught Kotlin crash with symbolicated Kotlin/Swift source locations, structured logs, network spans, release/session health and a view-hierarchy attachment through the bridge. Android emulator testing also confirmed handled errors, uncaught JVM crashes, logs, traces, operation metrics, sessions, feedback, view hierarchy and Replay delivery. The inspected Android Replay frame masked the feedback confirmation text. `/healthz` reports Relay readiness, not downstream Sentry receipt. Physical-device native crash paths and upstream rate-limit responses still require their own verification.
