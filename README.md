# 대한민국 지리 퀴즈 (Korea Geography Quiz)

Kotlin + Jetpack Compose Android app for learning Korean administrative
divisions: 광역자치단체(16개 시/도, 2026-07-01 광주·전남 통합 이후) →
시/군/구(230개).

## How it works

1. App opens on a 2D vector map of Korea, camera starting zoomed in on the
   country's center and animating out to frame the whole country (the "3D
   fly-out" effect from the spec, done with a Compose `Animatable` camera
   rather than a real 3D engine — see "Design notes" below for why).
2. Tap a province to fly the camera into it and switch to its city/county
   subdivisions.
3. Press **시작** to hide all labels and start the quiz. Tap any region, type
   its name, and submit. Correct answers reveal that region's label
   permanently. While typing, going past the answer's length (minus 1)
   or hitting backspace twice automatically shows the 글자수/초성 hints; each kind
   can be turned off in 설정.
4. Guessing every region in the current view shows a completion dialog.

## Project layout

- `geo/` — GeoJSON parsing, the lon/lat → screen projection, and point-in-polygon
  hit testing.
- `quiz/` — quiz state machine (`QuizViewModel`), answer normalization/aliases,
  and the 초성 hint generator.
- `ui/map/` — the Compose map canvas, camera animation, answer/completion
  dialogs, and the main screen.
- `app/src/main/assets/geo/` — bundled boundary data (see below).

## Building

This was authored in an environment without the Android SDK or a JDK
installed, so **it has not been compiled or run yet**. To build it:

1. Open the project root folder in Android Studio (Koala or newer). Let it
   sync Gradle — it will download AGP 8.5.2, Gradle 8.9, and Kotlin 2.0.20.
2. Run on an emulator or device (minSdk 26).
3. If Gradle sync complains about a missing `local.properties` / SDK path,
   point it at your Android SDK (Android Studio usually does this
   automatically on first sync).

Since the app logic couldn't be exercised in a running emulator here, please
treat the first run as a real test pass — in particular the intro camera
animation, tap hit-testing accuracy near region borders, and the on-screen
keyboard behavior with the answer dialog are worth checking first.

## Data source & scope

Boundary polygons come from the [`admdongkor`](https://github.com/vuski/admdongkor)
npm package (MIT, 행정안전부 공개자료 가공, actively maintained with dated
snapshots back to 1975). The app bundles the `"20260701"` snapshot — the most recent available at
build time, which happens to be the date Korea's latest administrative
changes took effect (제9회 지방선거 다음 날). To refresh later, call
`adk.get(newVersion, "sido" | "sgg")` for a newer entry from `adk.versions()`
and re-run the ward-merge preprocessing script (see git history for the
script used to build the currently-bundled snapshot).

Cities whose wards appear as separate sgg features in the source (수원시,
성남시, 안양시, 부천시, 안산시, 고양시, 용인시, 화성시, 청주시, 천안시,
전주시, 포항시, 창원시) are merged back into a single city polygon at build
time, since this app teaches city names, not their internal wards. The merge
is a real polygon union (matching boundary edges between adjacent wards are
detected and cancelled, not just a naive concatenation), so no ward seams
remain in the merged shape.

As of the bundled snapshot, this data source already reflects:
- 세종특별자치시/제주특별자치도 shortened to 세종시/제주도 for display (the
  only two manual renames — everything else uses its exact current official
  name).
- 인천 검단구/영종구/제물포구/서해구 (2026-07-01 인천형 행정체제 개편).
- 전남광주통합특별시 (2026-07-01, 광주광역시+전라남도 최초 광역행정통합).
- 강원특별자치도 (2023), 전북특별자치도 (2024).
- 인천 남구 → 미추홀구 (2018), 청주시+청원군 통합 (2014), 여주군 → 여주시
  (2013), and every other historical rename/merge already baked into the
  snapshot — none of these need manual patching the way the old 2013-dataset
  version of this project required.

**Scope**: v1 covers 시/도 and 시/군/구 only, per the project's own descoping
decision (자치구/법정동 would need ~3,500 more boundary records). The
`MapLevel` sealed class and `QuizViewModel` are written so a third level could
be added later by extending the `when` branches rather than restructuring.

## Ads

A bottom adaptive banner is always shown, and an interstitial plays every time a
run ends (all answered or "여기까지"), before the result dialog. The app currently
uses Google's public **test** AdMob IDs, set in one place: `defaultConfig` in
`app/build.gradle.kts` (`admobAppId`, `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`).
Replace them with real IDs from the AdMob console before publishing.

## Design notes / trade-offs

- **2D vector map, not a 3D engine**: a real 3D terrain render (e.g. via
  SceneView/Filament) would need actual elevation mesh + texture assets for
  Korea, which don't exist as convenient free data, and would have taken
  disproportionately longer than the flat, camera-animated illusion the intro
  actually needs. The "3D" feel comes entirely from the zoom/pan animation.
- **No map SDK (Naver/Kakao/Google)**: avoids API keys, quotas, and billing;
  the app never needs real basemap tiles since it only ever shows filled
  administrative polygons.
- **Rust**: not used. Nothing here is CPU-bound enough to justify a native
  module — hit-testing runs against a few thousand points, and rendering is a
  few hundred `Path` draws per frame. Kotlin/Compose alone is plenty fast.
- Per-level quiz progress (revealed regions, wrong counts, which screen was
  open) is persisted to SharedPreferences on every change (`SessionRepository`)
  so it survives the app's process being killed in the background, not just
  configuration changes.
