const fs = require('fs');
const path = require('path');
const outDir = path.join(__dirname, 'out');

const files = fs.readdirSync(outDir).filter(f => f.startsWith('sigungu_'));
let problems = 0;
for (const file of files) {
  const regions = JSON.parse(fs.readFileSync(path.join(outDir, file), 'utf8'));
  const codes = new Set();
  for (const r of regions) {
    if (codes.has(r.code)) {
      console.log(`DUPLICATE CODE in ${file}: ${r.code} (${r.name})`);
      problems++;
    }
    codes.add(r.code);
    if (r.rings.length === 0) {
      console.log(`EMPTY RINGS in ${file}: ${r.name}`);
      problems++;
    }
    for (const ring of r.rings) {
      if (ring.length < 4) {
        console.log(`DEGENERATE RING in ${file}: ${r.name} has a ring with ${ring.length} points`);
        problems++;
      }
    }
  }
}
console.log(problems === 0 ? 'no problems found' : `${problems} problem(s) found`);

// print 경기도, 인천, 전남광주 lists for a final human sanity check
for (const code of ['41', '28', '12']) {
  const regions = JSON.parse(fs.readFileSync(path.join(outDir, `sigungu_${code}.json`), 'utf8'));
  console.log(`\n${code}: ${regions.map(r => r.name).join(', ')}`);
}
