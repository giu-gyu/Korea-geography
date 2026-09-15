# 대한민국 지리 퀴즈 (Korea Geography Quiz)

Kotlin + Jetpack Compose Android app for learning Korean administrative
divisions: 광역자치단체(17개 시/도) → 시/군/구(229개).

## How it works

1. App opens on a 2D vector map of Korea, camera starting zoomed in near the
   south and animating out to frame the whole country (the "3D fly-out" effect
   from the spec, done with a Compose `Animatable` camera rather than a real
   3D engine — see "Design notes" below for why).
2. Tap a province to fly the camera into it and switch to its city/county
   subdivisions.
3. Press **시작** to hide all labels and start the quiz. Tap any region, type
   its name, and submit. Correct answers reveal that region's label
   permanently. Three wrong guesses on the same region unlocks a **힌트**
   button that shows its 초성 (initial consonants).
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

Boundary polygons come from the public `southkorea/southkorea-maps` GitHub
repository (2013 KOSTAT-derived, pre-simplified GeoJSON, ~12k points total —
small enough to bundle directly, no map SDK or API key needed). Two manual
corrections were applied to match today's administrative map:

- Cities that were split into wards in the 2013 source (수원시, 성남시,
  안양시, 부천시, 안산시, 고양시, 용인시, 청주시, 천안시, 전주시, 포항시,
  창원시) are merged back into a single city polygon, since this app teaches
  city names, not their internal wards.
- 청원군, which merged into 청주시 in 2014, is folded into 청주시's polygon.

Interior holes (found in exactly one source polygon, 청원군) are dropped for
simplicity — the visual difference is negligible.

**Scope**: v1 covers 시/도 and 시/군/구 only, per the project's own descoping
decision (자치구/법정동 would need ~3,500 more boundary records). The
`MapLevel` sealed class and `QuizViewModel` are written so a third level could
be added later by extending the `when` branches rather than restructuring.

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
- Per-level quiz progress (revealed regions, wrong counts) is kept in memory
  only (`QuizViewModel`), not persisted to disk — it resets when the app
  process dies. Worth adding `SavedStateHandle` or a small datastore if you
  want progress to survive process death.
