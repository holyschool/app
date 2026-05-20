import "dotenv/config";
import fs from "fs";
import path from "path";
import express from "express";
import Database from "better-sqlite3";
import admin from "firebase-admin";
import fetch from "node-fetch";

const {
  FCM_PROJECT_ID,
  FCM_SERVICE_ACCOUNT,
  FCM_TOPIC = "edupage_all",
  SERVER_API_KEY,
  DATABASE_PATH = "./data/edupage-notify.sqlite",
  POLL_INTERVAL_SECONDS = "60",
  DEBUG_LOG = "false",
  FIRST_RUN_NOTIFY = "false",
  SEED_GRADES_ON_START = "true",
  MESSAGE_PREVIEW_CHARS = "140",
  PORT = "4580",
} = process.env;

const REQUIRED = ["FCM_PROJECT_ID", "FCM_SERVICE_ACCOUNT", "SERVER_API_KEY"];
const missing = REQUIRED.filter((k) => !process.env[k]);
if (missing.length) {
  console.error(`Missing env vars: ${missing.join(", ")}`);
  process.exit(1);
}

const debug = DEBUG_LOG === "true";
const firstRunNotify = FIRST_RUN_NOTIFY === "true";
const seedGradesOnStart = SEED_GRADES_ON_START !== "false";
const previewCharsRaw = Number(MESSAGE_PREVIEW_CHARS);
const messagePreviewChars = Number.isFinite(previewCharsRaw) ? Math.max(20, previewCharsRaw) : 140;

const serviceAccount = JSON.parse(fs.readFileSync(FCM_SERVICE_ACCOUNT, "utf8"));
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: FCM_PROJECT_ID,
});

let sessionCookie = null;
let gsecHash = null;
let lastTimelineId = -1;
let lastGradeIds = new Set();

function log(...args) {
  if (debug) console.log("[debug]", ...args);
}

const IMPORTANT_MESSAGE_PREFIXES = [
  "D\u00f4le\u017eit\u00e1 spr\u00e1va",
  "D\u00f4le\u017eit\u00e1 sprava",
];

function stringOrNull(value) {
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function safeJsonParse(value) {
  try {
    return JSON.parse(value);
  } catch (e) {
    return null;
  }
}

function parseItemData(raw) {
  if (!raw) return null;
  if (typeof raw === "string") {
    const trimmed = raw.trim();
    if (!trimmed || trimmed === "[]") return null;
    return safeJsonParse(trimmed);
  }
  if (typeof raw === "object") return raw;
  return null;
}

function startsWithImportant(text) {
  return IMPORTANT_MESSAGE_PREFIXES.some((prefix) => text.startsWith(prefix));
}

function extractMessageContent(data) {
  if (!data || typeof data !== "object") return null;
  return stringOrNull(data.messageContent);
}

function buildHomeworkText(data) {
  if (!data || typeof data !== "object") return null;
  const name = stringOrNull(data.nazov);
  const due = stringOrNull(data.date);
  if (name && due) return `${name} (due ${due})`;
  if (name) return name;
  if (due) return `Due ${due}`;
  return null;
}

function stripHtml(value) {
  return value ? value.replace(/<[^>]*>/g, "") : "";
}

function buildPreview(text, limit) {
  const cleaned = stripHtml(text).trim();
  if (!cleaned) return "";
  const capped = Math.max(20, limit);
  if (cleaned.length <= capped) return cleaned;
  return `${cleaned.slice(0, capped - 3)}...`;
}

function formatGrade(value) {
  if (value === null || value === undefined || value === "") return "New grade";
  if (typeof value === "number") {
    return Number.isInteger(value) ? String(value) : String(value);
  }
  return String(value);
}

function extractGsecHash(html) {
  if (!html) return null;
  const patterns = [
    /ASC\.gsechash\s*=\s*"([^"]+)"/,
    /ASC\.gsechash\s*:\s*"([^"]+)"/,
    /gsechash\s*[:=]\s*"([^"]+)"/,
    /gsechash\s*[:=]\s*'([^']+)'/,
  ];
  for (const pattern of patterns) {
    const match = pattern.exec(html);
    if (match?.[1]) return match[1];
  }
  return null;
}

function parseUserHomeData(html) {
  const dataMatch = html.split("userhome(")[1]?.split(");")[0];
  if (!dataMatch) return null;
  try {
    return JSON.parse(dataMatch.replace(/\t|\n|\r/g, ""));
  } catch (e) {
    return null;
  }
}

function toDataPayload(data) {
  if (!data) return {};
  const result = {};
  for (const [key, value] of Object.entries(data)) {
    if (value === undefined || value === null) continue;
    result[key] = String(value);
  }
  return result;
}

function ensureDir(dirPath) {
  if (!fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, { recursive: true });
  }
}

ensureDir(path.dirname(DATABASE_PATH));
const db = new Database(DATABASE_PATH);
db.pragma("journal_mode = WAL");

db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    subdomain TEXT NOT NULL,
    username TEXT NOT NULL,
    password TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
  );
  CREATE UNIQUE INDEX IF NOT EXISTS idx_users_unique ON users(subdomain, username);

  CREATE TABLE IF NOT EXISTS user_state (
    user_id INTEGER PRIMARY KEY,
    last_timeline_id INTEGER NOT NULL DEFAULT -1,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
  );

  CREATE TABLE IF NOT EXISTS grade_state (
    user_id INTEGER NOT NULL,
    grade_event_id INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    PRIMARY KEY(user_id, grade_event_id),
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
  );

  CREATE TABLE IF NOT EXISTS message_read_state (
    user_id INTEGER NOT NULL,
    timeline_id INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    PRIMARY KEY(user_id, timeline_id),
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
  );
  CREATE INDEX IF NOT EXISTS idx_message_read_user ON message_read_state(user_id);

  CREATE TABLE IF NOT EXISTS devices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    fcm_token TEXT NOT NULL,
    platform TEXT,
    app_version TEXT,
    enabled INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
  );
  CREATE UNIQUE INDEX IF NOT EXISTS idx_devices_unique ON devices(user_id, fcm_token);
`);

const now = () => Date.now();

const queries = {
  upsertUser: db.prepare(`
    INSERT INTO users (subdomain, username, password, enabled, created_at, updated_at)
    VALUES (@subdomain, @username, @password, 1, @created_at, @updated_at)
    ON CONFLICT(subdomain, username)
    DO UPDATE SET password = excluded.password, updated_at = excluded.updated_at
  `),
  getUserById: db.prepare("SELECT * FROM users WHERE id = ?"),
  getUserByCreds: db.prepare("SELECT * FROM users WHERE subdomain = ? AND username = ?"),
  listEnabledUsers: db.prepare("SELECT * FROM users WHERE enabled = 1"),
  upsertDevice: db.prepare(`
    INSERT INTO devices (user_id, fcm_token, platform, app_version, enabled, created_at, updated_at)
    VALUES (@user_id, @fcm_token, @platform, @app_version, 1, @created_at, @updated_at)
    ON CONFLICT(user_id, fcm_token)
    DO UPDATE SET enabled = 1, platform = excluded.platform, app_version = excluded.app_version, updated_at = excluded.updated_at
  `),
  listEnabledDevices: db.prepare("SELECT * FROM devices WHERE user_id = ? AND enabled = 1"),
  getUserState: db.prepare("SELECT * FROM user_state WHERE user_id = ?"),
  upsertUserState: db.prepare(`
    INSERT INTO user_state (user_id, last_timeline_id, updated_at)
    VALUES (@user_id, @last_timeline_id, @updated_at)
    ON CONFLICT(user_id)
    DO UPDATE SET last_timeline_id = excluded.last_timeline_id, updated_at = excluded.updated_at
  `),
  insertGradeState: db.prepare(`
    INSERT OR IGNORE INTO grade_state (user_id, grade_event_id, created_at)
    VALUES (@user_id, @grade_event_id, @created_at)
  `),
  listGradeState: db.prepare("SELECT grade_event_id FROM grade_state WHERE user_id = ?"),
  insertMessageRead: db.prepare(`
    INSERT OR IGNORE INTO message_read_state (user_id, timeline_id, created_at)
    VALUES (@user_id, @timeline_id, @created_at)
  `),
  listMessageRead: db.prepare(`
    SELECT timeline_id FROM message_read_state
    WHERE user_id = ?
    ORDER BY timeline_id DESC
    LIMIT ?
  `),
  trimMessageRead: db.prepare(`
    DELETE FROM message_read_state
    WHERE user_id = ? AND timeline_id NOT IN (
      SELECT timeline_id FROM message_read_state
      WHERE user_id = ?
      ORDER BY timeline_id DESC
      LIMIT ?
    )
  `),
  deleteDevice: db.prepare("UPDATE devices SET enabled = 0, updated_at = ? WHERE id = ?"),
  disableUser: db.prepare("UPDATE users SET enabled = 0, updated_at = ? WHERE id = ?"),
};

async function login({ subdomain, username, password }) {
  const loginPageUrl = `https://${subdomain}.edupage.org/login/?cmd=MainLogin`;
  const loginPageRes = await fetch(loginPageUrl, { method: "GET" });
  const loginPageBody = await loginPageRes.text();
  const csrfToken = loginPageBody.split('"csrftoken":"')[1]?.split('"')[0];
  if (!csrfToken) throw new Error("Failed to extract CSRF token");

  const loginUrl = `https://${subdomain}.edupage.org/login/edubarLogin.php`;
  const form = new URLSearchParams();
  form.set("csrfauth", csrfToken);
  form.set("username", username);
  form.set("password", password);

  const loginRes = await fetch(loginUrl, {
    method: "POST",
    body: form,
    redirect: "manual",
  });

  const location = loginRes.headers.get("location") || "";

  const setCookie = loginRes.headers.get("set-cookie") || "";
  const sessionMatch = /PHPSESSID=([^;]+)/.exec(setCookie);
  if (!sessionMatch) throw new Error("Failed to extract PHPSESSID");
  sessionCookie = sessionMatch[1];

  const body = await loginRes.text();
  if (
    location.includes("bad=1") ||
    body.includes("bad_username") ||
    body.includes("wrong_password") ||
    body.includes("bad=1")
  ) {
    throw new Error("Bad credentials");
  }
  if (
    location.includes("cap=1") ||
    body.toLowerCase().includes("captcha") ||
    body.toLowerCase().includes("lerr=b43b43")
  ) {
    throw new Error("Captcha required");
  }
  if (location.includes("twofactor") || body.toLowerCase().includes("twofactor")) {
    throw new Error("Two-factor required");
  }

  gsecHash = extractGsecHash(body) || null;
  log("login ok", subdomain, username);
}

async function ensureSession({ subdomain, username, password }) {
  if (sessionCookie && gsecHash) return;
  await login({ subdomain, username, password });
}

async function fetchUserPage({ subdomain, username, password }) {
  await ensureSession({ subdomain, username, password });
  const url = `https://${subdomain}.edupage.org/user`;
  const res = await fetch(url, {
    headers: {
      Cookie: `PHPSESSID=${sessionCookie}`,
    },
  });
  return await res.text();
}

async function fetchTimeline({ subdomain, username, password }) {
  const html = await fetchUserPage({ subdomain, username, password });
  if (!gsecHash) {
    gsecHash = extractGsecHash(html) || null;
  }
  const data = parseUserHomeData(html);
  if (!data) throw new Error("Failed to extract session data");
  return data?.items || [];
}

function parseTimeline(items) {
  return items
    .map((item) => {
      if (!item || typeof item !== "object") return null;
      const timelineId = Number(item.timelineid);
      if (Number.isNaN(timelineId)) return null;
      const type = stringOrNull(item.typ);
      const authorName = stringOrNull(item.vlastnik_meno);
      const title = stringOrNull(item.titulok);
      const data = parseItemData(item.data);

      let text = stringOrNull(item.text);
      if (text && startsWithImportant(text)) {
        const messageContent = extractMessageContent(data);
        if (messageContent) text = messageContent;
      }

      if (!text && (type === "homework" || type === "hw")) {
        text = buildHomeworkText(data);
      }

      return {
        timelineId,
        type,
        authorName,
        title,
        text,
      };
    })
    .filter(Boolean);
}

async function fetchGrades({ subdomain, username, password }) {
  await ensureSession({ subdomain, username, password });
  if (!gsecHash) {
    const html = await fetchUserPage({ subdomain, username, password });
    gsecHash = extractGsecHash(html) || null;
  }
  if (!gsecHash) throw new Error("Failed to extract gsecHash");
  const url = `https://${subdomain}.edupage.org/grades/server/grades.js?__func=reload`;
  const payload = {
    __args: [null, { from: 0, to: 0 }],
    __gsh: gsecHash,
  };
  const res = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Cookie: `PHPSESSID=${sessionCookie}`,
    },
    body: JSON.stringify(payload),
  });
  const raw = await res.text();
  if (!raw.trim()) {
    console.warn("Grades response empty");
    return [];
  }
  let data = null;
  try {
    data = JSON.parse(raw);
  } catch (e) {
    const snippet = raw.slice(0, 200).replace(/\s+/g, " ");
    throw new Error(`Grades response not JSON: ${snippet}`);
  }
  const grades = data?.r?.grades || [];
  return grades.map((g) => ({
    eventId: Number(g.eventid),
    subject: g.subjectname,
    grade: g.grade_n ?? g.grade,
  })).filter((g) => !Number.isNaN(g.eventId));
}

async function sendTopicNotification({ title, body, data, topic, token }) {
  const payload = {
    notification: { title, body },
  };
  if (token) {
    payload.token = token;
  } else {
    payload.topic = topic || FCM_TOPIC;
  }
  const dataPayload = toDataPayload(data);
  if (Object.keys(dataPayload).length) {
    payload.data = dataPayload;
  }
  await admin.messaging().send(payload);
}

async function notifyMessages(events, { topic, token }) {
  if (!events.length) return;
  const sorted = [...events].sort((a, b) => b.timelineId - a.timelineId);
  if (sorted.length === 1) {
    const msg = sorted[0];
    const sender = msg.authorName || "New message";
    const preview = buildPreview(msg.text || msg.title || "", messagePreviewChars);
    await sendTopicNotification({
      title: `Message from ${sender}`,
      body: preview || "You have a new message",
      data: { type: "message", timelineId: msg.timelineId, sender },
      topic,
      token,
    });
    return;
  }
  const latest = sorted[0];
  const preview = buildPreview(latest.text || latest.title || "", messagePreviewChars);
  await sendTopicNotification({
    title: `${sorted.length} new messages`,
    body: preview || "Open Edupage to view them",
    data: { type: "message", count: sorted.length, timelineId: latest.timelineId },
    topic,
    token,
  });
}

async function notifySubstitutions(events, { topic, token }) {
  if (!events.length) return;
  const sorted = [...events].sort((a, b) => b.timelineId - a.timelineId);
  if (sorted.length === 1) {
    const ev = sorted[0];
    const title = ev.title || "Substitution update";
    const body = buildPreview(ev.text || "", messagePreviewChars) || "New substitution";
    await sendTopicNotification({
      title,
      body,
      data: { type: "substitution", timelineId: ev.timelineId },
      topic,
      token,
    });
    return;
  }
  const latest = sorted[0];
  const body = buildPreview(latest.text || latest.title || "", messagePreviewChars);
  await sendTopicNotification({
    title: `${sorted.length} substitution updates`,
    body: body || "Open Edupage to view them",
    data: { type: "substitution", count: sorted.length, timelineId: latest.timelineId },
    topic,
    token,
  });
}

async function notifyGrades(grades, { topic, token }) {
  if (!grades.length) return;
  const sorted = [...grades].sort((a, b) => b.eventId - a.eventId);
  if (sorted.length === 1) {
    const g = sorted[0];
    const gradeText = formatGrade(g.grade);
    const subject = g.subject || "Subject";
    await sendTopicNotification({
      title: "New grade",
      body: `${gradeText} in ${subject}`,
      data: { type: "grade", eventId: g.eventId, subject, grade: gradeText },
      topic,
      token,
    });
    return;
  }
  const latest = sorted[0];
  const gradeText = formatGrade(latest.grade);
  const subject = latest.subject || "Subject";
  await sendTopicNotification({
    title: `${sorted.length} new grades`,
    body: `Latest: ${gradeText} in ${subject}`,
    data: { type: "grade", count: sorted.length, eventId: latest.eventId, subject, grade: gradeText },
    topic,
    token,
  });
}

async function seedInitialState(user, maxTimelineId) {
  queries.upsertUserState.run({
    user_id: user.id,
    last_timeline_id: maxTimelineId,
    updated_at: now(),
  });
  if (!seedGradesOnStart) return;
  try {
    const grades = await fetchGrades(user);
    grades.forEach((g) => {
      queries.insertGradeState.run({
        user_id: user.id,
        grade_event_id: g.eventId,
        created_at: now(),
      });
    });
  } catch (e) {
    console.warn("Failed to seed grade ids for user", user.id, e.message);
  }
}

async function pollUser(user) {
  try {
    sessionCookie = null;
    gsecHash = null;
    const items = await fetchTimeline(user);
    const events = parseTimeline(items);
    if (!events.length) return;

    const maxId = Math.max(...events.map((e) => e.timelineId));
    const minId = Math.min(...events.map((e) => e.timelineId));
    const userState = queries.getUserState.get(user.id);
    let effectiveLastTimelineId = userState?.last_timeline_id ?? -1;

    if (effectiveLastTimelineId === -1) {
      if (!firstRunNotify) {
        log("first run: seeding timeline watermark at", maxId, "user", user.id);
        await seedInitialState(user, maxId);
        return;
      }
      effectiveLastTimelineId = minId - 1;
      log("first run: notifying for existing timeline events");
    }

    const newEvents = events.filter((e) => e.timelineId > effectiveLastTimelineId);
    if (!newEvents.length) {
      queries.upsertUserState.run({
        user_id: user.id,
        last_timeline_id: maxId,
        updated_at: now(),
      });
      return;
    }

    const messageEvents = newEvents.filter((e) => e.type === "sprava");
    const gradeEvents = newEvents.filter((e) => e.type === "znamka" || e.type === "znamkydoc");
    const substitutionEvents = newEvents.filter((e) => e.type === "suplovanie");

    const devices = queries.listEnabledDevices.all(user.id);
    const tokenTargets = devices.length ? devices.map((d) => d.fcm_token) : [];

    const sendToUser = async (builder) => {
      if (!tokenTargets.length) {
        await builder(null);
        return;
      }
      for (const token of tokenTargets) {
        await builder(token);
      }
    };

    if (messageEvents.length) {
      await sendToUser((token) => notifyMessages(messageEvents, { token }));
    }

    if (substitutionEvents.length) {
      await sendToUser((token) => notifySubstitutions(substitutionEvents, { token }));
    }

    if (gradeEvents.length) {
      const grades = await fetchGrades(user);
      const notifiedIds = new Set(
        queries.listGradeState.all(user.id).map((row) => row.grade_event_id)
      );
      const newGrades = grades.filter((g) => !notifiedIds.has(g.eventId));
      if (newGrades.length) {
        await sendToUser((token) => notifyGrades(newGrades, { token }));
        newGrades.forEach((g) => {
          queries.insertGradeState.run({
            user_id: user.id,
            grade_event_id: g.eventId,
            created_at: now(),
          });
        });
      }
    }

    queries.upsertUserState.run({
      user_id: user.id,
      last_timeline_id: maxId,
      updated_at: now(),
    });
  } catch (e) {
    console.error("poll failed for user", user.id, e.message);
    sessionCookie = null;
    gsecHash = null;
  }
}

async function start() {
  console.log("Edupage notify backend starting...");

  const maxReadIds = 2000;

  const app = express();
  app.use(express.json({ limit: "200kb" }));

  app.use((req, res, next) => {
    const authHeader = req.headers.authorization || "";
    const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : null;
    if (token !== SERVER_API_KEY) {
      return res.status(401).json({ error: "Unauthorized" });
    }
    next();
  });

  app.post("/api/register", async (req, res) => {
    const { subdomain, username, password, fcmToken, platform, appVersion } = req.body || {};
    if (!subdomain || !username || !password || !fcmToken) {
      return res.status(400).json({ error: "Missing fields" });
    }

    try {
      try {
        await login({
          subdomain: String(subdomain).trim(),
          username: String(username).trim(),
          password: String(password),
        });
      } catch (e) {
        const msg = String(e.message || "Login failed");
        if (msg.includes("Two-factor")) return res.status(409).json({ error: "Two-factor required" });
        if (msg.includes("Captcha")) return res.status(409).json({ error: "Captcha required" });
        if (msg.includes("Bad credentials")) return res.status(401).json({ error: "Invalid credentials" });
        return res.status(401).json({ error: "Login failed" });
      }

      const existing = queries.getUserByCreds.get(String(subdomain).trim(), String(username).trim());
      if (existing && existing.password !== String(password)) {
        return res.status(401).json({ error: "Invalid credentials" });
      }
      sessionCookie = null;
      gsecHash = null;
      const userParams = {
        subdomain: String(subdomain).trim(),
        username: String(username).trim(),
        password: String(password),
        created_at: now(),
        updated_at: now(),
      };
      const info = queries.upsertUser.run(userParams);
      const user = queries.getUserByCreds.get(userParams.subdomain, userParams.username);
      queries.upsertDevice.run({
        user_id: user.id,
        fcm_token: String(fcmToken),
        platform: platform ? String(platform) : null,
        app_version: appVersion ? String(appVersion) : null,
        created_at: now(),
        updated_at: now(),
      });
      res.json({ userId: user.id, ok: true });
    } catch (e) {
      res.status(500).json({ error: "Failed to register" });
    }
  });

  app.post("/api/unregister", (req, res) => {
    const { userId, fcmToken, subdomain, username, password } = req.body || {};
    if (!fcmToken) return res.status(400).json({ error: "Missing fields" });
    const user = userId ? queries.getUserById.get(userId)
      : queries.getUserByCreds.get(String(subdomain || "").trim(), String(username || "").trim());
    if (!user) return res.status(404).json({ error: "User not found" });
    if (!userId && password && user.password !== String(password)) {
      return res.status(401).json({ error: "Invalid credentials" });
    }
    const devices = queries.listEnabledDevices.all(user.id);
    const target = devices.find((d) => d.fcm_token === fcmToken);
    if (!target) return res.status(404).json({ error: "Device not found" });
    queries.deleteDevice.run(now(), target.id);
    res.json({ ok: true });
  });

  app.post("/api/messages/read", (req, res) => {
    const { userId, subdomain, username, password, limit } = req.body || {};
    const user = userId
      ? queries.getUserById.get(userId)
      : queries.getUserByCreds.get(String(subdomain || "").trim(), String(username || "").trim());
    if (!user) return res.status(404).json({ error: "User not found" });
    if (!userId && password && user.password !== String(password)) {
      return res.status(401).json({ error: "Invalid credentials" });
    }
    const effectiveLimit = Number.isFinite(Number(limit))
      ? Math.max(1, Math.min(maxReadIds, Number(limit)))
      : 1000;
    const rows = queries.listMessageRead.all(user.id, effectiveLimit);
    const ids = rows.map((row) => row.timeline_id);
    res.json({ ids });
  });

  app.post("/api/messages/mark-read", (req, res) => {
    const { userId, subdomain, username, password, ids } = req.body || {};
    const user = userId
      ? queries.getUserById.get(userId)
      : queries.getUserByCreds.get(String(subdomain || "").trim(), String(username || "").trim());
    if (!user) return res.status(404).json({ error: "User not found" });
    if (!userId && password && user.password !== String(password)) {
      return res.status(401).json({ error: "Invalid credentials" });
    }
    const parsedIds = Array.isArray(ids)
      ? ids.map((id) => Number(id)).filter((id) => Number.isFinite(id))
      : [];
    if (!parsedIds.length) return res.status(400).json({ error: "Missing ids" });

    const insert = queries.insertMessageRead;
    const nowTs = now();
    for (const id of parsedIds) {
      insert.run({ user_id: user.id, timeline_id: id, created_at: nowTs });
    }
    queries.trimMessageRead.run(user.id, user.id, maxReadIds);
    res.json({ ok: true, count: parsedIds.length });
  });

  app.post("/api/messages/sync", (req, res) => {
    const { userId, subdomain, username, password, ids, limit } = req.body || {};
    const user = userId
      ? queries.getUserById.get(userId)
      : queries.getUserByCreds.get(String(subdomain || "").trim(), String(username || "").trim());
    if (!user) return res.status(404).json({ error: "User not found" });
    if (!userId && password && user.password !== String(password)) {
      return res.status(401).json({ error: "Invalid credentials" });
    }

    const parsedIds = Array.isArray(ids)
      ? ids.map((id) => Number(id)).filter((id) => Number.isFinite(id))
      : [];
    if (parsedIds.length) {
      const insert = queries.insertMessageRead;
      const nowTs = now();
      for (const id of parsedIds) {
        insert.run({ user_id: user.id, timeline_id: id, created_at: nowTs });
      }
      queries.trimMessageRead.run(user.id, user.id, maxReadIds);
    }

    const effectiveLimit = Number.isFinite(Number(limit))
      ? Math.max(1, Math.min(maxReadIds, Number(limit)))
      : 1000;
    const rows = queries.listMessageRead.all(user.id, effectiveLimit);
    const readIds = rows.map((row) => row.timeline_id);
    res.json({ ids: readIds, ok: true });
  });

  app.post("/api/disable-user", (req, res) => {
    const { userId, subdomain, username, password } = req.body || {};
    const user = userId ? queries.getUserById.get(userId)
      : queries.getUserByCreds.get(String(subdomain || "").trim(), String(username || "").trim());
    if (!user) return res.status(404).json({ error: "User not found" });
    if (!userId && password && user.password !== String(password)) {
      return res.status(401).json({ error: "Invalid credentials" });
    }
    queries.disableUser.run(now(), user.id);
    res.json({ ok: true });
  });

  app.get("/api/health", (_req, res) => {
    res.json({ ok: true });
  });

  app.listen(Number(PORT), () => {
    console.log(`API listening on ${PORT}`);
  });

  const users = queries.listEnabledUsers.all();
  if (!users.length) {
    console.log("No users registered yet.");
  }
  for (const user of users) {
    await pollUser(user);
  }
  const intervalMs = Math.max(15, Number(POLL_INTERVAL_SECONDS)) * 1000;
  setInterval(async () => {
    const activeUsers = queries.listEnabledUsers.all();
    for (const user of activeUsers) {
      await pollUser(user);
    }
  }, intervalMs);
}

start();
