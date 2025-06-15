const net = require("net");

function checkUrlBlacklist(url) {
  return new Promise((resolve, reject) => {
    //-----debug-----
    console.log("[BlacklistClient] Trying to connect to TCP server...");
    //-----end of debug-----
    const client = net.createConnection({ host: "blacklist-server", port: 5555 }, () => {
      client.write(`GET ${url}\n`);
    });

    let buffer = "";
    client.setEncoding("utf8");

    client.on("data", (data) => { 
      //-----debug-----
      console.log("[BlacklistClient] Received data:", data);
      //-----end of debug-----
      buffer += data; 
      //-----debug-----
      console.log("[BlacklistClient] Buffer:", buffer);
      //-----end of debug-----



      // let's try that:
       //-----debug-----
      console.log("[BlacklistClient] Connection ended, buffer:", buffer);
      //-----end of debug-----
      const lines = buffer.trim().split("\n");
      //-----debug-----
      console.log("[BlacklistClient] line 0: ", lines[0]);
      console.log("[BlacklistClient] line 1: ", lines[1]);
      //-----end of debug-----
      if (lines[0] !== "200 OK") {
        return reject(new Error(`Unexpected status from blacklist server: ${lines[0]}`));
      }
      //-----debug-----
      console.log("[BlacklistClient] Status OK, processing response...");
      //-----end of debug-----
      const parts = (lines[1] || "").trim().split(/\s+/);
      const isBlacklisted = parts[0] === "true" && parts[1] === "true";
      //-----debug-----
      console.log("[BlacklistClient] isBlacklisted =", isBlacklisted);
      //-----end of debug-----
      resolve(isBlacklisted);

    });
    /*client.on("end", () => {
      //-----debug-----
      console.log("[BlacklistClient] Connection ended, buffer:", buffer);
      //-----end of debug-----
      const lines = buffer.trim().split("\n");
      //-----debug-----
      console.log("[BlacklistClient] line 0: ", lines[0]);
      console.log("[BlacklistClient] line 1: ", lines[1]);
      //-----end of debug-----
      if (lines[0] !== "200 OK") {
        return reject(new Error(`Unexpected status from blacklist server: ${lines[0]}`));
      }
      //-----debug-----
      console.log("[BlacklistClient] Status OK, processing response...");
      //-----end of debug-----
      const parts = (lines[1] || "").trim().split(/\s+/);
      const isBlacklisted = parts[0] === "true" && parts[1] === "true";
      //-----debug-----
      console.log("[BlacklistClient] isBlacklisted =", isBlacklisted);
      //-----end of debug-----
      resolve(isBlacklisted);
    });*/

    client.on("error", reject);
  });
}

function addUrlToBlacklist(url) {
  return new Promise((resolve, reject) => {
    const client = net.createConnection({ host: "blacklist-server", port: 5555 }, () => {
      client.write(`POST ${url}\n`);
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
    const client = net.createConnection({ host: "blacklist-server", port: 5555 }, () => {
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
