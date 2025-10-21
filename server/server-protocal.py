import asyncio
import json
import socket
import time
from typing import Optional, Tuple

import websockets
from websockets import WebSocketServerProtocol
from websockets.exceptions import ConnectionClosed

TCP_HOST = "0.0.0.0"
TCP_PORT = 10001
UDP_PORT = 10002

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

    try:
        addr = await loop.run_in_executor(None, sock.recvfrom, 1024)
    except socket.timeout as exc:
        sock.close()
        raise TimeoutError("No UDP trigger received") from exc

    # recvfrom returns (data, addr)
    _, udp_addr = addr
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


async def handle_client(ws: WebSocketServerProtocol, path: str) -> None:
    client = f"{ws.remote_address[0]}:{ws.remote_address[1]}" if ws.remote_address else "unknown"
    print(f"[ws] Client connected: {client} path={path}")

    udp_sock: Optional[socket.socket] = None

    try:
        hello = await expect_json(ws, HANDSHAKE_TIMEOUT_SECONDS)
        if hello.get("msg") != "hello":
            await ws.send(json.dumps({"error": "unexpected handshake payload"}))
            print(f"[ws] Invalid hello from {client}: {hello}")
            return
        await ws.send(json.dumps({"msg": "hello there"}))

        loop = asyncio.get_running_loop()
        try:
            udp_sock, udp_addr = await await_udp_trigger(loop)
        except TimeoutError as exc:
            await ws.send(json.dumps({"error": str(exc)}))
            print(f"[ws] {client} {exc}")
            return

        total_bytes_sent = 0
        speed = INITIAL_SPEED_MEGABITS

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
    except ConnectionClosed as exc:
        print(f"[ws] Connection closed for {client}: {exc.code} {exc.reason or ''}")
    except Exception as exc:
        print(f"[ws] Unexpected error for {client}: {exc!r}")
    finally:
        if udp_sock:
            udp_sock.close()
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


def main() -> None:
    try:
        asyncio.run(run_server())
    except KeyboardInterrupt:
        print("[ws] Shutdown requested via KeyboardInterrupt")


if __name__ == "__main__":
    main()
