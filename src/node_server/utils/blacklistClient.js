const net = require("net");

function checkUrlBlacklist(url) {
  return new Promise((resolve, reject) => {
    // Connect to the C++ TCP server (exercise 2)
    const client = net.createConnection({ host: "cpp_server", port: 5555 }, () => {
      // Send blacklist check command
      client.write(`2 ${url}\n`);
    });

    client.setEncoding("utf8");

    let buffer = "";

    // Collect incoming data (could arrive in chunks)
    client.on("data", (data) => {
      buffer += data;
    });

    // Once connection ends, process the full response
    client.on("end", () => {
      const lines = buffer.trim().split("\n");

      // Expecting "200 OK" as the first line
      if (lines[0] !== "200 OK") {
        return reject(new Error(`Unexpected status from blacklist server: ${lines[0]}`));
      }

      // Second line contains result like "true true" or "false"
      const result = lines[1] || "";
      const parts = result.trim().split(" ");

      // Only "true true" means the URL is blacklisted
      const isBlacklisted = parts[0] === "true" && parts[1] === "true";
      resolve(isBlacklisted);
    });

    // Handle connection errors
    client.on("error", (err) => {
      reject(err);
    });
  });
}

module.exports = {
  checkUrlBlacklist,
};
