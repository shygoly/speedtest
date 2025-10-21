import asyncio
import json
import socket
import time
from typing import Optional, Tuple

import websockets
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from websockets import WebSocketServerProtocol
from websockets.exceptions import ConnectionClosed

import threading

TCP_HOST = "0.0.0.0"
TCP_PORT = 8080
UDP_PORT = 10002
HTTP_HOST = "0.0.0.0"
HTTP_PORT = 12345

INITIAL_SPEED_MEGABITS = 200
SENDING_TIME_SECONDS = 0.1

HANDSHAKE_TIMEOUT_SECONDS = 3
MESSAGE_TIMEOUT_SECONDS = 2
UDP_TRIGGER_TIMEOUT_SECONDS = 3

PING_INTERVAL_SECONDS = 15
PING_TIMEOUT_SECONDS = 30

PAYLOAD_SIZE = 1024  # bytes per UDP packet
UDP_PAYLOAD = b"X" * PAYLOAD_SIZE


def generate_udp_traffic(sock: socket.socket, addr: Tuple[str, int], speed_mbps: int) -> int:
    """Send UDP payloads at approximately the requested Mbps for SENDING_TIME_SECONDS."""
    start = time.time()
    bytes_sent = 0
    target_bytes_per_second = speed_mbps * 1024 * 1024 / 8

    while (elapsed := time.time() - start) < SENDING_TIME_SECONDS:
        desired_bytes = target_bytes_per_second * elapsed
        if bytes_sent < desired_bytes:
            bytes_sent += sock.sendto(UDP_PAYLOAD, addr)
        else:
            time.sleep(0.001)
    return bytes_sent


async def await_udp_trigger(loop: asyncio.AbstractEventLoop) -> Tuple[socket.socket, Tuple[str, int]]:
    """Bind to UDP_PORT and wait for a trigger packet to determine the client address."""
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((TCP_HOST, UDP_PORT))
    sock.settimeout(UDP_TRIGGER_TIMEOUT_SECONDS)
    print(f"[udp] Waiting for trigger on {TCP_HOST}:{UDP_PORT}")

    try:
        addr = await loop.run_in_executor(None, sock.recvfrom, 1024)
    except socket.timeout as exc:
        sock.close()
        raise TimeoutError("No UDP trigger received") from exc

    # recvfrom returns (data, addr)
    _, udp_addr = addr
    print(f"[udp] Trigger received from {udp_addr}")
    return sock, udp_addr


async def expect_json(ws: WebSocketServerProtocol, timeout: float) -> dict:
    """Receive a JSON message from the client with a timeout."""
    try:
        raw = await asyncio.wait_for(ws.recv(), timeout=timeout)
    except asyncio.TimeoutError as exc:
        raise TimeoutError("WebSocket receive timeout") from exc

    if isinstance(raw, bytes):
        raw = raw.decode("utf-8", errors="ignore")

    try:
        return json.loads(raw)
    except json.JSONDecodeError as exc:
        raise ValueError(f"Invalid JSON payload: {raw!r}") from exc


async def run_echo_mode(
    ws: WebSocketServerProtocol,
    client: str,
    first_message: Optional[object],
) -> None:
    """Mirror all incoming frames back to the client (binary or text)."""
    if first_message is not None:
        await ws.send(first_message)
        print(f"[ws] Echoed initial frame for {client}; size={len(first_message) if isinstance(first_message, (bytes, bytearray)) else len(str(first_message))} bytes")

    try:
        async for message in ws:
            await ws.send(message)
            frame_type = "binary" if isinstance(message, (bytes, bytearray)) else "text"
            print(f"[ws] Echoed {frame_type} frame for {client}; size={len(message) if isinstance(message, (bytes, bytearray)) else len(str(message))} bytes")
    except ConnectionClosed as exc:
        print(f"[ws] Echo mode connection closed for {client}: {exc.code} {exc.reason or ''}")
    finally:
        print(f"[ws] Echo mode finished for {client}")


async def run_udp_speedtest(
    ws: WebSocketServerProtocol,
    client: str,
    *,
    announce_udp_port: bool = False,
) -> None:
    """Execute the classic UDP-driven speed test protocol."""
    udp_sock: Optional[socket.socket] = None
    try:
        if announce_udp_port:
            await ws.send(json.dumps({"udp_port": UDP_PORT}))
        loop = asyncio.get_running_loop()
        udp_sock, udp_addr = await await_udp_trigger(loop)
    except TimeoutError as exc:
        await ws.send(json.dumps({"error": str(exc)}))
        print(f"[ws] {client} {exc}")
        return

    if announce_udp_port:
        await ws.send(json.dumps({"udp_port": UDP_PORT}))

    total_bytes_sent = 0
    speed = INITIAL_SPEED_MEGABITS

    try:
        while True:
            await ws.send(json.dumps({"msg": "start", "speed": speed}))
            ack = await expect_json(ws, MESSAGE_TIMEOUT_SECONDS)
            if ack.get("msg") != "start":
                print(f"[ws] Unexpected ACK from {client}: {ack}")
                break

            try:
                batch_bytes = await asyncio.to_thread(generate_udp_traffic, udp_sock, udp_addr, speed)
            except Exception as exc:
                await ws.send(json.dumps({"error": f"UDP send failed: {exc}"}))
                print(f"[ws] UDP error for {client}: {exc!r}")
                return

            total_bytes_sent += batch_bytes
            instruction = await expect_json(ws, MESSAGE_TIMEOUT_SECONDS)
            msg = instruction.get("msg")
            if msg == "finish":
                break
            if msg == "increase":
                speed *= 2
                continue
            if msg == "repeat":
                continue

            print(f"[ws] Unknown instruction {instruction} from {client}, ending session")
            break

        await ws.send(json.dumps({"traffic": total_bytes_sent}))
        print(f"[ws] Session complete for {client}, bytes_sent={total_bytes_sent}")
    except (TimeoutError, ValueError) as exc:
        await ws.send(json.dumps({"error": str(exc)}))
        print(f"[ws] Error for {client}: {exc}")
    finally:
        if udp_sock:
            udp_sock.close()


async def handle_client(ws: WebSocketServerProtocol, path: Optional[str] = None) -> None:
    client = f"{ws.remote_address[0]}:{ws.remote_address[1]}" if ws.remote_address else "unknown"
    print(f"[ws] Client connected: {client} path={path}")

    try:
        first = await asyncio.wait_for(ws.recv(), timeout=HANDSHAKE_TIMEOUT_SECONDS)
    except asyncio.TimeoutError:
        print(f"[ws] Client {client} did not send initial data in time")
        return
    except ConnectionClosed as exc:
        print(f"[ws] Connection closed before handshake for {client}: {exc.code} {exc.reason or ''}")
        return

    try:
        if isinstance(first, (bytes, bytearray)):
            await run_echo_mode(ws, client, bytes(first))
            return

        # Attempt to parse JSON handshake.
        hello = json.loads(first)
    except json.JSONDecodeError:
        await run_echo_mode(ws, client, first)
        return

    try:
        if isinstance(hello, dict) and hello.get("msg") == "hello":
            await ws.send(json.dumps({"msg": "hello there"}))
            await run_udp_speedtest(ws, client)
        else:
            # Legacy client: expect UDP port announcement before trigger.
            await run_udp_speedtest(ws, client, announce_udp_port=True)
    except ConnectionClosed as exc:
        print(f"[ws] Connection closed for {client}: {exc.code} {exc.reason or ''}")
    except Exception as exc:
        print(f"[ws] Unexpected error for {client}: {exc!r}")
    finally:
        print(f"[ws] Client disconnected: {client}")


async def run_server() -> None:
    server = await websockets.serve(
        handle_client,
        TCP_HOST,
        TCP_PORT,
        ping_interval=PING_INTERVAL_SECONDS,
        ping_timeout=PING_TIMEOUT_SECONDS,
        max_size=None,
        max_queue=None,
    )
    print(f"[ws] WebSocket server listening on {TCP_HOST}:{TCP_PORT}")
    await server.wait_closed()

class LegacyControllerHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length) if length else b""
        response = None
        status = 200

        if self.path == "/speedtest/new":
            payload = {
                "BPS": "139.180.223.171",
                "testID": str(int(time.time()))
            }
            response = json.dumps(payload).encode()
        elif self.path == "/speedtest/record":
            response = json.dumps({"status": "ok"}).encode()
        elif self.path == "/feedback/new":
            response = json.dumps({"status": "received"}).encode()
        else:
            status = 404
            response = json.dumps({"error": "not found"}).encode()

        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(response)))
        self.end_headers()
        self.wfile.write(response)

    def log_message(self, format, *args):
        # Suppress default stdout logging.
        return


def start_legacy_controller():
    server = ThreadingHTTPServer((HTTP_HOST, HTTP_PORT), LegacyControllerHandler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    print(f"[http] Legacy controller listening on {HTTP_HOST}:{HTTP_PORT}")
    return server


def main() -> None:
    start_legacy_controller()
    try:
        asyncio.run(run_server())
    except KeyboardInterrupt:
        print("[ws] Shutdown requested via KeyboardInterrupt")


if __name__ == "__main__":
    main()
