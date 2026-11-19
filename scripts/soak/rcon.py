"""Minimal Source-RCON client for the soak harness (2.8.0). No dependencies.

    from rcon import Rcon
    with Rcon("127.0.0.1", 25575, "soak") as r:
        print(r.cmd("forge tps"))
"""
import socket
import struct


class Rcon:
    LOGIN, COMMAND = 3, 2

    def __init__(self, host="127.0.0.1", port=25575, password="soak", timeout=10.0):
        self.host, self.port, self.password, self.timeout = host, port, password, timeout
        self.sock = None
        self.req = 0

    def __enter__(self):
        self.connect()
        return self

    def __exit__(self, *exc):
        self.close()

    def connect(self):
        self.sock = socket.create_connection((self.host, self.port), timeout=self.timeout)
        rid, _ = self._send(self.LOGIN, self.password)
        if rid == -1:
            raise PermissionError("RCON login refused (wrong password?)")

    def close(self):
        if self.sock:
            self.sock.close()
            self.sock = None

    def _recv_exact(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError("RCON connection closed")
            buf += chunk
        return buf

    def _send(self, kind, payload):
        self.req += 1
        body = struct.pack("<ii", self.req, kind) + payload.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(body)) + body)
        length = struct.unpack("<i", self._recv_exact(4))[0]
        data = self._recv_exact(length)
        rid, _ = struct.unpack("<ii", data[:8])
        return rid, data[8:-2].decode("utf-8", "replace")

    def cmd(self, command):
        return self._send(self.COMMAND, command.lstrip("/"))[1]
