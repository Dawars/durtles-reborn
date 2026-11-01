# Feature parity inventory (Phase 0)

Source: Smouldering Durtles master, read from `app/src/main/java/com/smouldering_durtles/wk` (364 Java files) and `AndroidManifest.xml`. Settings are listed separately in `parity-settings.md` (178 preferences). Items below are inferred from class names and still need a read of each class to turn into test cases.

## Screens (activities)

- [ ] MainActivity (home/dashboard)
- [ ] SessionActivity (lessons, reviews, self-study share it; fragments: Unanswered, Answered, Summary, Lesson, Anki, SessionLog)
- [ ] BrowseActivity + BrowseOverviewFragment + SearchResultFragment (advanced search, presets)
- [ ] SubjectInfoFragment (item detail)
- [ ] SelfStudyStartActivity
- [ ] StudyMaterialsActivity (notes and synonyms)
- [ ] BurnActivity, ResurrectActivity
- [ ] PreferencesActivity
- [ ] ThemeCustomizationActivity, FontSelectionActivity, FontImportActivity
- [ ] BackupActivity, DataImportExportActivity
- [ ] DownloadAudioActivity
- [ ] Help: DigraphHelpActivity, KeyboardHelpActivity, NoApiKeyHelpActivity
- [ ] AboutActivity, SupportActivity, TestActivity

## Background and system

- [x] Notifications (NotificationWorker logic, categories, priority, update frequency) and the hourly alarm. [ ] Background sync is partly done (periodic WorkManager sync, no network-rule setting), [x] Home-screen widget (`SessionWidgetProvider`).
- [ ] Home-screen widget (SessionWidgetProvider)
- [ ] Network state handling with `NetworkRule` (wifi-only etc.)
- [ ] Audio download, scan, abort and delete jobs — per-level automatic download, offline cache and per-level delete are done (Settings > Account > Manage audio downloads); scan jobs are not.
- [ ] Pitch-accent info download (DownloadPitchInfoTask) and reference data load
- [ ] Permissions: network state, internet, notifications, vibrate, wake lock, set alarm, exact alarm, legacy storage read/write

## API tasks to reproduce

Subjects, assignments, patched assignments, review statistics, patched review statistics, study materials (get, patched, submit), level progression, SRS systems, summary, user, report session item.

## Quiz rules and options (enums and model)

- [ ] QuestionType, QuestionChoiceReason, AnswerVerdict
- [ ] CloseEnoughAction, DontKnowButtonBehavior, SpecialButtonBehavior, KanjiAcceptedReadingType
- [ ] ReviewOrder, LessonOrder, SessionPriority, SessionType/State, SessionItemState
- [ ] Digraph matching and on-device kana input
- [ ] SubjectSelectionRules, AdvancedSearchParameters, SearchSortOrder
- [ ] SubjectCardLayout, SubjectInfoDump, FragmentTransitionAnimation, typeface configuration

## Progress and stats

- [x] Level progress (kanji needed to level up). [ ] Level durations, JLPT progress and Joyo progress are not done.
- [x] SRS breakdown and a review forecast (own design). [ ] Timeline bar chart styles are not ported.
- [x] Settings backup/restore (`SettingsBackup`). [ ] Star ratings and search preset export are not done (no such features yet).
- [ ] Session log

## Findings that change the outline

Several "new feature" proposals in the outline already exist in the original and must be treated as parity, not extras:

| Outline extra | Already in original |
| --- | --- |
| Smart notifications | NotificationWorker, notification category/priority/frequency enums |
| Widgets | SessionWidgetProvider (quick tile not found) |
| Audio with offline cache | DownloadAudioActivity and audio jobs |
| Backup and restore | BackupActivity, DataImportExportActivity |
| Forecast and stats | TimeLine, SRS breakdown, JLPT/Joyo progress |

Still genuinely new: weak-item trainer, interleaved sessions and wrap-up mode (verify in SessionActivity), handwriting, custom decks, AI helper, quick settings tile.

## Ported so far

- [x] English answer matching: `FuzzyMatching` + OSA distance + `CloseEnoughAction` + `AnswerVerdict` (`app/src/main/java/com/example/durtlesreborn/answer`, tests in `FuzzyMatchingTest`). One deliberate difference: digits of any script map via `Character.digit` instead of the original's hand-written table.
- [x] All eight `QuestionType.checkAnswer` rules: meaning (the four meaning types are identical in the original), kanji reading, vocabulary reading (retry on the source kanji's reading), kanji on'yomi, kanji kun'yomi, digraph near-misses (`QuestionChecks.kt`, `Reading.kt`; `QuestionChecksTest`).
- [x] `PseudoIme` romaji to kana and `KanaUtil`: `kana/Kana.kt` with the 290-entry table generated from the original into `kana/RomajiTable.kt` (`KanaTest`). Quirk kept for parity: only a lower-case single `n` becomes ん before a consonant.
- [ ] Not ported from `QuestionType`: titles, hints, colours, Anki-mode answer text (UI concerns), and `KanjiAcceptedReadingType` title indicators.
- [ ] Settings that feed these rules still need wiring: `require_on_in_katakana`, close-enough action, "don't know" and special button behaviour.
- [x] Question and session state (`session/SessionEngine.kt`, `SessionModel.kt`; `SessionEngineTest`, `SrsSystemTest`): question choice (buckets, choice delay, back-to-back, reading-first, meaning-first, 10-started cap), typed/Anki/don't-know answers, advance, skip, undo and retry, undo and put back, ignore, wrap-up, delayed (pending) reporting, kana-answer guard, SRS new-stage penalty.
- [ ] Not ported from `Session.java`: session loading and persistence (`load`, `populateItems`, `trimSelection`, `SubjectSelectionRules`), lightning mode, typeface choice, the session log adapter, floating UI state (toasts).
- [ ] Unverified: the SRS penalty reads stage indexes; the original takes them from the API's SRS system data. `isPassed` is assumed to mean index at or above the "passed" stage.
- [ ] Inflection drills: the original can randomise verb conjugations and adjective declensions in reviews (`InflectionUtil`, `getRandomizeInflections`). Another feature the outline did not list.

- [x] Session start (`SessionPlanner`): all 16 review orders and 6 lesson orders (as sort keys level/type/SRS, shuffle), reversal, overdue-first, the 5 session priorities, max size, shuffle-after-selection, bucketing, per-kind question sets incl. on'yomi/kun'yomi mode. `SessionPlannerTest`.
- [x] Report path (`LocalProgress`, `SessionRecorder`): local assignment patch (new stage, next review at top of the hour after interval + 30 s, passed/burned times), local statistic patch (streaks, percentage), lesson start. `LocalProgressTest`.
- [x] Lesson presentation mode (`SessionEngine` presentation state, `LessonScreen`, `LessonInfo`) and lesson orders incl. radicals-first and shuffle variants. `LessonSessionTest`, `LessonInfoTest`.
- [x] Search and item detail (`SearchRepository`, `SearchScreen`): text in characters, meanings and readings (romaji or katakana is converted for readings), type, level range, SRS group. Search presets, sort order options, JLPT/Joyo filters and tags are not done.
- [x] Themes: `theme` and `nightTheme` with five themes of this app's own (dynamic, light, dark, black, Nord); the original's two WaniKani-coloured styles are not copied. Per-colour customisation, fonts and font sizes are not done.
- [x] Self-study quiz from search results (`startSelfStudy`); results stay on the phone and nothing is queued. Saved presets and the advanced self-study settings are not done.
- [x] Stats: SRS breakdown by kind, 24-hour and 7-day review forecast, level progress (`StatsRepository`, `StatsScreen`), and level-up ids for the LEVEL_UP_FIRST priority. Level timeline, JLPT and Joyo progress, accuracy and burned views are not done.
- [ ] Not ported: `SubjectSelectionRules` (minimum/maximum per current/earlier level and per type when trimming a session), `isOverdue` threshold maths (the planner takes a precomputed flag), session persistence across app restarts (`SessionItemDao`), the summary screen's pending-item report (engine has `reportPending()`).
- [x] API tasks (subjects, assignments, review statistics, study materials, user, report review, start lesson) as `WaniKaniClient` + `SyncEngine`.
- [ ] Not yet ported from the API tasks: SRS systems and level progressions (not synced yet; `SrsSystem` in the session package is hand-built until they are), writing study materials back (`SubmitStudyMaterialTask`), the "patched" assignment/statistics/material tasks (local edits applied before the server confirms), summary, pitch info, audio download.

## Open items for the next read

- Read `SessionActivity`/`Session.java` and the answer-checking classes to extract the exact rules and port their tests.
- Read `BackgroundSyncWorker`, `NotificationWorker`, `SessionWidgetProvider`.
- Check the original's unit tests, if any, under `app/src/test`.
- Decide fork vs rewrite: the code is plain Java with Android Views, so a Compose UI means a rewrite of every screen either way.

## Dashboard and level browser

- [x] Dashboard home: lessons and reviews cards, level progress pill, radical and kanji tiles with SRS bars, forecast, stage tiles, two-pane tablet layout.
- [x] Level browser: swipe between levels, radicals, kanji and vocabulary sections.
- [ ] Vocabulary on the dashboard, per-level timeline, saved layouts.

## Sessions

- [x] Leave and resume reviews and lessons (saved after every answer, survives app kill, results sent only at the end or on wrap-up, Resume/Discard card on Home).
- [x] Item details: related subjects, swipe-up details sheet in the quiz, stroke order chart (KanjiVG) on radical and kanji pages (new feature, not in the original).
