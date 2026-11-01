# Graph Report - DurtlesReborn  (2026-11-01)

## Corpus Check
- 107 files · ~441,885 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 32 file(s) not represented in the graph (top: .xml 22, .properties 3, (none) 2)

## Summary
- 1715 nodes · 4584 edges · 75 communities (50 shown, 25 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 367 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- SessionEngine
- LessonScreen.kt
- FakeSubjects
- TokenStore.kt
- CLAUDE.md
- AppSettings
- WaniKaniClientTest
- UiState
- AppContainer.kt
- SessionEngineTest
- SyncEngineTest
- SearchTest
- Screens.kt
- SessionRepositoryTest
- SessionRepository
- BreakdownRow
- DashboardRepository.kt
- SyncEngine
- StatsScreen.kt
- Widget Provider
- Stats
- StrokeOrder.kt
- PitchAccentTest
- assertequals
- Settings parity checklist
- StatsRepository.kt
- StageGroup
- Android Notifications
- Layout (`app/src/main/java/com/example/durtlesreborn`)
- Dashboard.kt
- Test
- SessionItem
- AppDatabase
- AnswerVerdict
- Layout (`app/src/main/java/com/example/durtlesreborn`)
- QuizControllerTest
- Feature parity inventory (Phase 0)
- SyncWorker.kt
- SubjectContent.kt
- SubjectAnswers
- AndroidNotifications.kt
- correctFeedbackColors
- Settings
- Instrumented Test Stub
- Unit Test Stub
- SessionType
- AssignmentEntity
- FakeState
- PitchAccent.kt
- activityMonths
- .upcomingMessage
- .persist
- AppTheme
- MainViewModel.kt
- .matches
- formatUntil
- SettingsScreen.kt
- AppIcons.kt
- MainViewModel
- Daos.kt
- QuizController
- Release builds
- LevelsScreen.kt
- SettingsStoreTest
- SettingsStore
- AudioCache
- FakeCipher
- InMemoryDataStore
- SubjectKind

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 87 edges
2. `Layout (`app/src/main/java/com/example/durtlesreborn`)` - 46 edges
3. `SessionEngine` - 41 edges
4. `SubjectKind` - 37 edges
5. `QuizController` - 33 edges
6. `SyncEngine` - 31 edges
7. `SessionRepository` - 30 edges
8. `SessionItem` - 30 edges
9. `SubjectAnswers` - 29 edges
10. `SrsSystem` - 29 edges

## Surprising Connections (you probably didn't know these)
- `Progress and stats` --references--> `SettingsBackup`  [INFERRED]
  docs/parity-features.md → app/src/main/java/com/example/durtlesreborn/settings/SettingsBackup.kt
- `Ported so far` --references--> `CloseEnoughAction`  [INFERRED]
  docs/parity-features.md → app/src/main/java/com/example/durtlesreborn/answer/AnswerVerdict.kt
- `Ported so far` --references--> `AnswerVerdict`  [INFERRED]
  docs/parity-features.md → app/src/main/java/com/example/durtlesreborn/answer/AnswerVerdict.kt
- `Ported so far` --references--> `FuzzyMatching`  [INFERRED]
  docs/parity-features.md → app/src/main/java/com/example/durtlesreborn/answer/FuzzyMatching.kt
- `Layout (`app/src/main/java/com/example/durtlesreborn`)` --references--> `WaniKaniClient`  [INFERRED]
  CLAUDE.md → app/src/main/java/com/example/durtlesreborn/api/WaniKaniClient.kt

## Import Cycles
- None detected.

## Communities (75 total, 25 thin omitted)

### Community 0 - "SessionEngine"
Cohesion: 0.07
Nodes (11): FileSessionSnapshotStore, SessionSnapshotStore, SessionEngine, SessionListener, Question, ItemSnapshot, QuestionRef, SessionSnapshot (+3 more)

### Community 1 - "LessonScreen.kt"
Cohesion: 0.10
Nodes (19): RelatedGroup, relatedGroups(), RelatedSubject, LessonInfo, subjectKindOf(), CardSection(), kindLabel(), LessonScreen() (+11 more)

### Community 2 - "FakeSubjects"
Cohesion: 0.13
Nodes (4): FakeMaterials, FakeSrsSystems, FakeSubjects, LevelUpIdsTest

### Community 3 - "TokenStore.kt"
Cohesion: 0.12
Nodes (3): KeystoreTokenCipher, TokenCipher, TokenStore

### Community 4 - "CLAUDE.md"
Cohesion: 0.12
Nodes (15): confirmationReading(), AnswerRevealTest, Behaviour decisions to keep, Current state, Design direction, Durtles Reborn, Not done yet (see `docs/parity-features.md` and `docs/parity-settings.md`), Open decisions (ask the user, do not assume) (+7 more)

### Community 5 - "AppSettings"
Cohesion: 0.24
Nodes (5): ImportResult, SettingsBackup, Spec, AppSettings, SettingsBackupTest

### Community 7 - "UiState"
Cohesion: 0.15
Nodes (7): AudioLevels, LevelAudio, Levels, Loading, Search, StatsView, UiState

### Community 8 - "AppContainer.kt"
Cohesion: 0.06
Nodes (20): AppContainer, AudioPlayer, AlertCounts, currentAlertContext(), AlertContext, NotificationDecision, NotificationFrequency, CONTINUOUSLY (+12 more)

### Community 9 - "SessionEngineTest"
Cohesion: 0.24
Nodes (3): Recorder, SessionEngineTest, SrsSystemTest

### Community 10 - "SyncEngineTest"
Cohesion: 0.10
Nodes (7): PendingKind, REVIEW, START_LESSON, PendingReview, PendingReviewDao, FakePending, SyncEngineTest

### Community 11 - "SearchTest"
Cohesion: 0.27
Nodes (4): queryVariants(), buildSearchText(), RecordingSearchDao, SearchTest

### Community 12 - "Screens.kt"
Cohesion: 0.05
Nodes (18): QuestionKind, KANA_VOCAB_MEANING, KANJI_KUNYOMI, KANJI_MEANING, KANJI_ONYOMI, KANJI_READING, RADICAL_NAME, VOCAB_MEANING (+10 more)

### Community 14 - "SessionRepository"
Cohesion: 0.06
Nodes (16): AudioClip, HomeCounts, SessionRepository, stripMarkup(), SrsSystem, CurrentLevelFirst, CurrentLevelRadicalKanjiFirst, LevelUpFirst (+8 more)

### Community 15 - "BreakdownRow"
Cohesion: 0.26
Nodes (4): BreakdownRow, StageCount, StatsMath, StatsMathTest

### Community 16 - "DashboardRepository.kt"
Cohesion: 0.18
Nodes (13): activityMonths(), buildLevelContent(), Dashboard, DashboardRepository, DayForecast, forecastDays(), HeatDay, HeatMonth (+5 more)

### Community 17 - "SyncEngine"
Cohesion: 0.06
Nodes (35): AssignmentData, Collection, Pages, Resource, ResourcesUpdated, ReviewBody, ReviewRequest, ReviewResponse (+27 more)

### Community 19 - "Widget Provider"
Cohesion: 0.11
Nodes (4): SessionWidgetProvider, WidgetUpdater, Background and system, Open items for the next read

### Community 20 - "Stats"
Cohesion: 0.32
Nodes (12): ForecastBucket, LevelProgress, Stats, BarChart(), BreakdownSection(), Cell(), dayLabels(), ForecastSection() (+4 more)

### Community 23 - "assertequals"
Cohesion: 0.05
Nodes (18): SessionType, LESSON, REVIEW, SELF_STUDY, SessionConfig, SessionPlanner, SortKey, DUE (+10 more)

### Community 24 - "Settings parity checklist"
Cohesion: 0.11
Nodes (17): Advanced settings, API and audio download settings, Audio settings, Dashboard settings, Display settings, Experimental (temporary) settings, Font settings, Keyboard input settings (+9 more)

### Community 25 - "StatsRepository.kt"
Cohesion: 0.17
Nodes (4): LevelKanjiCounts, StatsDao, StatsRepository, FakeStats

### Community 26 - "StageGroup"
Cohesion: 0.15
Nodes (12): ItemDetail, SearchCriteria, SearchRepository, StageGroup, APPRENTICE, BURNED, ENLIGHTENED, GURU (+4 more)

### Community 28 - "Layout (`app/src/main/java/com/example/durtlesreborn`)"
Cohesion: 0.14
Nodes (10): ActionPalette, contentColorOn(), contrastRatio(), DurtlesTheme(), paletteFor(), scheme(), StagePalette, SubjectPalette (+2 more)

### Community 29 - "Dashboard.kt"
Cohesion: 0.16
Nodes (22): LevelTile, ActionCard(), ActivityHeatmap(), DashboardActions, DashboardContent(), DashboardScreen(), DashboardTopBar(), DryRunStrip() (+14 more)

### Community 30 - "Test"
Cohesion: 0.20
Nodes (3): Kana, KanaTest, QuestionChecksTest

### Community 31 - "SessionItem"
Cohesion: 0.06
Nodes (19): LocalProgress, ReviewOutcome, Report, SessionRecorder, SessionItem, SessionItemState, ABANDONED, ACTIVE (+11 more)

### Community 32 - "AppDatabase"
Cohesion: 0.16
Nodes (7): AppDatabase, ReviewStatisticEntity, SrsSystemEntity, StudyMaterialEntity, SubjectEntity, SyncStateEntity, toEntity()

### Community 34 - "Layout (`app/src/main/java/com/example/durtlesreborn`)"
Cohesion: 0.24
Nodes (3): fixedScheme(), isDarkSurface(), ThemeTest

### Community 35 - "QuizControllerTest"
Cohesion: 0.22
Nodes (3): Meaning, KanaTypingTest, QuizControllerTest

### Community 36 - "Feature parity inventory (Phase 0)"
Cohesion: 0.22
Nodes (8): API tasks to reproduce, Dashboard and level browser, Feature parity inventory (Phase 0), Findings that change the outline, Progress and stats, Quiz rules and options (enums and model), Screens (activities), Sessions

### Community 37 - "SyncWorker.kt"
Cohesion: 0.11
Nodes (3): DurtlesApp, SyncScheduler, SyncWorker

### Community 38 - "SubjectContent.kt"
Cohesion: 0.15
Nodes (4): AuxiliaryMeaning, ContextSentence, ReadingInfo, SubjectContent

### Community 39 - "SubjectAnswers"
Cohesion: 0.27
Nodes (4): SubjectAnswers, DigraphMatch, Reading, ConfirmationReadingTest

### Community 40 - "AndroidNotifications.kt"
Cohesion: 0.12
Nodes (6): AlarmReceiver, AndroidNotificationSink, categoryOf(), compatPriorityOf(), importanceOf(), NotificationScheduler

### Community 41 - "correctFeedbackColors"
Cohesion: 0.23
Nodes (10): correctFeedbackColors(), FeedbackColors, harmonize(), hsl(), hslOf(), hueOf(), wrongFeedbackColors(), acceptedAnswers() (+2 more)

### Community 52 - "AssignmentEntity"
Cohesion: 0.17
Nodes (3): AssignmentDao, AssignmentEntity, FakeAssignments

### Community 53 - "FakeState"
Cohesion: 0.22
Nodes (3): DashboardTest, FakeLevels, FakeState

### Community 54 - "PitchAccent.kt"
Cohesion: 0.67
Nodes (4): decode(), fallbackFor(), lookup(), PitchAccent

### Community 57 - ".persist"
Cohesion: 0.17
Nodes (3): Lesson, SaveRequest, Summary

### Community 58 - "AppTheme"
Cohesion: 0.18
Nodes (11): AppTheme, BLACK, CRAB_CROC, DARK, DYNAMIC, FOREST, LIGHT, MATCHA (+3 more)

### Community 60 - ".matches"
Cohesion: 0.25
Nodes (3): FuzzyMatching, optimalStringAlignmentDistance(), FuzzyMatchingTest

### Community 62 - "formatUntil"
Cohesion: 0.22
Nodes (4): stageLabel(), formatUntil(), FormatTest, StageLabelTest

### Community 63 - "SettingsScreen.kt"
Cohesion: 0.19
Nodes (12): AudioLevelsScreen(), LevelAudioRow(), AudioDownload, AudioDownloadRow(), ChoiceRow(), failedSuffix(), formatSize(), pretty() (+4 more)

### Community 66 - "MainViewModel"
Cohesion: 0.11
Nodes (3): Factory, MainViewModel, NeedsToken

### Community 67 - "Daos.kt"
Cohesion: 0.10
Nodes (8): AlertDao, ReviewStatisticDao, SearchDao, SearchRow, SrsSystemDao, StudyMaterialDao, SubjectDao, SyncStateDao

### Community 70 - "QuizController"
Cohesion: 0.08
Nodes (12): CloseEnoughAction, ACCEPT_WITH_TOAST, ACCEPT_WITH_TOAST_NO_LM, REJECT, SHAKE_AND_RETRY, SILENTLY_ACCEPT, QuizCard, QuizController (+4 more)

### Community 71 - "Release builds"
Cohesion: 0.29
Nodes (6): Before the first Play upload, Build, Every upload, Installing over an existing build, Release builds, Signing key

### Community 72 - "LevelsScreen.kt"
Cohesion: 0.08
Nodes (7): BigTile(), LevelPage(), LevelsScreen(), section(), pluralLabel(), StageRow(), StageScreen()

### Community 81 - "AudioCache"
Cohesion: 0.08
Nodes (5): AudioCache, isWaniKaniAudioUrl(), LevelStats, AudioDownloader, AudioCacheTest

### Community 91 - "SubjectKind"
Cohesion: 0.21
Nodes (8): SearchResult, SubjectKind, KANA_VOCABULARY, KANJI, RADICAL, VOCABULARY, ofKind(), StageListTest

## Knowledge Gaps
- **56 isolated node(s):** `SILENTLY_ACCEPT`, `ACCEPT_WITH_TOAST`, `ACCEPT_WITH_TOAST_NO_LM`, `SHAKE_AND_RETRY`, `REJECT` (+51 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 338 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **25 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Layout (`app/src/main/java/com/example/durtlesreborn`)` connect `Layout (`app/src/main/java/com/example/durtlesreborn`)` to `SessionEngine`, `LessonScreen.kt`, `TokenStore.kt`, `CLAUDE.md`, `AppSettings`, `UiState`, `AppContainer.kt`, `SyncEngineTest`, `SessionRepository`, `BreakdownRow`, `DashboardRepository.kt`, `SyncEngine`, `Widget Provider`, `assertequals`, `StageGroup`, `SessionItem`, `AppDatabase`, `Layout (`app/src/main/java/com/example/durtlesreborn`)`, `SyncWorker.kt`, `SubjectContent.kt`, `AndroidNotifications.kt`, `SessionType`, `FakeState`, `SettingsScreen.kt`, `MainViewModel`, `QuizController`, `SettingsStore`, `AudioCache`?**
  _High betweenness centrality (0.179) - this node is a cross-community bridge._
- **Are the 45 inferred relationships involving `Layout (`app/src/main/java/com/example/durtlesreborn`)` (e.g. with `WaniKaniClient` and `.pages()`) actually correct?**
  _`Layout (`app/src/main/java/com/example/durtlesreborn`)` has 45 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SILENTLY_ACCEPT`, `ACCEPT_WITH_TOAST`, `ACCEPT_WITH_TOAST_NO_LM` to the rest of the system?**
  _56 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SessionEngine` be split into smaller, more focused modules?**
  _Cohesion score 0.06606990622335891 - nodes in this community are weakly interconnected._
- **Why does `MainViewModel` connect `MainViewModel` to `LessonScreen.kt`, `QuizControllerTest`, `QuizController`, `UiState`, `AppContainer.kt`, `Android Notifications`, `Settings`, `SubjectKind`, `Screens.kt`, `SessionType`, `.persist`, `StageGroup`, `MainViewModel.kt`, `Layout (`app/src/main/java/com/example/durtlesreborn`)`, `SettingsScreen.kt`, `SessionItem`?**
  _High betweenness centrality (0.108) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `SessionEngine` (e.g. with `Layout (`app/src/main/java/com/example/durtlesreborn`)` and `Ported so far`) actually correct?**
  _`SessionEngine` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Should `LessonScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.09986504723346828 - nodes in this community are weakly interconnected._