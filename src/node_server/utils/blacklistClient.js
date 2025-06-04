const net = require("net");

function checkUrlBlacklist(url) {
  return new Promise((resolve, reject) => {
    const client = net.createConnection({ host: "cpp_server", port: 5555 }, () => {
      client.write(`2 ${url}\n`);
    });

    let buffer = "";
    client.setEncoding("utf8");

    client.on("data", (data) => { buffer += data; });
    client.on("end", () => {
      const lines = buffer.trim().split("\n");
      if (lines[0] !== "200 OK") {
        return reject(new Error(`Unexpected status from blacklist server: ${lines[0]}`));
      }

      const parts = (lines[1] || "").trim().split(/\s+/);
      const isBlacklisted = parts[0] === "true" && parts[1] === "true";
      resolve(isBlacklisted);
    });

    client.on("error", reject);
  });
}

function addUrlToBlacklist(url) {
  return new Promise((resolve, reject) => {
    const client = net.createConnection({ host: "cpp_server", port: 5555 }, () => {
      client.write(`1 ${url}\n`);
    });

    let buffer = "";
    client.setEncoding("utf8");

    client.on("data", (data) => { buffer += data; });
    client.on("end", () => {
      if (buffer.trim() === "201 Created") {
        resolve();
      } else {
        reject(new Error(`Unexpected response: ${buffer.trim()}`));
      }
    });

    client.on("error", reject);
  });
}

function deleteUrlFromBlacklist(url) {
  return new Promise((resolve, reject) => {
    const client = net.createConnection({ host: "cpp_server", port: 5555 }, () => {
      client.write(`DELETE ${url}\n`);
    });

    let buffer = "";
    client.setEncoding("utf8");

    client.on("data", (data) => { buffer += data; });
    client.on("end", () => {
      const response = buffer.trim();
      if (response === "204 No Content") return resolve({ status: 204 });
      if (response === "404 Not Found") return resolve({ status: 404 });
      reject(new Error(`Unexpected response: ${response}`));
    });

    client.on("error", reject);
  });
}

module.exports = {
  checkUrlBlacklist,
  addUrlToBlacklist,
  deleteUrlFromBlacklist,
};
