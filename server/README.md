# Speedtest Backend

This backend exposes the legacy controller and websocket services required by the Android client. It replaces the previous Fly.io deployment; deploy it on your own infrastructure (e.g. Vultr).

## Running locally

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python server.py
```

## Docker

```
docker build -t swiftest-backend server
# map TCP/8080 and UDP/10002
```

## Endpoints

- `POST /speedtest/new` returns controller connection info
- WebSocket `ws://<host>:8080`
- UDP traffic on `<host>:10002`

Remove obsolete Fly.io configuration; customise controller IPs via `server.py` if required.
