/**
 * FORGE — stitch_mcp_proxy Cloud Function (Gen 2)
 *
 * Correct Stitch REST API protocol (from stitch-mcp npm package source):
 *  - POST ONE JSON-RPC object per call to https://stitch.googleapis.com/mcp
 *  - method: "tools/call", params: { name, arguments }
 *  - NO initialize handshake — the REST endpoint is stateless
 *  - Response is a single JSON object (not NDJSON)
 *  - result.content[0].text is a JSON string with the actual Stitch data
 *
 * create_project result: { "name": "projects/P/stitchProjects/ID", "title": "..." }
 * generate_screen result: { outputComponents: [{ design: { screens: [{ id, screenshot, htmlCode }] } }] }
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { GoogleAuth } = require("google-auth-library");
const https = require("https");

const auth = new GoogleAuth({
  scopes: ["https://www.googleapis.com/auth/cloud-platform"],
});

exports.stitch_mcp_proxy = onCall(
  {
    region: "us-central1",
    timeoutSeconds: 120,
    memory: "512MiB",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "You must be signed in to use FORGE AI.");
    }

    const { tool, prompt, projectId, deviceType, selectedScreenIds, screenId, title } = request.data;
    if (!tool) throw new HttpsError("invalid-argument", "tool is required");

    try {
      const accessToken = await getAccessToken();
      const gcpProject = process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT;
      if (!gcpProject) throw new HttpsError("internal", "GOOGLE_CLOUD_PROJECT env var not set");

      console.log(`[FORGE] tool=${tool}, project=${gcpProject}`);

      switch (tool) {

        // ── Create a new Stitch project ──────────────────────────────────────
        case "create_project": {
          if (!title) throw new HttpsError("invalid-argument", "title is required");

          const rpc = await callStitch(accessToken, gcpProject, "tools/call", {
            name: "create_project",
            arguments: { title },
          });

          console.log("[FORGE] create_project raw:", JSON.stringify(rpc).slice(0, 600));
          const text = stitchText(rpc);
          console.log("[FORGE] create_project text:", text ? text.slice(0, 400) : "NONE");

          let pid = null;
          if (text) {
            try {
              const data = JSON.parse(text);
              console.log("[FORGE] create_project data keys:", Object.keys(data));

              // Stitch returns { "name": "projects/P/stitchProjects/ID" }
              const resourceName = data.name || data.projectId || data.id || data.stitchProjectId;
              if (resourceName) {
                pid = resourceName.includes("/") ? resourceName.split("/").pop() : resourceName;
              }
              if (!pid) pid = deepFind(data, ["projectId", "stitchProjectId", "id"]);
            } catch {
              const m = text.match(/"name"\s*:\s*"([^"]+)"/);
              if (m) pid = m[1].includes("/") ? m[1].split("/").pop() : m[1];
            }
          }

          console.log("[FORGE] create_project pid:", pid || "NONE");
          return {
            projectId:     pid || null,
            title,
            screenId:      null,
            htmlUrl:       null,
            screenshotUrl: null,
            description:   null,
            suggestions:   [],
            error: pid ? null : `No projectId in Stitch response. Raw: ${text ? text.slice(0, 200) : "empty"}`,
          };
        }

        // ── Generate a new screen ────────────────────────────────────────────
        case "generate_screen_from_text": {
          if (!prompt)    throw new HttpsError("invalid-argument", "prompt is required");
          if (!projectId) throw new HttpsError("invalid-argument", "projectId is required");

          const rpc = await callStitch(accessToken, gcpProject, "tools/call", {
            name: "generate_screen_from_text",
            arguments: { projectId, prompt, deviceType: deviceType || "MOBILE" },
          });

          const text = stitchText(rpc);
          console.log("[FORGE] gen_screen text:", text ? text.slice(0, 400) : "NONE");
          return parseScreenText(text, projectId);
        }

        // ── Edit screens ─────────────────────────────────────────────────────
        case "edit_screens": {
          if (!projectId || !selectedScreenIds?.length || !prompt) {
            throw new HttpsError("invalid-argument", "projectId, selectedScreenIds[], prompt required");
          }
          const rpc = await callStitch(accessToken, gcpProject, "tools/call", {
            name: "edit_screens",
            arguments: { projectId, selectedScreenIds, prompt },
          });
          return parseScreenText(stitchText(rpc), projectId);
        }

        // ── List screens ─────────────────────────────────────────────────────
        case "list_screens": {
          if (!projectId) throw new HttpsError("invalid-argument", "projectId is required");
          const rpc = await callStitch(accessToken, gcpProject, "tools/call", {
            name: "list_screens",
            arguments: { projectId },
          });
          const text = stitchText(rpc);
          let screens = [];
          try { screens = JSON.parse(text || "{}").screens || []; } catch { /* ignore */ }
          return { screens };
        }

        // ── Get screen ───────────────────────────────────────────────────────
        case "get_screen": {
          if (!projectId || !screenId) throw new HttpsError("invalid-argument", "projectId and screenId required");
          const rpc = await callStitch(accessToken, gcpProject, "tools/call", {
            name: "get_screen",
            arguments: { projectId, screenId },
          });
          return parseScreenText(stitchText(rpc), projectId);
        }

        default:
          throw new HttpsError("invalid-argument", `Unknown tool: ${tool}`);
      }

    } catch (e) {
      console.error("[FORGE] stitch_mcp_proxy error:", e.message || e);
      if (e instanceof HttpsError) throw e;
      throw new HttpsError("internal", `Stitch error: ${e.message}`);
    }
  }
);

// ── Stitch REST API ───────────────────────────────────────────────────────────
// Single POST per call — mirrors stitch-mcp/index.js callStitchAPI() exactly.

async function callStitch(token, gcpProject, method, params) {
  const body = JSON.stringify({
    jsonrpc: "2.0",
    id: Date.now(),
    method,
    params,
  });

  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: "stitch.googleapis.com",
      path: "/mcp",
      method: "POST",
      headers: {
        "Authorization": `Bearer ${token}`,
        "X-Goog-User-Project": gcpProject,
        "Content-Type": "application/json",
        "Content-Length": Buffer.byteLength(body),
      },
    }, (res) => {
      let data = "";
      res.on("data", chunk => (data += chunk));
      res.on("end", () => {
        console.log(`[FORGE] Stitch HTTP ${res.statusCode}, len=${data.length}`);
        console.log(`[FORGE] Stitch body: ${data.slice(0, 800)}`);

        if (res.statusCode >= 400) {
          reject(new Error(`Stitch HTTP ${res.statusCode}: ${data.slice(0, 300)}`));
          return;
        }
        try {
          resolve(JSON.parse(data));
        } catch {
          reject(new Error(`Stitch JSON parse failed: ${data.slice(0, 200)}`));
        }
      });
    });
    req.on("error", err => reject(new Error(`Network: ${err.message}`)));
    req.write(body);
    req.end();
  });
}

// ── Auth ──────────────────────────────────────────────────────────────────────

async function getAccessToken() {
  const client = await auth.getClient();
  const tokenResponse = await client.getAccessToken();
  if (!tokenResponse.token) throw new Error("Failed to obtain service account access token");
  return tokenResponse.token;
}

// ── Response parsers ──────────────────────────────────────────────────────────

function stitchText(rpc) {
  if (!rpc) return null;
  if (rpc.error) {
    console.error("[FORGE] RPC error:", JSON.stringify(rpc.error));
    return null;
  }
  return rpc?.result?.content?.[0]?.text || null;
}

function parseScreenText(text, projectId) {
  if (!text) {
    return { screenId: `sc_${Date.now()}`, projectId, htmlUrl: null, screenshotUrl: null, description: null, suggestions: [], error: "empty Stitch response" };
  }
  try {
    const data = JSON.parse(text);

    // outputComponents[].design.screens[] — confirmed format from stitch-mcp package
    if (Array.isArray(data.outputComponents)) {
      for (const comp of data.outputComponents) {
        const screens = comp?.design?.screens;
        if (screens?.length > 0) {
          const s = screens[0];
          return {
            screenId:      s.id || s.screenId || `sc_${Date.now()}`,
            projectId,
            htmlUrl:       s.htmlCode?.downloadUrl || s.htmlUrl || null,
            screenshotUrl: s.screenshot?.downloadUrl || s.screenshotUrl || null,
            description:   s.title || s.description || null,
            suggestions:   data.suggestions || [],
            error:         null,
          };
        }
      }
    }

    const htmlUrl = data.htmlUrl || data.htmlCode?.downloadUrl || null;
    const screenshotUrl = data.screenshotUrl || data.screenshot?.downloadUrl || null;
    return {
      screenId:      data.id || data.screenId || `sc_${Date.now()}`,
      projectId:     data.projectId || projectId,
      htmlUrl, screenshotUrl,
      description:   data.title || data.description || null,
      suggestions:   data.suggestions || [],
      error:         (htmlUrl || screenshotUrl) ? null : "no screen URLs in Stitch response",
    };
  } catch {
    return { screenId: `sc_${Date.now()}`, projectId, htmlUrl: null, screenshotUrl: null, description: text.slice(0, 200), suggestions: [], error: "non-JSON Stitch response" };
  }
}

function deepFind(obj, fields) {
  if (!obj || typeof obj !== "object") return null;
  for (const f of fields) if (obj[f]) return obj[f];
  for (const key of Object.keys(obj)) {
    const found = deepFind(obj[key], fields);
    if (found) return found;
  }
  return null;
}
