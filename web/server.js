const express = require("express");
const path = require("path");

const app = express();
const PORT = process.env.PORT || 3000;
const API_KEY = process.env.RADAR_API_KEY;        // shared secret with the plugin
const VIEW_PASSWORD = process.env.VIEW_PASSWORD;  // optional: protects the radar page
const STALE_MS = 10000;

if (!API_KEY) {
  console.error("RADAR_API_KEY env var is required");
  process.exit(1);
}

let state = { players: [], updatedAt: 0, server: "Minecraft Server" };

app.use(express.json({ limit: "256kb" }));

// Plugin -> website
app.post("/api/update", (req, res) => {
  if (req.get("x-api-key") !== API_KEY) return res.status(401).json({ error: "bad key" });
  const body = req.body || {};
  const players = Array.isArray(body.players) ? body.players : [];
  state = {
    server: String(body.server || "Minecraft Server").slice(0, 64),
    updatedAt: Date.now(),
    players: players.slice(0, 500).map(p => ({
      name: String(p.name).slice(0, 32),
      uuid: String(p.uuid || ""),
      world: String(p.world || "world"),
      dim: String(p.dim || "overworld"),
      x: Math.round(Number(p.x) || 0),
      y: Math.round(Number(p.y) || 0),
      z: Math.round(Number(p.z) || 0),
      yaw: Math.round(Number(p.yaw) || 0),
    })),
  };
  res.json({ ok: true });
});

// Optional viewer password (HTTP basic auth, any username)
function viewAuth(req, res, next) {
  if (!VIEW_PASSWORD) return next();
  const h = req.get("authorization") || "";
  const pass = Buffer.from(h.split(" ")[1] || "", "base64").toString().split(":").slice(1).join(":");
  if (pass === VIEW_PASSWORD) return next();
  res.set("WWW-Authenticate", 'Basic realm="Radar"').status(401).send("Password required");
}

app.get("/api/players", viewAuth, (req, res) => {
  res.set("Cache-Control", "no-store");
  res.json({ ...state, online: Date.now() - state.updatedAt < STALE_MS, age: Date.now() - state.updatedAt });
});

app.get("/healthz", (_, res) => res.send("ok"));
app.use(viewAuth, express.static(path.join(__dirname, "public")));

app.listen(PORT, () => console.log("Radar web listening on " + PORT));
