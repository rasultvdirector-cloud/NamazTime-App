import { appendFileSync, createReadStream, existsSync, readFileSync } from "node:fs";
import { extname, join, resolve } from "node:path";
import { createServer } from "node:http";

const PORT = Number(process.env.PORT || 8787);
const HOST = process.env.HOST || "0.0.0.0";
const rootDir = resolve(process.cwd(), "backend");
const dataDir = join(rootDir, "data");
const sampleAudioPath = resolve(process.cwd(), "android_app/app/src/main/res/raw/azan_short_1.mp3");
const telemetryLogPath = join(dataDir, "telemetry_logs.ndjson");
const feedbackLogPath = join(dataDir, "feedback.ndjson");

const jsonHeaders = {
  "Content-Type": "application/json; charset=utf-8",
  "Access-Control-Allow-Origin": "*",
};

function sendJson(res, statusCode, body) {
  res.writeHead(statusCode, jsonHeaders);
  res.end(JSON.stringify(body));
}

function sendError(res, statusCode, code, message) {
  sendJson(res, statusCode, { error: { code, message } });
}

function loadJson(fileName) {
  return JSON.parse(readFileSync(join(dataDir, fileName), "utf8"));
}

function baseUrl(req) {
  const host = req.headers.host || `127.0.0.1:${PORT}`;
  return `http://${host}`;
}

function healthHandler(res) {
  sendJson(res, 200, { ok: true });
}

function surasHandler(res) {
  sendJson(res, 200, loadJson("audio_suras.json"));
}

function ayahsHandler(req, res, suraNumber) {
  if (suraNumber !== "1") {
    sendError(res, 404, "SURA_NOT_FOUND", "Only Surah 1 is available in the local stub.");
    return;
  }

  const body = loadJson("audio_sura_1_ayahs.json");
  const hostBase = baseUrl(req);
  body.items = body.items.map((item) => ({
    ...item,
    audioUrl: `${hostBase}/v1/quran/audio/files/ayah_stub.mp3?ayah=${encodeURIComponent(item.ayahKey)}`,
  }));
  sendJson(res, 200, body);
}

function audioFileHandler(res) {
  if (!existsSync(sampleAudioPath)) {
    sendError(res, 500, "AUDIO_FILE_MISSING", "Sample audio file was not found.");
    return;
  }

  res.writeHead(200, {
    "Content-Type": "audio/mpeg",
    "Cache-Control": "no-cache",
    "Access-Control-Allow-Origin": "*",
  });
  createReadStream(sampleAudioPath).pipe(res);
}

function telemetryHandler(req, res) {
  let raw = "";
  req.on("data", (chunk) => {
    raw += chunk.toString("utf8");
    if (raw.length > 32_000) {
      req.destroy();
    }
  });
  req.on("end", () => {
    try {
      const parsed = JSON.parse(raw || "{}");
      appendFileSync(telemetryLogPath, `${JSON.stringify(parsed)}\n`, "utf8");
      console.log(
        `[telemetry] ${parsed.timestamp || "-"} event=${parsed.event || "-"} version=${parsed.versionName || "-"} device=${parsed.device || "-"}`
      );
      sendJson(res, 200, { ok: true });
    } catch {
      sendError(res, 400, "BAD_JSON", "Invalid telemetry payload.");
    }
  });
}

function feedbackHandler(req, res) {
  let raw = "";
  req.on("data", (chunk) => {
    raw += chunk.toString("utf8");
    if (raw.length > 32_000) {
      req.destroy();
    }
  });
  req.on("end", () => {
    try {
      const parsed = JSON.parse(raw || "{}");
      appendFileSync(feedbackLogPath, `${JSON.stringify(parsed)}\n`, "utf8");
      console.log(
        `[feedback] ${parsed.timestamp || "-"} version=${parsed.versionName || "-"} device=${parsed.device || "-"} contact=${parsed.contact || "-"}`
      );
      sendJson(res, 200, { ok: true });
    } catch {
      sendError(res, 400, "BAD_JSON", "Invalid feedback payload.");
    }
  });
}

const server = createServer((req, res) => {
  if (!req.url) {
    sendError(res, 400, "BAD_REQUEST", "Missing request URL.");
    return;
  }

  const url = new URL(req.url, `http://${req.headers.host || "127.0.0.1"}`);
  const pathname = url.pathname.replace(/\/+$/, "") || "/";

  if (req.method === "GET" && pathname === "/v1/health") {
    healthHandler(res);
    return;
  }

  if (req.method === "GET" && pathname === "/v1/quran/audio/suras") {
    surasHandler(res);
    return;
  }

  const ayahsMatch = pathname.match(/^\/v1\/quran\/audio\/suras\/(\d+)\/ayahs$/);
  if (req.method === "GET" && ayahsMatch) {
    ayahsHandler(req, res, ayahsMatch[1]);
    return;
  }

  if (req.method === "GET" && pathname === "/v1/quran/audio/files/ayah_stub.mp3") {
    audioFileHandler(res);
    return;
  }

  if (req.method === "POST" && pathname === "/v1/telemetry/logs") {
    telemetryHandler(req, res);
    return;
  }

  if (req.method === "POST" && pathname === "/v1/feedback") {
    feedbackHandler(req, res);
    return;
  }

  if (pathname.startsWith("/v1/")) {
    sendError(res, 404, "NOT_FOUND", "Endpoint not found.");
    return;
  }

  const body = {
    name: "NamazTime Quran Audio Stub",
    endpoints: [
      "/v1/health",
      "/v1/quran/audio/suras",
      "/v1/quran/audio/suras/1/ayahs",
      "/v1/quran/audio/files/ayah_stub.mp3",
      "/v1/telemetry/logs",
      "/v1/feedback",
    ],
  };
  sendJson(res, 200, body);
});

server.listen(PORT, HOST, () => {
  console.log(`NamazTime Quran audio stub running at http://${HOST}:${PORT}`);
});
