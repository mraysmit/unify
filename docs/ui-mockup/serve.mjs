import http from "node:http";
import { readFile, realpath } from "node:fs/promises";
import { dirname, extname, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

const root = await realpath(dirname(fileURLToPath(import.meta.url)));
const port = Number(process.env.UNIFY_PREVIEW_PORT || 4321);
const types = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".md": "text/plain; charset=utf-8",
};
const publicFiles = new Set(["index.html", "data.js", "README.md"]);

export function createPreviewServer() {
  return http.createServer(async (request, response) => {
    try {
      if (!["GET", "HEAD"].includes(request.method)) {
        response.writeHead(405, { Allow: "GET, HEAD" });
        response.end();
        return;
      }
      const path = decodeURIComponent(
        new URL(request.url, "http://127.0.0.1").pathname,
      );
      const name = path === "/" ? "index.html" : path.slice(1);
      if (!publicFiles.has(name)) {
        response.writeHead(404);
        response.end("Not found");
        return;
      }
      const file = await realpath(resolve(root, name));
      if (!file.startsWith(root + sep)) {
        response.writeHead(404);
        response.end("Not found");
        return;
      }
      const content = await readFile(file);
      response.writeHead(200, {
        "Content-Type": types[extname(file)] || "application/octet-stream",
        "Cache-Control": "no-store",
        "X-Content-Type-Options": "nosniff",
      });
      response.end(request.method === "HEAD" ? undefined : content);
    } catch {
      response.writeHead(404);
      response.end("Not found");
    }
  });
}

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  const server = createPreviewServer();
  server.on("error", (error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
  server.listen(port, "127.0.0.1", () =>
    console.log(
      `Unify local prototype: http://127.0.0.1:${server.address().port}\nPress Ctrl+C to stop.`,
    ),
  );
}
