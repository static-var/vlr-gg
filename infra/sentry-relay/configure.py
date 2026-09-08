#!/usr/bin/env python3
"""Render private Relay and ingress configuration without executing shell input."""

import argparse
import os
from pathlib import Path
import re


UPSTREAM = re.compile(r"o[0-9]+\.ingest(?:\.[a-z]{2})?\.sentry\.io\Z")
HOST = re.compile(r"(?=.{1,253}\Z)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}\Z")
PROJECT_KEY = re.compile(r"([1-9][0-9]*):([a-f0-9]{32})\Z")
FIELDS = {"SENTRY_UPSTREAM_HOST", "SENTRY_PROJECT_KEYS", "PUBLIC_HOST", "INGRESS_NETWORK"}


def read_config(path):
    values = {}
    for number, line in enumerate(path.read_text().splitlines(), 1):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        name, separator, value = line.partition("=")
        if not separator or name not in FIELDS or name in values:
            raise ValueError(f"Invalid or duplicate setting on line {number}")
        values[name] = value.strip()
    return validate(values)


def validate(values):
    if set(values) != FIELDS:
        raise ValueError("Set all four fields from .env.example")
    if not UPSTREAM.fullmatch(values["SENTRY_UPSTREAM_HOST"]):
        raise ValueError("SENTRY_UPSTREAM_HOST must be the original organization ingest hostname")
    if not HOST.fullmatch(values["PUBLIC_HOST"]):
        raise ValueError("PUBLIC_HOST must be a lowercase public hostname without a scheme or path")
    if not re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9_.-]{0,62}", values["INGRESS_NETWORK"]):
        raise ValueError("INGRESS_NETWORK must be an existing Docker network name")
    pairs = []
    seen = set()
    for item in values["SENTRY_PROJECT_KEYS"].split(","):
        match = PROJECT_KEY.fullmatch(item.strip())
        if not match or match[1] in seen:
            raise ValueError("Use unique project-id:32-character-public-key pairs")
        seen.add(match[1])
        pairs.append(match.groups())
    if len(pairs) > 2:
        raise ValueError("Configure at most two projects, for Android and iOS")
    return values, pairs


def render(values, pairs):
    maps = []
    locations = []
    for project, key in pairs:
        maps.append(f'''    map $http_x_sentry_auth $header_{project} {{
        default 0;
        "~^Sentry\\s+(?:.*[, ]\\s*)?sentry_key={key}(?:[, ]|$)" 1;
    }}
    map $arg_sentry_key $query_{project} {{
        default 0;
        "{key}" 1;
    }}
    map "$header_{project}:$query_{project}" $allowed_{project} {{
        default 0;
        "1:0" 1;
        "0:1" 1;
        "1:1" 1;
    }}''')
        locations.append(f'''        location = /api/{project}/envelope/ {{
            if ($request_method != POST) {{ return 405; }}
            if ($allowed_{project} = 0) {{ return 403; }}
            limit_req zone=events burst=100 nodelay;
            limit_conn uploads 8;
            proxy_pass http://relay:3000;
            proxy_http_version 1.1;
            proxy_set_header Connection "";
            proxy_set_header Host {values["PUBLIC_HOST"]};
            proxy_set_header X-Sentry-Auth $http_x_sentry_auth;
            proxy_set_header X-Forwarded-For "";
            proxy_set_header X-Real-IP "";
            proxy_set_header Forwarded "";
            proxy_set_header Cookie "";
            proxy_set_header Authorization "";
            proxy_request_buffering off;
            proxy_buffering off;
            proxy_connect_timeout 5s;
            proxy_read_timeout 60s;
            proxy_send_timeout 60s;
        }}''')
    nginx = '''worker_processes 1;
pid /tmp/nginx.pid;
error_log /dev/stderr crit;
events { worker_connections 256; }
http {
    access_log off;
    client_body_temp_path /tmp/client_body;
    proxy_temp_path /tmp/proxy;
    fastcgi_temp_path /tmp/fastcgi;
    uwsgi_temp_path /tmp/uwsgi;
    scgi_temp_path /tmp/scgi;
    client_max_body_size 200m;
    client_body_timeout 30s;
    client_header_timeout 10s;
    send_timeout 30s;
    keepalive_timeout 10s;
    server_tokens off;
    limit_req_zone $server_name zone=events:1m rate=20r/s;
    limit_conn_zone $server_name zone=uploads:1m;
    limit_req_status 429;
    limit_conn_status 429;
''' + "\n".join(maps) + '''
    server {
        listen 8080;
        server_name _;
        location = /healthz {
            proxy_pass http://relay:3000/api/relay/healthcheck/ready/;
        }
''' + "\n".join(locations) + '''
        location / { return 404; }
    }
}
'''
    relay = f'''relay:
  mode: proxy
  upstream: https://{values["SENTRY_UPSTREAM_HOST"]}/
  host: 0.0.0.0
  port: 3000
limits:
  max_thread_count: 2
  max_concurrent_requests: 8
  max_envelope_size: 209715200
  shutdown_timeout: 30
cache:
  envelope_buffer_size: 128
health:
  max_memory_bytes: 1610612736
logging:
  level: warn
'''
    return nginx, relay


def render_caddy(values, pairs):
    paths = " ".join(f"/api/{project}/envelope/" for project, _ in pairs)
    return f'''{values["PUBLIC_HOST"]} {{
    @sentry path {paths} /healthz
    handle @sentry {{
        request_body {{
            max_size 209715200
        }}
        reverse_proxy vlr-sentry-ingress:8080 {{
            transport http {{
                dial_timeout 5s
                response_header_timeout 60s
            }}
        }}
    }}
    handle {{
        respond "Not found" 404
    }}
}}
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--env", type=Path, default=Path(__file__).parent / ".env")
    parser.add_argument("--output", type=Path, default=Path(__file__).parent / ".generated")
    args = parser.parse_args()
    try:
        values, pairs = read_config(args.env)
        nginx, relay = render(values, pairs)
    except (ValueError, OSError) as error:
        parser.exit(1, f"Configuration failed: {error}\n")
    os.chmod(args.env, 0o600)
    args.output.mkdir(mode=0o755, parents=True, exist_ok=True)
    (args.output / "relay").mkdir(mode=0o755, exist_ok=True)
    for path, content in [(args.output / "nginx.conf", nginx), (args.output / "relay/config.yml", relay)]:
        path.write_text(content)
        path.chmod(0o644)
    (args.output / "Caddyfile.fragment").write_text(render_caddy(values, pairs))
    print("Validated configuration written. No network requests or containers started.")


if __name__ == "__main__":
    main()
