"use strict";

/**
 * hoght S3 upload API — dependency-free AWS Lambda.
 *
 * Required env vars:
 *   AWS_S3_BUCKET
 *   AWS_PUBLIC_BASE_URL
 *
 * Optional:
 *   AWS_S3_REGION
 *   FIREBASE_WEB_API_KEY
 */

const crypto = require("crypto");
const https = require("https");

const API_VERSION = "2026-06-20-v4";
const MAX_UPLOAD_BYTES = 50 * 1024 * 1024;

function lambdaRegion() {
  return process.env.AWS_S3_REGION || process.env.AWS_REGION || "us-east-1";
}

function configError(message) {
  const err = new Error(message);
  err.statusCode = 500;
  return err;
}

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Authorization, Content-Type",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Content-Type": "application/json",
    "X-Hoght-Api-Version": API_VERSION,
  };
}

function json(statusCode, body) {
  return {
    statusCode,
    headers: corsHeaders(),
    body: JSON.stringify(body),
  };
}

function encodeRfc3986(value) {
  return encodeURIComponent(String(value)).replace(/[!'()*]/g, (char) =>
    `%${char.charCodeAt(0).toString(16).toUpperCase()}`,
  );
}

function hmac(key, value, encoding) {
  return crypto.createHmac("sha256", key).update(value, "utf8").digest(encoding);
}

function sha256(value) {
  return crypto.createHash("sha256").update(value, "utf8").digest("hex");
}

function signingKey(secretAccessKey, dateStamp, region) {
  const kDate = hmac(`AWS4${secretAccessKey}`, dateStamp);
  const kRegion = hmac(kDate, region);
  const kService = hmac(kRegion, "s3");
  return hmac(kService, "aws4_request");
}

function requireBucket() {
  const bucket = (process.env.AWS_S3_BUCKET || "").trim();
  if (!bucket) throw configError("AWS_S3_BUCKET is not configured.");
  return bucket;
}

function publicUrlForKey(key) {
  let base = (process.env.AWS_PUBLIC_BASE_URL || "").trim().replace(/\/$/, "");
  if (!base) throw configError("AWS_PUBLIC_BASE_URL is not configured.");
  if (!/^https?:\/\//i.test(base)) base = `https://${base}`;
  return `${base}/${key}`;
}

function presignS3PutUrl(key) {
  const bucket = requireBucket();
  const region = lambdaRegion();
  const accessKeyId = process.env.AWS_ACCESS_KEY_ID;
  const secretAccessKey = process.env.AWS_SECRET_ACCESS_KEY;
  const sessionToken = process.env.AWS_SESSION_TOKEN;
  if (!accessKeyId || !secretAccessKey) {
    throw configError("Lambda execution role credentials are missing.");
  }

  const now = new Date();
  const amzDate = now.toISOString().replace(/[:-]|\.\d{3}/g, "");
  const dateStamp = amzDate.slice(0, 8);
  const credentialScope = `${dateStamp}/${region}/s3/aws4_request`;
  const host = `${bucket}.s3.${region}.amazonaws.com`;
  const canonicalUri = `/${key.split("/").map(encodeRfc3986).join("/")}`;

  const params = {
    "X-Amz-Algorithm": "AWS4-HMAC-SHA256",
    "X-Amz-Credential": `${accessKeyId}/${credentialScope}`,
    "X-Amz-Date": amzDate,
    "X-Amz-Expires": "900",
    "X-Amz-SignedHeaders": "host",
  };
  if (sessionToken) params["X-Amz-Security-Token"] = sessionToken;

  const canonicalQuery = Object.keys(params)
    .sort()
    .map((name) => `${encodeRfc3986(name)}=${encodeRfc3986(params[name])}`)
    .join("&");

  const canonicalRequest = [
    "PUT",
    canonicalUri,
    canonicalQuery,
    `host:${host}\n`,
    "host",
    "UNSIGNED-PAYLOAD",
  ].join("\n");

  const stringToSign = ["AWS4-HMAC-SHA256", amzDate, credentialScope, sha256(canonicalRequest)].join("\n");
  const signature = hmac(signingKey(secretAccessKey, dateStamp, region), stringToSign, "hex");
  return `https://${host}${canonicalUri}?${canonicalQuery}&X-Amz-Signature=${signature}`;
}

function parseQuery(event) {
  const fromMap = event.queryStringParameters || {};
  if (fromMap && Object.keys(fromMap).length > 0) return fromMap;

  const raw = event.rawQueryString || "";
  if (!raw) return {};

  const out = {};
  for (const part of raw.split("&")) {
    if (!part) continue;
    const idx = part.indexOf("=");
    const key = decodeURIComponent(idx >= 0 ? part.slice(0, idx) : part);
    const value = decodeURIComponent(idx >= 0 ? part.slice(idx + 1) : "");
    out[key] = value;
  }
  return out;
}

function parseInput(event) {
  const query = parseQuery(event);
  if (event.body == null || event.body === "") return query;

  let raw = event.body;
  if (event.isBase64Encoded && typeof raw === "string") {
    raw = Buffer.from(raw, "base64").toString("utf8");
  }
  if (typeof raw === "object") return { ...query, ...raw };

  try {
    return { ...query, ...JSON.parse(raw) };
  } catch (_) {
    return query;
  }
}

function httpsJson(url, body) {
  return new Promise((resolve, reject) => {
    const payload = JSON.stringify(body);
    const req = https.request(
      url,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Content-Length": Buffer.byteLength(payload),
        },
      },
      (res) => {
        let raw = "";
        res.on("data", (chunk) => {
          raw += chunk;
        });
        res.on("end", () => {
          let parsed = {};
          try {
            parsed = raw ? JSON.parse(raw) : {};
          } catch (_) {
            reject(Object.assign(new Error("Firebase auth response was invalid."), { statusCode: 500 }));
            return;
          }
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(parsed);
            return;
          }
          const message = parsed.error?.message || `Firebase auth failed (${res.statusCode})`;
          reject(Object.assign(new Error(message), { statusCode: 401 }));
        });
      },
    );
    req.on("error", (err) => reject(Object.assign(new Error(`Firebase auth network error: ${err.message}`), { statusCode: 500 })));
    req.write(payload);
    req.end();
  });
}

async function verifyAuth(event) {
  const headers = event.headers || {};
  const auth = headers.authorization || headers.Authorization || "";
  const input = parseQuery(event);
  const token =
    (auth.startsWith("Bearer ") ? auth.slice(7).trim() : "") ||
    String(input.idToken || "").trim();
  if (!token) {
    throw Object.assign(new Error("Sign in required."), { statusCode: 401 });
  }

  const apiKey = (process.env.FIREBASE_WEB_API_KEY || "").trim();
  if (!apiKey) {
    throw Object.assign(new Error("FIREBASE_WEB_API_KEY is not configured."), { statusCode: 500 });
  }
  const result = await httpsJson(
    `https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${apiKey}`,
    { idToken: token },
  );
  const uid = result.users?.[0]?.localId;
  if (!uid) {
    throw Object.assign(new Error("Sign in expired. Sign out and sign in again."), { statusCode: 401 });
  }
  return uid;
}

function buildObjectKey(kind, uid, thoughtId, index) {
  const normalizedKind = String(kind || "").trim().toLowerCase();
  if (normalizedKind === "profile") return `profiles/${uid}/avatar.jpg`;
  if (normalizedKind === "thought") {
    if (!thoughtId || index === undefined || index === null || index === "") {
      throw Object.assign(new Error("thoughtId and index are required."), { statusCode: 400 });
    }
    return `thoughts/${uid}/${thoughtId}/${index}.jpg`;
  }
  throw Object.assign(new Error(`Invalid kind: ${kind || "(missing)"}`), { statusCode: 400 });
}

function handleUploadUrl(uid, input) {
  const { kind, thoughtId, index, contentLength } = input || {};
  const key = buildObjectKey(kind, uid, thoughtId, index);
  const size = Number(contentLength || 0);
  if (Number.isFinite(size) && size > MAX_UPLOAD_BYTES) {
    throw Object.assign(new Error("Each photo must be 50 MB or smaller."), { statusCode: 400 });
  }

  const bucket = requireBucket();
  const uploadUrl = presignS3PutUrl(key);
  return {
    uploadUrl,
    publicUrl: publicUrlForKey(key),
    key,
    bucket,
    region: lambdaRegion(),
  };
}

function awsCredentials() {
  const accessKeyId = process.env.AWS_ACCESS_KEY_ID;
  const secretAccessKey = process.env.AWS_SECRET_ACCESS_KEY;
  const sessionToken = process.env.AWS_SESSION_TOKEN;
  if (!accessKeyId || !secretAccessKey) {
    throw configError("Lambda execution role credentials are missing.");
  }
  return { accessKeyId, secretAccessKey, sessionToken };
}

function canonicalUriForKey(key) {
  if (!key) return "/";
  return `/${key.split("/").map(encodeRfc3986).join("/")}`;
}

function buildSignedRequestHeaders(method, canonicalUri, canonicalQueryString) {
  const { accessKeyId, secretAccessKey, sessionToken } = awsCredentials();
  const region = lambdaRegion();
  const bucket = requireBucket();
  const host = `${bucket}.s3.${region}.amazonaws.com`;
  const now = new Date();
  const amzDate = now.toISOString().replace(/[:-]|\.\d{3}/g, "");
  const dateStamp = amzDate.slice(0, 8);
  const credentialScope = `${dateStamp}/${region}/s3/aws4_request`;
  const payloadHash = sha256("");

  const headerLines = [`host:${host}`, `x-amz-content-sha256:${payloadHash}`, `x-amz-date:${amzDate}`];
  const signedHeaderNames = ["host", "x-amz-content-sha256", "x-amz-date"];
  if (sessionToken) {
    headerLines.push(`x-amz-security-token:${sessionToken}`);
    signedHeaderNames.push("x-amz-security-token");
  }

  const canonicalRequest = [
    method,
    canonicalUri,
    canonicalQueryString,
    `${headerLines.join("\n")}\n`,
    signedHeaderNames.join(";"),
    payloadHash,
  ].join("\n");

  const stringToSign = ["AWS4-HMAC-SHA256", amzDate, credentialScope, sha256(canonicalRequest)].join("\n");
  const signature = hmac(signingKey(secretAccessKey, dateStamp, region), stringToSign, "hex");
  const authorization = `AWS4-HMAC-SHA256 Credential=${accessKeyId}/${credentialScope}, SignedHeaders=${signedHeaderNames.join(";")}, Signature=${signature}`;

  const headers = {
    Host: host,
    "x-amz-date": amzDate,
    "x-amz-content-sha256": payloadHash,
    Authorization: authorization,
  };
  if (sessionToken) headers["x-amz-security-token"] = sessionToken;
  return { host, headers };
}

function httpsS3Request(method, key, queryParams = {}) {
  return new Promise((resolve, reject) => {
    const canonicalUri = key ? canonicalUriForKey(key) : "/";
    const canonicalQuery = Object.keys(queryParams)
      .sort()
      .map((name) => `${encodeRfc3986(name)}=${encodeRfc3986(queryParams[name])}`)
      .join("&");
    const { host, headers } = buildSignedRequestHeaders(method, canonicalUri, canonicalQuery);
    const path = canonicalUri + (canonicalQuery ? `?${canonicalQuery}` : "");
    const req = https.request({ hostname: host, path, method, headers }, (res) => {
      let raw = "";
      res.on("data", (chunk) => {
        raw += chunk;
      });
      res.on("end", () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(raw);
          return;
        }
        reject(
          Object.assign(new Error(`S3 ${method} failed (${res.statusCode})`), {
            statusCode: res.statusCode,
            body: raw,
          }),
        );
      });
    });
    req.on("error", (err) => reject(err));
    req.end();
  });
}

function isAllowedDeletePrefix(uid, prefix) {
  const normalized = prefix.endsWith("/") ? prefix : `${prefix}/`;
  return (
    normalized === `profiles/${uid}/` ||
    normalized.startsWith(`profiles/${uid}/`) ||
    normalized === `thoughts/${uid}/` ||
    normalized.startsWith(`thoughts/${uid}/`)
  );
}

async function listObjectKeys(prefix) {
  const keys = [];
  let continuationToken;
  do {
    const query = { "list-type": "2", prefix, "max-keys": "1000" };
    if (continuationToken) query["continuation-token"] = continuationToken;
    const body = await httpsS3Request("GET", "", query);
    const keyMatches = [...body.matchAll(/<Key>([^<]+)<\/Key>/g)];
    keys.push(...keyMatches.map((match) => match[1]));
    const tokenMatch = body.match(/<NextContinuationToken>([^<]+)<\/NextContinuationToken>/);
    continuationToken = tokenMatch ? tokenMatch[1] : null;
    if (!body.includes("<IsTruncated>true</IsTruncated>")) continuationToken = null;
  } while (continuationToken);
  return keys;
}

async function deleteObjectsWithPrefix(prefix) {
  const keys = await listObjectKeys(prefix);
  for (const key of keys) {
    await httpsS3Request("DELETE", key);
  }
  return keys.length;
}

exports.handler = async (event) => {
  const method = (event.requestContext?.http?.method || event.httpMethod || "GET").toUpperCase();
  const path = event.rawPath || event.path || "";

  if (method === "OPTIONS") {
    return { statusCode: 204, headers: corsHeaders(), body: "" };
  }

  try {
    if (method === "GET" && path.endsWith("/health")) {
      return json(200, { ok: true });
    }

    const uid = await verifyAuth(event);
    const input = parseInput(event);
    console.log("request", { method, path, inputKeys: Object.keys(input) });

    if ((method === "GET" || method === "POST") && path.endsWith("/upload-url")) {
      const result = handleUploadUrl(uid, input);
      console.log("upload-url ok", { uid, key: result.key });
      return json(200, result);
    }

    if ((method === "GET" || method === "POST") && path.endsWith("/delete-prefix")) {
      const prefix = String(input.prefix || "").trim();
      if (!prefix) {
        throw Object.assign(new Error("prefix is required."), { statusCode: 400 });
      }
      if (!isAllowedDeletePrefix(uid, prefix)) {
        throw Object.assign(new Error("You can only delete your own media."), { statusCode: 403 });
      }
      const deletedCount = await deleteObjectsWithPrefix(prefix.endsWith("/") ? prefix : `${prefix}/`);
      console.log("delete-prefix ok", { uid, prefix, deletedCount });
      return json(200, { ok: true, prefix, deletedCount });
    }

    return json(404, { error: "Not found" });
  } catch (err) {
    const status = err.statusCode || 500;
    console.error("hoght-s3-api error", { status, message: err.message, stack: err.stack });
    return json(status, { error: err.message || "Server error" });
  }
};
