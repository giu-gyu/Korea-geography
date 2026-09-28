const adk = require("admdongkor");
const fs = require("fs");
const path = require("path");

async function main() {
  const versions = adk.versions();
  console.log("latest 10 versions:", versions.slice(-10));

  const latest = versions[versions.length - 1];
  console.log("using version:", latest);

  const dv = await adk.dataVersion();
  console.log("data version:", dv);

  const sido = await adk.get(latest, "sido");
  console.log("sido features:", sido.features.length);
  fs.writeFileSync(path.join(__dirname, "latest_sido.json"), JSON.stringify(sido));

  const sgg = await adk.get(latest, "sgg");
  console.log("sgg features:", sgg.features.length);
  fs.writeFileSync(path.join(__dirname, "latest_sgg.json"), JSON.stringify(sgg));

  // print incheon sgg entries specifically
  const incheon = sgg.features.filter((f) => f.properties.sidonm === "인천광역시");
  console.log("incheon sgg entries:", incheon.length);
  for (const f of incheon) console.log(" -", f.properties.sggnm, f.properties.sggcd);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
