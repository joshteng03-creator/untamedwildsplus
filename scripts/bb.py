#!/usr/bin/env python3
"""Drive the Blockbench MCP plugin over plain HTTP JSON-RPC.

WHY THIS EXISTS.  Claude Code binds MCP tools at session start, so if Blockbench is
launched after the session begins its tools are simply absent -- `claude mcp list` still
reports "Connected" because that runs a fresh health check in a subprocess, which makes
the failure look like something else entirely.  The plugin is just an HTTP MCP server on
localhost:3000/bb-mcp, so it can be driven directly and the session does not need
restarting.

Usage:
    python scripts/bb.py list                      # tool names + one-line schemas
    python scripts/bb.py call <tool> '<json args>' # invoke a tool
    python scripts/bb.py eval <file.js>            # risky_eval a local JS file
    python scripts/bb.py shot '<json args>'        # capture_screenshot -> PNG on disk

Screenshots come back as base64 image content and are written to
scratch/bb_shot_<n>.png rather than being echoed, so a 200 KB data URL never lands in
the transcript.
"""
import base64
import json
import pathlib
import sys
import urllib.request

URL = "http://localhost:3000/bb-mcp"
SESSION = {"id": None}
SHOT_DIR = pathlib.Path(__file__).resolve().parent.parent / "scratch"


def rpc(method, params=None, notify=False):
    body = {"jsonrpc": "2.0", "method": method}
    if params is not None:
        body["params"] = params
    if not notify:
        body["id"] = 1
    req = urllib.request.Request(
        URL, data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json",
                 "Accept": "application/json, text/event-stream",
                 **({"mcp-session-id": SESSION["id"]} if SESSION["id"] else {})})
    with urllib.request.urlopen(req, timeout=180) as r:
        if SESSION["id"] is None:
            SESSION["id"] = r.headers.get("mcp-session-id")
        raw = r.read().decode()
    if not raw.strip():
        return None
    # the transport may answer as SSE ("event: message\ndata: {...}")
    if raw.lstrip().startswith("event:") or raw.lstrip().startswith("data:"):
        raw = "\n".join(l[5:].strip() for l in raw.splitlines()
                        if l.startswith("data:"))
    return json.loads(raw)


def connect():
    rpc("initialize", {"protocolVersion": "2024-11-05", "capabilities": {},
                       "clientInfo": {"name": "bb.py", "version": "1.0"}})
    rpc("notifications/initialized", notify=True)


def call(tool, args):
    """Invoke a tool; returns (text_parts, image_parts)."""
    res = rpc("tools/call", {"name": tool, "arguments": args})
    if "error" in res:
        raise SystemExit(f"MCP error: {json.dumps(res['error'])[:2000]}")
    content = res.get("result", {}).get("content", [])
    texts = [c.get("text", "") for c in content if c.get("type") == "text"]
    images = [c.get("data", "") for c in content if c.get("type") == "image"]
    if res.get("result", {}).get("isError"):
        raise SystemExit("TOOL ERROR: " + "\n".join(texts)[:4000])
    return texts, images


def save_images(images, tag):
    SHOT_DIR.mkdir(exist_ok=True)
    out = []
    for i, b64 in enumerate(images):
        p = SHOT_DIR / f"bb_{tag}_{i}.png"
        p.write_bytes(base64.b64decode(b64))
        out.append(str(p))
    return out


def main():
    connect()
    cmd = sys.argv[1]

    if cmd == "list":
        res = rpc("tools/list", {})
        for t in res["result"]["tools"]:
            props = list((t.get("inputSchema") or {}).get("properties", {}))
            req = set((t.get("inputSchema") or {}).get("required", []))
            sig = ", ".join(f"{p}*" if p in req else p for p in props)
            print(f"{t['name']}({sig})")
        return

    if cmd == "schema":
        res = rpc("tools/list", {})
        for t in res["result"]["tools"]:
            if t["name"] in sys.argv[2:]:
                print(json.dumps(t, indent=1)[:6000])
        return

    if cmd == "eval":
        src = pathlib.Path(sys.argv[2]).read_text(encoding="utf-8")
        texts, images = call("risky_eval", {"code": src})
        print("\n".join(texts)[:12000])
        for p in save_images(images, "eval"):
            print("IMAGE ->", p)
        return

    if cmd in ("call", "shot"):
        tool = "capture_screenshot" if cmd == "shot" else sys.argv[2]
        args = json.loads(sys.argv[3] if cmd == "call" else sys.argv[2])
        texts, images = call(tool, args)
        body = "\n".join(texts)
        print(body[:12000] if body else "(no text)")
        for p in save_images(images, tool):
            print("IMAGE ->", p)
        return

    raise SystemExit(__doc__)


if __name__ == "__main__":
    main()
