const fs = require('fs');
const path = require('path');
const { dissolveRings } = require('./dissolve.js');
const dir = __dirname;

function exteriorRings(geometry) {
  const polys = geometry.type === 'Polygon' ? [geometry.coordinates] : geometry.coordinates;
  return polys.map(poly => poly[0]);
}

function allRingsIncludingHoles(geometry) {
  const polys = geometry.type === 'Polygon' ? [geometry.coordinates] : geometry.coordinates;
  const rings = [];
  for (const poly of polys) for (const ring of poly) rings.push(ring);
  return rings;
}

function ringArea(ring) {
  let a = 0;
  for (let i = 0; i < ring.length - 1; i++) {
    const [x1, y1] = ring[i];
    const [x2, y2] = ring[i + 1];
    a += x1 * y2 - x2 * y1;
  }
  return a / 2;
}

function ringCentroid(ring) {
  let cx = 0, cy = 0, a = 0;
  for (let i = 0; i < ring.length - 1; i++) {
    const [x1, y1] = ring[i];
    const [x2, y2] = ring[i + 1];
    const cross = x1 * y2 - x2 * y1;
    a += cross;
    cx += (x1 + x2) * cross;
    cy += (y1 + y2) * cross;
  }
  a *= 0.5;
  if (Math.abs(a) < 1e-12) {
    let sx = 0, sy = 0;
    for (const [x, y] of ring) { sx += x; sy += y; }
    return [sx / ring.length, sy / ring.length];
  }
  return [cx / (6 * a), cy / (6 * a)];
}

function computeCentroidAndBbox(rings) {
  let best = null, bestArea = -1;
  let minLon = Infinity, minLat = Infinity, maxLon = -Infinity, maxLat = -Infinity;
  for (const ring of rings) {
    const area = Math.abs(ringArea(ring));
    if (area > bestArea) { bestArea = area; best = ring; }
    for (const [lon, lat] of ring) {
      if (lon < minLon) minLon = lon;
      if (lon > maxLon) maxLon = lon;
      if (lat < minLat) minLat = lat;
      if (lat > maxLat) maxLat = lat;
    }
  }
  const centroid = ringCentroid(best);
  return { centroid, bbox: [minLon, minLat, maxLon, maxLat] };
}

function round(rings, decimals = 5) {
  const f = Math.pow(10, decimals);
  return rings.map(ring => ring.map(([lon, lat]) => [Math.round(lon * f) / f, Math.round(lat * f) / f]));
}

function toRegion(code, name, rings) {
  const { centroid, bbox } = computeCentroidAndBbox(rings);
  return {
    code,
    name,
    rings: round(rings),
    centroid: [Math.round(centroid[0] * 1e5) / 1e5, Math.round(centroid[1] * 1e5) / 1e5],
    bbox: bbox.map(v => Math.round(v * 1e5) / 1e5),
  };
}

// ---- provinces (sido) ----
const sidoSource = JSON.parse(fs.readFileSync(path.join(dir, 'latest_sido.json'), 'utf8'));
// shorter, more commonly used display/answer names for these two — everything else uses
// its current official name exactly as admdongkor reports it (e.g. 강원특별자치도,
// 전북특별자치도, 전남광주통합특별시).
const provinceDisplayNameOverrides = {
  '세종특별자치시': '세종시',
  '제주특별자치도': '제주도',
};

const sido = sidoSource.features.map(f => {
  const p = f.properties;
  const name = provinceDisplayNameOverrides[p.sidonm] || p.sidonm;
  return toRegion(p.sidocd, name, exteriorRings(f.geometry));
});
sido.sort((a, b) => a.code.localeCompare(b.code));

const outDir = path.join(dir, 'out');
fs.mkdirSync(outDir, { recursive: true });
fs.writeFileSync(path.join(outDir, 'sido.json'), JSON.stringify(sido));
console.log('sido.json written, count =', sido.length);
console.log(sido.map(s => `${s.code}:${s.name}`).join(', '));

// ---- municipalities (sgg) ----
const sggSource = JSON.parse(fs.readFileSync(path.join(dir, 'latest_sgg.json'), 'utf8'));

const byProvince = {};
for (const f of sggSource.features) {
  const provCode = f.properties.sidocd;
  (byProvince[provCode] = byProvince[provCode] || []).push(f);
}

const mergeRegex = /^(.+시)(.+구)$/;

let totalOut = 0;
const summary = {};

for (const provCode of Object.keys(byProvince)) {
  const features = byProvince[provCode];

  const groups = new Map(); // parentName -> { codes: [], rings: [] }
  const passthrough = [];

  for (const f of features) {
    const name = f.properties.sggnm;
    const m = mergeRegex.exec(name);
    if (m) {
      const parentName = m[1];
      if (!groups.has(parentName)) groups.set(parentName, { codes: [], rings: [] });
      const g = groups.get(parentName);
      g.codes.push(f.properties.sggcd);
      g.rings.push(...allRingsIncludingHoles(f.geometry));
    } else {
      passthrough.push(f);
    }
  }

  const regions = [];
  for (const f of passthrough) {
    regions.push(toRegion(f.properties.sggcd, f.properties.sggnm, exteriorRings(f.geometry)));
  }
  for (const [parentName, g] of groups.entries()) {
    const minCode = g.codes.slice().sort()[0];
    const parentCode = minCode.slice(0, -1);
    const { rings: dissolved, droppedInternalEdgeCount } = dissolveRings(g.rings);
    console.log(`  dissolve ${parentName}: ${g.rings.length} ward rings -> ${dissolved.length} ring(s), ${droppedInternalEdgeCount} internal edges removed`);
    regions.push(toRegion(parentCode, parentName, dissolved));
  }

  regions.sort((a, b) => a.name.localeCompare(b.name, 'ko'));
  fs.writeFileSync(path.join(outDir, `sigungu_${provCode}.json`), JSON.stringify(regions));
  summary[provCode] = regions.length;
  totalOut += regions.length;
}

console.log('sigungu files written. per-province counts:', summary);
console.log('total sigungu regions:', totalOut);
