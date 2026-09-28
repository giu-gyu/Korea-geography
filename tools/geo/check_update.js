// 앱에 들어있는 행정구역 스냅샷(BUNDLED_VERSION)보다 새 스냅샷이 admdongkor에 올라왔는지 확인한다.
// GitHub Actions에서 쓸 수 있도록 GITHUB_OUTPUT에 newer / latest 를 기록한다.
const fs = require('fs');
const path = require('path');
const adk = require('admdongkor');

const bundled = fs.readFileSync(path.join(__dirname, 'BUNDLED_VERSION'), 'utf8').trim();
const versions = adk.versions().slice().sort();
const latest = versions[versions.length - 1];
const newer = latest > bundled;

console.log(`앱에 반영된 스냅샷: ${bundled}`);
console.log(`admdongkor 최신 스냅샷: ${latest}`);
console.log(newer ? '새 스냅샷이 있습니다. 데이터 갱신이 필요합니다.' : '최신입니다.');

if (process.env.GITHUB_OUTPUT) {
  fs.appendFileSync(process.env.GITHUB_OUTPUT, `newer=${newer}\nlatest=${latest}\nbundled=${bundled}\n`);
}
