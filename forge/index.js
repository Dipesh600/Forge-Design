/**
 * FORGE — Firebase Cloud Functions (Gen 2)
 * Codebase: forge
 *
 * ⚠️  SECURITY: The MiniMax API key NEVER leaves this server.
 *     The Android app calls these endpoints; it can never read the key.
 *
 * Functions:
 *   minimax_proxy     — authenticated chat completion proxy for MiniMax API
 *   stitch_mcp_proxy  — Stitch MCP server-side proxy for screen generation
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const https = require("https");

// Store the MiniMax API key as a Firebase Secret (set via: firebase functions:secrets:set MINIMAX_API_KEY)
const minimaxApiKey = defineSecret("MINIMAX_API_KEY");

// ─────────────────────────────────────────────────────────────────────────────
// minimax_proxy
// ─────────────────────────────────────────────────────────────────────────────
exports.minimax_proxy = onCall(
  {
    secrets: [minimaxApiKey],
    region: "us-central1",
    timeoutSeconds: 60,
    memory: "256MiB",
  },
  async (request) => {
    // 1. Auth gate — only signed-in Firebase users can call this
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "You must be signed in to use FORGE AI."
      );
    }

    // 2. Validate payload
    const { messages, model } = request.data;
    if (!messages || !Array.isArray(messages) || messages.length === 0) {
      throw new HttpsError(
        "invalid-argument",
        "messages must be a non-empty array."
      );
    }

    // 3. Build MiniMax request body
    const body = JSON.stringify({
      model: model || "MiniMax-Text-01",
      messages: messages,
      temperature: 0.7,
      max_tokens: 2048,
    });

    // 4. Forward to MiniMax API using the secret key
    const apiKey = minimaxApiKey.value();
    const responseData = await callMiniMaxApi(body, apiKey);

    return responseData;
  }
);

/**
 * Makes an HTTPS request to the MiniMax API.
 * Using Node's built-in https to avoid needing extra dependencies.
 */
function callMiniMaxApi(body, apiKey) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "api.minimax.io",
      path: "/v1/text/chatcompletion_v2",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${apiKey}`,
        "Content-Length": Buffer.byteLength(body),
      },
    };

    const req = https.request(options, (res) => {
      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => {
        try {
          const parsed = JSON.parse(data);
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(parsed);
          } else {
            reject(
              new HttpsError(
                "internal",
                `MiniMax API error ${res.statusCode}: ${parsed.message || data}`
              )
            );
          }
        } catch (e) {
          reject(new HttpsError("internal", "Failed to parse MiniMax response."));
        }
      });
    });

    req.on("error", (err) => {
      reject(new HttpsError("internal", `Network error: ${err.message}`));
    });

    req.write(body);
    req.end();
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// minimax_embedding
// ─────────────────────────────────────────────────────────────────────────────
exports.minimax_embedding = onCall(
  {
    secrets: [minimaxApiKey],
    region: "us-central1",
    timeoutSeconds: 60,
    memory: "256MiB",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "You must be signed in to use FORGE AI.");
    }

    const { texts } = request.data;
    if (!texts || !Array.isArray(texts) || texts.length === 0) {
      throw new HttpsError("invalid-argument", "texts must be a non-empty array.");
    }

    const body = JSON.stringify({
      texts: texts,
      model: "embo-01",
      type: "query"
    });

    const apiKey = minimaxApiKey.value();
    
    return new Promise((resolve, reject) => {
      const options = {
        hostname: "api.minimax.io",
        path: "/v1/embeddings",
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${apiKey}`,
          "Content-Length": Buffer.byteLength(body),
        },
      };

      const req = https.request(options, (res) => {
        let data = "";
        res.on("data", (chunk) => (data += chunk));
        res.on("end", () => {
          try {
            const parsed = JSON.parse(data);
            if (res.statusCode >= 200 && res.statusCode < 300) {
              resolve(parsed);
            } else {
              reject(new HttpsError("internal", `MiniMax API error ${res.statusCode}: ${parsed.message || data}`));
            }
          } catch (e) {
            reject(new HttpsError("internal", "Failed to parse MiniMax response."));
          }
        });
      });

      req.on("error", (err) => {
        reject(new HttpsError("internal", `Network error: ${err.message}`));
      });

      req.write(body);
      req.end();
    });
  }
);

// ─────────────────────────────────────────────────────────────────────────────
// Re-export Stitch MCP proxy
// ─────────────────────────────────────────────────────────────────────────────
const { stitch_mcp_proxy } = require("./stitch_mcp_proxy");
exports.stitch_mcp_proxy = stitch_mcp_proxy;
