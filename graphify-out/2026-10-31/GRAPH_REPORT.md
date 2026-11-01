# Graph Report - DurtlesReborn  (2026-10-31)

## Corpus Check
- 107 files · ~441,626 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 32 file(s) not represented in the graph (top: .xml 22, .properties 3, (none) 2)

## Summary
- 1709 nodes · 4574 edges · 93 communities (53 shown, 40 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 366 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- SessionEngine
- LessonScreen.kt
- FakeMaterials
- TokenStore.kt
- CLAUDE.md
- AppSettings
- WaniKaniClientTest
- FakeSubjects
- AppContainer.kt
- SessionEngineTest
- SyncEngineTest
- SubjectKind
- Screens.kt
- SessionRepositoryTest
- SessionRepository
- .sync
- DashboardRepository.kt
- WaniKaniClient
- Widget Provider
- .audioLevelsState
- StrokeOrder.kt
- PitchAccentTest
- Settings parity checklist
- StatsRepository.kt
- SubjectKind
- Android Notifications
- Layout (`app/src/main/java/com/example/durtlesreborn`)
- Dashboard.kt
- Test
- SessionItem
- AppDatabase
- SessionSnapshot
- Layout (`app/src/main/java/com/example/durtlesreborn`)
- QuizControllerTest
- SyncEngine
- SyncWorker.kt
- SubjectContent.kt
- SubjectAnswers
- AndroidNotifications.kt
- correctFeedbackColors
- AppContainer.kt
- Instrumented Test Stub
- Unit Test Stub
- SessionType
- AssignmentEntity
- FakeState
- Kana
- activityMonths
- .upcomingMessage
- QuizController
- ApiModels.kt
- MainViewModel.kt
- .matches
- AppDatabase
- formatUntil
- SettingsScreen.kt
- AnswerVerdict
- AppIcons.kt
- MainViewModel
- Daos.kt
- SessionConfig
- stageLabel
- CloseEnoughAction
- Release builds
- LevelsScreen.kt
- Settings
- SettingsStore
- SessionResumeTest
- EndToEndTest.kt
- Stats
- SessionModel.kt
- SettingsStoreTest.kt
- QuestionKind
- AudioCache
- AudioPlayer
- FakeCipher
- StatsMath
- FakeAssignments
- SyncEngineTest.kt
- InMemoryDataStore
- SyncStateDao
- DurtlesApp.kt
- StageScreen
- Factory

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 86 edges
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
- `Ported so far` --references--> `CloseEnoughAction`  [INFERRED]
  docs/parity-features.md → app/src/main/java/com/example/durtlesreborn/answer/AnswerVerdict.kt
- `Ported so far` --references--> `AnswerVerdict`  [INFERRED]
  docs/parity-features.md → app/src/main/java/com/example/durtlesreborn/answer/AnswerVerdict.kt
- `Ported so far` --references--> `FuzzyMatching`  [INFERRED]
  docs/parity-features.md → app/src/main/java/com/example/durtlesreborn/answer/FuzzyMatching.kt
- `Layout (`app/src/main/java/com/example/durtlesreborn`)` --references--> `WaniKaniClient`  [INFERRED]
  CLAUDE.md → app/src/main/java/com/example/durtlesreborn/api/WaniKaniClient.kt
- `Ported so far` --references--> `WaniKaniClient`  [INFERRED]
  docs/parity-features.md → app/src/main/java/com/example/durtlesreborn/api/WaniKaniClient.kt

## Import Cycles
- None detected.

## Communities (93 total, 40 thin omitted)

### Community 0 - "SessionEngine"
Cohesion: 0.12
Nodes (3): SessionEngine, SessionListener, Question

### Community 1 - "LessonScreen.kt"
Cohesion: 0.19
Nodes (11): LessonInfo, CardSection(), kindLabel(), PitchPatternText(), PitchSection(), readingsText(), RelatedChip(), RelatedSection() (+3 more)

### Community 2 - "FakeMaterials"
Cohesion: 0.23
Nodes (3): FakeMaterials, FakeSrsSystems, LevelUpIdsTest

### Community 3 - "TokenStore.kt"
Cohesion: 0.12
Nodes (3): KeystoreTokenCipher, TokenCipher, TokenStore

### Community 4 - "CLAUDE.md"
Cohesion: 0.12
Nodes (14): confirmationReading(), AnswerRevealTest, Behaviour decisions to keep, Current state, Design direction, Not done yet (see `docs/parity-features.md` and `docs/parity-settings.md`), Open decisions (ask the user, do not assume), Planned stack (confirm in Phase 0) (+6 more)

### Community 5 - "AppSettings"
Cohesion: 0.23
Nodes (6): ImportResult, SettingsBackup, Spec, AppSettings, SettingsBackupTest, Progress and stats

### Community 8 - "AppContainer.kt"
Cohesion: 0.09
Nodes (16): AlertCounts, AlertContext, NotificationDecision, NotificationFrequency, CONTINUOUSLY, ONCE_PER_HOUR, ONLY_NEW_REVIEWS, NotificationPolicy (+8 more)

### Community 9 - "SessionEngineTest"
Cohesion: 0.24
Nodes (3): Recorder, SessionEngineTest, SrsSystemTest

### Community 10 - "SyncEngineTest"
Cohesion: 0.10
Nodes (7): PendingKind, REVIEW, START_LESSON, PendingReview, PendingReviewDao, FakePending, SyncEngineTest

### Community 11 - "SubjectKind"
Cohesion: 0.21
Nodes (5): queryVariants(), SearchRepository, buildSearchText(), RecordingSearchDao, SearchTest

### Community 12 - "Screens.kt"
Cohesion: 0.08
Nodes (17): LessonScreen(), Lesson, Loading, UiState, acceptedAnswers(), AppRoot(), FeedbackPanel(), ItemDetailsSheet() (+9 more)

### Community 14 - "SessionRepository"
Cohesion: 0.06
Nodes (14): AudioClip, HomeCounts, SessionRepository, SrsSystem, CurrentLevelFirst, CurrentLevelRadicalKanjiFirst, LevelUpFirst, None (+6 more)

### Community 16 - "DashboardRepository.kt"
Cohesion: 0.18
Nodes (12): buildLevelContent(), Dashboard, DashboardRepository, DayForecast, forecastDays(), HeatDay, HeatMonth, HourRow (+4 more)

### Community 17 - "WaniKaniClient"
Cohesion: 0.16
Nodes (10): ApiResult, CacheValidators, HttpError, NetworkError, NotModified, PagesRun, RateLimited, Success (+2 more)

### Community 19 - "Widget Provider"
Cohesion: 0.08
Nodes (11): SessionWidgetProvider, WidgetUpdater, API tasks to reproduce, Background and system, Dashboard and level browser, Feature parity inventory (Phase 0), Findings that change the outline, Open items for the next read (+3 more)

### Community 21 - "StrokeOrder.kt"
Cohesion: 0.10
Nodes (3): hasStrokeOrder(), startOf(), StrokeOrderChart()

### Community 24 - "Settings parity checklist"
Cohesion: 0.11
Nodes (17): Advanced settings, API and audio download settings, Audio settings, Dashboard settings, Display settings, Experimental (temporary) settings, Font settings, Keyboard input settings (+9 more)

### Community 25 - "StatsRepository.kt"
Cohesion: 0.15
Nodes (6): BreakdownRow, LevelKanjiCounts, StageCount, StatsDao, StatsRepository, FakeStats

### Community 26 - "SubjectKind"
Cohesion: 0.08
Nodes (24): RelatedGroup, relatedGroups(), RelatedSubject, ItemDetail, SearchCriteria, SearchResult, StageGroup, APPRENTICE (+16 more)

### Community 28 - "Layout (`app/src/main/java/com/example/durtlesreborn`)"
Cohesion: 0.18
Nodes (6): ActionPalette, scheme(), StagePalette, SubjectPalette, ThemePalette, Layout (`app/src/main/java/com/example/durtlesreborn`)

### Community 29 - "Dashboard.kt"
Cohesion: 0.13
Nodes (23): LevelTile, LevelProgress, ActionCard(), ActivityHeatmap(), DashboardActions, DashboardContent(), DashboardScreen(), DashboardTopBar() (+15 more)

### Community 31 - "SessionItem"
Cohesion: 0.08
Nodes (9): LocalProgress, ReviewOutcome, Report, SessionRecorder, SessionItem, FakeStatistics, LocalProgressTest, SessionListener (+1 more)

### Community 32 - "AppDatabase"
Cohesion: 0.32
Nodes (6): ReviewStatisticEntity, SrsSystemEntity, StudyMaterialEntity, SubjectEntity, SyncStateEntity, toEntity()

### Community 33 - "SessionSnapshot"
Cohesion: 0.16
Nodes (6): FileSessionSnapshotStore, SessionSnapshotStore, ItemSnapshot, QuestionRef, SessionSnapshot, SrsSnapshot

### Community 34 - "Layout (`app/src/main/java/com/example/durtlesreborn`)"
Cohesion: 0.11
Nodes (17): AppTheme, BLACK, CRAB_CROC, DARK, DYNAMIC, FOREST, LIGHT, MATCHA (+9 more)

### Community 35 - "QuizControllerTest"
Cohesion: 0.22
Nodes (3): Meaning, KanaTypingTest, QuizControllerTest

### Community 36 - "SyncEngine"
Cohesion: 0.17
Nodes (7): Done, Drain, Failed, SyncEngine, SyncReport, SyncStep, Unchanged

### Community 38 - "SubjectContent.kt"
Cohesion: 0.09
Nodes (7): parseTime(), string(), toSrsSystem(), ContextSentence, ReadingInfo, stripMarkup(), SubjectContent

### Community 39 - "SubjectAnswers"
Cohesion: 0.27
Nodes (4): SubjectAnswers, DigraphMatch, Reading, ConfirmationReadingTest

### Community 40 - "AndroidNotifications.kt"
Cohesion: 0.11
Nodes (7): AlarmReceiver, AndroidNotificationSink, categoryOf(), compatPriorityOf(), importanceOf(), NotificationScheduler, NotificationSettings

### Community 41 - "correctFeedbackColors"
Cohesion: 0.25
Nodes (9): correctFeedbackColors(), FeedbackColors, harmonize(), hsl(), hslOf(), hueOf(), wrongFeedbackColors(), contrastRatio() (+1 more)

### Community 51 - "SessionType"
Cohesion: 0.13
Nodes (7): SessionType, LESSON, REVIEW, SELF_STUDY, Home, label(), SavedSession

### Community 52 - "AssignmentEntity"
Cohesion: 0.24
Nodes (3): AssignmentDao, SubjectDao, AssignmentEntity

### Community 53 - "FakeState"
Cohesion: 0.22
Nodes (3): DashboardTest, FakeLevels, FakeState

### Community 55 - "activityMonths"
Cohesion: 0.38
Nodes (3): activityMonths(), heatLevel(), HeatmapTest

### Community 57 - "QuizController"
Cohesion: 0.11
Nodes (5): QuizCard, QuizController, QuizFeedback, QuizProgress, Quiz

### Community 58 - "ApiModels.kt"
Cohesion: 0.35
Nodes (15): AssignmentData, Collection, Pages, Resource, ResourcesUpdated, ReviewBody, ReviewRequest, ReviewResponse (+7 more)

### Community 60 - ".matches"
Cohesion: 0.25
Nodes (3): FuzzyMatching, optimalStringAlignmentDistance(), FuzzyMatchingTest

### Community 63 - "SettingsScreen.kt"
Cohesion: 0.13
Nodes (12): AudioLevelsScreen(), LevelAudioRow(), AudioDownload, AudioDownloadRow(), ChoiceRow(), failedSuffix(), formatSize(), pretty() (+4 more)

### Community 66 - "MainViewModel"
Cohesion: 0.11
Nodes (4): Levels, MainViewModel, SaveRequest, Summary

### Community 67 - "Daos.kt"
Cohesion: 0.12
Nodes (6): AlertDao, ReviewStatisticDao, SearchDao, SearchRow, SrsSystemDao, StudyMaterialDao

### Community 68 - "SessionConfig"
Cohesion: 0.18
Nodes (3): SessionConfig, EndToEndTest, LessonSessionTest

### Community 70 - "CloseEnoughAction"
Cohesion: 0.25
Nodes (7): CloseEnoughAction, ACCEPT_WITH_TOAST, ACCEPT_WITH_TOAST_NO_LM, REJECT, SHAKE_AND_RETRY, SILENTLY_ACCEPT, closeEnoughLabel()

### Community 71 - "Release builds"
Cohesion: 0.29
Nodes (6): Before the first Play upload, Build, Every upload, Installing over an existing build, Release builds, Signing key

### Community 72 - "LevelsScreen.kt"
Cohesion: 0.12
Nodes (4): BigTile(), LevelPage(), LevelsScreen(), section()

### Community 74 - "SettingsStore"
Cohesion: 0.21
Nodes (3): Keys, SettingsStore, SettingsStoreTest

### Community 76 - "EndToEndTest.kt"
Cohesion: 0.19
Nodes (5): SessionPlanner, SessionPriority, SubjectOrder, Dispatcher, Dispatcher

### Community 77 - "Stats"
Cohesion: 0.27
Nodes (12): ForecastBucket, Stats, StatsView, BarChart(), BreakdownSection(), Cell(), dayLabels(), ForecastSection() (+4 more)

### Community 78 - "SessionModel.kt"
Cohesion: 0.18
Nodes (10): SessionItemState, ABANDONED, ACTIVE, PENDING, REPORTED, SessionState, ACTIVE, FINISHING (+2 more)

### Community 79 - "SettingsStoreTest.kt"
Cohesion: 0.18
Nodes (6): SortKey, DUE, LEVEL, RADICALS_FIRST, SRS, TYPE

### Community 80 - "QuestionKind"
Cohesion: 0.20
Nodes (9): QuestionKind, KANA_VOCAB_MEANING, KANJI_KUNYOMI, KANJI_MEANING, KANJI_ONYOMI, KANJI_READING, RADICAL_NAME, VOCAB_MEANING (+1 more)

### Community 81 - "AudioCache"
Cohesion: 0.08
Nodes (8): AudioCache, isWaniKaniAudioUrl(), LevelStats, decode(), fallbackFor(), lookup(), PitchAccent, AudioCacheTest

### Community 91 - "StageScreen"
Cohesion: 0.50
Nodes (3): pluralLabel(), StageRow(), StageScreen()

## Knowledge Gaps
- **56 isolated node(s):** `SILENTLY_ACCEPT`, `ACCEPT_WITH_TOAST`, `ACCEPT_WITH_TOAST_NO_LM`, `SHAKE_AND_RETRY`, `REJECT` (+51 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 334 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **40 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Layout (`app/src/main/java/com/example/durtlesreborn`)` connect `Layout (`app/src/main/java/com/example/durtlesreborn`)` to `SessionEngine`, `LessonScreen.kt`, `TokenStore.kt`, `CLAUDE.md`, `AppSettings`, `AppContainer.kt`, `SyncEngineTest`, `SubjectKind`, `SessionRepository`, `.sync`, `DashboardRepository.kt`, `WaniKaniClient`, `Widget Provider`, `.audioLevelsState`, `SubjectKind`, `SessionItem`, `Layout (`app/src/main/java/com/example/durtlesreborn`)`, `SyncEngine`, `SyncWorker.kt`, `SubjectContent.kt`, `AndroidNotifications.kt`, `AppContainer.kt`, `SessionType`, `FakeState`, `QuizController`, `AppDatabase`, `SettingsScreen.kt`, `MainViewModel`, `SettingsStore`, `EndToEndTest.kt`, `StatsMath`?**
  _High betweenness centrality (0.179) - this node is a cross-community bridge._
- **Are the 45 inferred relationships involving `Layout (`app/src/main/java/com/example/durtlesreborn`)` (e.g. with `WaniKaniClient` and `.pages()`) actually correct?**
  _`Layout (`app/src/main/java/com/example/durtlesreborn`)` has 45 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SILENTLY_ACCEPT`, `ACCEPT_WITH_TOAST`, `ACCEPT_WITH_TOAST_NO_LM` to the rest of the system?**
  _56 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SessionEngine` be split into smaller, more focused modules?**
  _Cohesion score 0.12413793103448276 - nodes in this community are weakly interconnected._
- **Why does `MainViewModel` connect `MainViewModel` to `QuizControllerTest`, `Android Notifications`, `Settings`, `AppContainer.kt`, `Screens.kt`, `Stats`, `Layout (`app/src/main/java/com/example/durtlesreborn`)`, `.sync`, `SessionType`, `.audioLevelsState`, `QuizController`, `SubjectKind`, `MainViewModel.kt`, `Factory`, `SettingsScreen.kt`, `SessionItem`?**
  _High betweenness centrality (0.103) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `SessionEngine` (e.g. with `Layout (`app/src/main/java/com/example/durtlesreborn`)` and `Ported so far`) actually correct?**
  _`SessionEngine` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Should `TokenStore.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.1225296442687747 - nodes in this community are weakly interconnected._