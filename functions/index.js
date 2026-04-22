/**
 * FORGE — Cloud Functions (default codebase)
 *
 * minimax_proxy   — MiniMax AI chat proxy (api.minimax.io)
 * stitch_mcp_proxy — Stitch MCP tool proxy
 *
 * CORRECT Stitch API protocol (from stitch-mcp package source, index.js:83-130):
 *   - POST one JSON-RPC object per call to https://stitch.googleapis.com/mcp
 *   - method: "tools/call", params: { name, arguments }
 *   - NO initialize handshake needed — the REST endpoint is stateless
 *   - NO NDJSON batching — response.json(), not streaming
 *   - Response: { id, result: { content: [{ type: "text", text: "..." }] } }
 *   - The "text" field is a JSON string containing the actual Stitch result
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { GoogleAuth } = require("google-auth-library");
const https = require("https");

const MINIMAX_API_KEY = defineSecret("MINIMAX_API_KEY");

const auth = new GoogleAuth({
  scopes: ["https://www.googleapis.com/auth/cloud-platform"],
});

// ── MiniMax AI Proxy ──────────────────────────────────────────────────────────

exports.minimax_proxy = onCall(
  { secrets: [MINIMAX_API_KEY], region: "us-central1", timeoutSeconds: 120, memory: "256MiB" },
  async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Auth required.");

    const { messages, model, tools, tool_choice } = request.data;

    // Use OpenAI-compatible endpoint — required for MiniMax-M2.7 native tool calling
    // Also supports MiniMax-Text-01 for backward compat
    const bodyObj = {
      model: model || "MiniMax-Text-01",
      messages,
    };
    // Only include tools + tool_choice when tools are provided
    if (tools && tools.length > 0) {
      bodyObj.tools = tools;
      bodyObj.tool_choice = tool_choice || "auto";
    }

    const body = JSON.stringify(bodyObj);

    return new Promise((resolve, reject) => {
      const req = https.request({
        hostname: "api.minimax.io",
        path: "/v1/chat/completions",   // OpenAI-compatible — works with M2.7 tool calling
        method: "POST",
        headers: {
          "Authorization": `Bearer ${MINIMAX_API_KEY.value()}`,
          "Content-Type": "application/json",
          "Content-Length": Buffer.byteLength(body),
        },
      }, (res) => {
        let data = "";
        res.on("data", chunk => (data += chunk));
        res.on("end", () => {
          console.log(`[FORGE] MiniMax HTTP ${res.statusCode}, model=${model}, len=${data.length}`);
          try { resolve(JSON.parse(data)); }
          catch { reject(new HttpsError("internal", "MiniMax parse error")); }
        });
      });
      req.on("error", (e) => reject(new HttpsError("internal", e.message)));
      req.write(body);
      req.end();
    });
  }
);

// ── Stitch MCP Proxy ──────────────────────────────────────────────────────────

exports.stitch_mcp_proxy = onCall(
  { region: "us-central1", timeoutSeconds: 120, memory: "512MiB" },
  async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Auth required.");

    const { tool, title, prompt, projectId, deviceType, selectedScreenIds, screenId } = request.data;
    if (!tool) throw new HttpsError("invalid-argument", "tool is required");

    try {
      const accessToken = await getStitchToken();
      const gcpProject = process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT;
      if (!gcpProject) throw new HttpsError("internal", "GOOGLE_CLOUD_PROJECT not set");

      console.log(`[FORGE] tool=${tool}, project=${gcpProject}`);

      switch (tool) {

        case "create_project": {
          if (!title) throw new HttpsError("invalid-argument", "title required");

          // Single POST — no initialize handshake needed for the REST endpoint
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
              // Stitch returns: { "name": "projects/P/stitchProjects/ID", "title": "..." }
              // Project ID = last segment of the resource path
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
            projectId: pid || null,
            title,
            screenId: null, htmlUrl: null, screenshotUrl: null,
            suggestions: [],
            error: pid ? null : `No projectId in Stitch response. Raw: ${text ? text.slice(0, 200) : "empty"}`,
          };
        }

        case "generate_screen_from_text": {
          if (!prompt)    throw new HttpsError("invalid-argument", "prompt required");
          if (!projectId) throw new HttpsError("invalid-argument", "projectId required");

          const rpc = await callStitch(accessToken, gcpProject, "tools/call", {
            name: "generate_screen_from_text",
            arguments: { projectId, prompt, deviceType: deviceType || "MOBILE" },
          });

          const text = stitchText(rpc);
          console.log("[FORGE] gen_screen text:", text ? text.slice(0, 400) : "NONE");
          return parseScreenText(text, projectId);
        }

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

        case "list_screens": {
          if (!projectId) throw new HttpsError("invalid-argument", "projectId required");
          const rpc = await callStitch(accessToken, gcpProject, "tools/call", {
            name: "list_screens",
            arguments: { projectId },
          });
          const text = stitchText(rpc);
          let screens = [];
          try { screens = JSON.parse(text || "{}").screens || []; } catch { /**/ }
          return { screens };
        }

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
      console.error("[FORGE] stitch_mcp_proxy error:", e.message);
      if (e instanceof HttpsError) throw e;
      throw new HttpsError("internal", `Stitch error: ${e.message}`);
    }
  }
);

// ── Stitch REST API call ──────────────────────────────────────────────────────
// One POST per call, returns the parsed JSON response object.
// Mirrors stitch-mcp/index.js callStitchAPI() exactly.

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
        // Always log full response for create_project (short), first 500 for others
        console.log(`[FORGE] Stitch response: ${data.slice(0, 800)}`);

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

async function getStitchToken() {
  const client = await auth.getClient();
  const { token } = await client.getAccessToken();
  if (!token) throw new Error("Failed to get service account token");
  return token;
}

// ── Response parsers ──────────────────────────────────────────────────────────

/**
 * Extract text from a Stitch JSON-RPC response.
 * Format: { result: { content: [{ type: "text", text: "..." }] } }
 * The text field is itself a JSON string with the actual Stitch data.
 */
function stitchText(rpc) {
  if (!rpc) return null;
  if (rpc.error) {
    console.error("[FORGE] RPC error:", JSON.stringify(rpc.error));
    return null;
  }
  return rpc?.result?.content?.[0]?.text || null;
}

/**
 * Parse screen data from generate_screen_from_text / edit_screens / get_screen.
 * Stitch returns outputComponents[].design.screens[] with screenshot & htmlCode URLs.
 */
function parseScreenText(text, projectId) {
  if (!text) {
    return { screenId: `sc_${Date.now()}`, projectId, htmlUrl: null, screenshotUrl: null, description: null, suggestions: [], error: "empty Stitch response" };
  }

  try {
    const data = JSON.parse(text);

    // Method 1: outputComponents[].design.screens[] (confirmed format from stitch-mcp source)
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

    // Method 2: direct fields
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
