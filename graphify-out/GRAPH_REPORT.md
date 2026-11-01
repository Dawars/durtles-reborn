# Graph Report - DurtlesReborn  (2026-11-01)

## Corpus Check
- 107 files · ~441,815 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 32 file(s) not represented in the graph (top: .xml 22, .properties 3, (none) 2)

## Summary
- 1714 nodes · 4580 edges · 79 communities (49 shown, 30 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 366 edges (avg confidence: 0.87)
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
- SessionRepositoryTest
- SessionRepository
- BreakdownRow
- DashboardRepository.kt
- WaniKaniClient
- Kana
- Widget Provider
- Stats
- StrokeOrder.kt
- PitchAccentTest
- assertequals
- Ported so far
- StatsRepository.kt
- SubjectKind
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
- SessionConfig
- Instrumented Test Stub
- Unit Test Stub
- SessionType
- AssignmentEntity
- FakeState
- AppContainer.kt
- activityMonths
- .upcomingMessage
- .clearSaved
- AppTheme
- .matches
- SyncEngine
- ApiModels.kt
- SettingsScreen.kt
- SessionPlannerTest
- AppIcons.kt
- MainViewModel
- Daos.kt
- AudioCacheTest
- AudioPlayer
- QuizController
- Release builds
- LevelsScreen.kt
- AppDatabase
- .sync
- FakeAssignments
- Factory
- AudioCache
- FakeCipher

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

## Communities (79 total, 30 thin omitted)

### Community 0 - "SessionEngine"
Cohesion: 0.07
Nodes (11): FileSessionSnapshotStore, SessionSnapshotStore, SessionEngine, SessionListener, Question, ItemSnapshot, QuestionRef, SessionSnapshot (+3 more)

### Community 1 - "LessonScreen.kt"
Cohesion: 0.12
Nodes (15): RelatedGroup, relatedGroups(), RelatedSubject, LessonInfo, subjectKindOf(), CardSection(), kindLabel(), PitchPatternText() (+7 more)

### Community 2 - "FakeSubjects"
Cohesion: 0.12
Nodes (4): FakeMaterials, FakeSrsSystems, FakeSubjects, LevelUpIdsTest

### Community 3 - "TokenStore.kt"
Cohesion: 0.12
Nodes (3): KeystoreTokenCipher, TokenCipher, TokenStore

### Community 4 - "CLAUDE.md"
Cohesion: 0.12
Nodes (15): confirmationReading(), AnswerRevealTest, Behaviour decisions to keep, Current state, Design direction, Durtles Reborn, Not done yet (see `docs/parity-features.md` and `docs/parity-settings.md`), Open decisions (ask the user, do not assume) (+7 more)

### Community 5 - "AppSettings"
Cohesion: 0.05
Nodes (27): BackupFormatException, ImportResult, SettingsBackup, Spec, AppSettings, Keys, SettingsStore, InMemoryDataStore (+19 more)

### Community 7 - "UiState"
Cohesion: 0.16
Nodes (6): AudioLevels, LevelAudio, Levels, Loading, StatsView, UiState

### Community 8 - "AppContainer.kt"
Cohesion: 0.10
Nodes (15): AlertCounts, currentAlertContext(), AlertContext, NotificationDecision, NotificationFrequency, CONTINUOUSLY, ONCE_PER_HOUR, ONLY_NEW_REVIEWS (+7 more)

### Community 9 - "SessionEngineTest"
Cohesion: 0.24
Nodes (3): Recorder, SessionEngineTest, SrsSystemTest

### Community 10 - "SyncEngineTest"
Cohesion: 0.10
Nodes (7): PendingKind, REVIEW, START_LESSON, PendingReview, PendingReviewDao, FakePending, SyncEngineTest

### Community 11 - "SearchTest"
Cohesion: 0.20
Nodes (5): SearchRow, queryVariants(), SearchRepository, RecordingSearchDao, SearchTest

### Community 14 - "SessionRepository"
Cohesion: 0.07
Nodes (14): AudioClip, HomeCounts, SessionRepository, SrsSystem, CurrentLevelFirst, CurrentLevelRadicalKanjiFirst, LevelUpFirst, None (+6 more)

### Community 16 - "DashboardRepository.kt"
Cohesion: 0.17
Nodes (13): buildLevelContent(), Dashboard, DashboardRepository, DayForecast, forecastDays(), HeatDay, heatLevel(), HeatMonth (+5 more)

### Community 17 - "WaniKaniClient"
Cohesion: 0.16
Nodes (10): ApiResult, CacheValidators, HttpError, NetworkError, NotModified, PagesRun, RateLimited, Success (+2 more)

### Community 19 - "Widget Provider"
Cohesion: 0.11
Nodes (4): SessionWidgetProvider, WidgetUpdater, Background and system, Open items for the next read

### Community 20 - "Stats"
Cohesion: 0.32
Nodes (12): ForecastBucket, LevelProgress, Stats, BarChart(), BreakdownSection(), Cell(), dayLabels(), ForecastSection() (+4 more)

### Community 21 - "StrokeOrder.kt"
Cohesion: 0.11
Nodes (3): startOf(), StrokeOrderChart(), StrokeOrderSection()

### Community 23 - "assertequals"
Cohesion: 0.05
Nodes (19): decode(), fallbackFor(), lookup(), PitchAccent, SessionPlanner, SortKey, DUE, LEVEL (+11 more)

### Community 24 - "Ported so far"
Cohesion: 0.09
Nodes (24): QuestionKind, KANA_VOCAB_MEANING, KANJI_KUNYOMI, KANJI_MEANING, KANJI_ONYOMI, KANJI_READING, RADICAL_NAME, VOCAB_MEANING (+16 more)

### Community 25 - "StatsRepository.kt"
Cohesion: 0.15
Nodes (6): BreakdownRow, LevelKanjiCounts, StageCount, StatsDao, StatsRepository, FakeStats

### Community 26 - "SubjectKind"
Cohesion: 0.09
Nodes (20): ItemDetail, SearchCriteria, SearchResult, StageGroup, APPRENTICE, BURNED, ENLIGHTENED, GURU (+12 more)

### Community 28 - "Layout (`app/src/main/java/com/example/durtlesreborn`)"
Cohesion: 0.18
Nodes (6): ActionPalette, scheme(), StagePalette, SubjectPalette, ThemePalette, Layout (`app/src/main/java/com/example/durtlesreborn`)

### Community 29 - "Dashboard.kt"
Cohesion: 0.12
Nodes (22): LevelTile, ActionCard(), ActivityHeatmap(), DashboardActions, DashboardContent(), DashboardScreen(), DashboardTopBar(), DryRunStrip() (+14 more)

### Community 31 - "SessionItem"
Cohesion: 0.06
Nodes (19): LocalProgress, ReviewOutcome, Report, SessionRecorder, SessionItem, SessionItemState, ABANDONED, ACTIVE (+11 more)

### Community 32 - "AppDatabase"
Cohesion: 0.26
Nodes (7): ReviewStatisticEntity, SrsSystemEntity, StudyMaterialEntity, SubjectEntity, SyncStateEntity, toEntity(), buildSearchText()

### Community 34 - "Layout (`app/src/main/java/com/example/durtlesreborn`)"
Cohesion: 0.21
Nodes (6): contentColorOn(), DurtlesTheme(), fixedScheme(), isDarkSurface(), paletteFor(), ThemeTest

### Community 35 - "QuizControllerTest"
Cohesion: 0.25
Nodes (3): AuxiliaryMeaning, Meaning, QuizControllerTest

### Community 36 - "Feature parity inventory (Phase 0)"
Cohesion: 0.22
Nodes (8): API tasks to reproduce, Dashboard and level browser, Feature parity inventory (Phase 0), Findings that change the outline, Progress and stats, Quiz rules and options (enums and model), Screens (activities), Sessions

### Community 37 - "SyncWorker.kt"
Cohesion: 0.11
Nodes (3): DurtlesApp, SyncScheduler, SyncWorker

### Community 38 - "SubjectContent.kt"
Cohesion: 0.09
Nodes (7): parseTime(), string(), toSrsSystem(), ContextSentence, ReadingInfo, stripMarkup(), SubjectContent

### Community 39 - "SubjectAnswers"
Cohesion: 0.27
Nodes (4): SubjectAnswers, DigraphMatch, Reading, ConfirmationReadingTest

### Community 40 - "AndroidNotifications.kt"
Cohesion: 0.12
Nodes (6): AlarmReceiver, AndroidNotificationSink, categoryOf(), compatPriorityOf(), importanceOf(), NotificationScheduler

### Community 41 - "correctFeedbackColors"
Cohesion: 0.22
Nodes (11): correctFeedbackColors(), FeedbackColors, harmonize(), hsl(), hslOf(), hueOf(), wrongFeedbackColors(), acceptedAnswers() (+3 more)

### Community 42 - "SessionConfig"
Cohesion: 0.18
Nodes (3): SessionConfig, EndToEndTest, LessonSessionTest

### Community 51 - "SessionType"
Cohesion: 0.13
Nodes (7): SessionType, LESSON, REVIEW, SELF_STUDY, Home, label(), SavedSession

### Community 52 - "AssignmentEntity"
Cohesion: 0.24
Nodes (3): AssignmentDao, SubjectDao, AssignmentEntity

### Community 53 - "FakeState"
Cohesion: 0.22
Nodes (3): DashboardTest, FakeLevels, FakeState

### Community 54 - "AppContainer.kt"
Cohesion: 0.13
Nodes (5): AppContainer, AudioDownloader, NotificationSettings, NotificationSink, NotificationUpdater

### Community 58 - "AppTheme"
Cohesion: 0.15
Nodes (11): AppTheme, BLACK, CRAB_CROC, DARK, DYNAMIC, FOREST, LIGHT, MATCHA (+3 more)

### Community 60 - ".matches"
Cohesion: 0.25
Nodes (3): FuzzyMatching, optimalStringAlignmentDistance(), FuzzyMatchingTest

### Community 61 - "SyncEngine"
Cohesion: 0.17
Nodes (7): Done, Drain, Failed, SyncEngine, SyncReport, SyncStep, Unchanged

### Community 62 - "ApiModels.kt"
Cohesion: 0.35
Nodes (15): AssignmentData, Collection, Pages, Resource, ResourcesUpdated, ReviewBody, ReviewRequest, ReviewResponse (+7 more)

### Community 63 - "SettingsScreen.kt"
Cohesion: 0.23
Nodes (12): AudioLevelsScreen(), LevelAudioRow(), AudioDownload, AudioDownloadRow(), ChoiceRow(), failedSuffix(), formatSize(), pretty() (+4 more)

### Community 66 - "MainViewModel"
Cohesion: 0.12
Nodes (3): Lesson, MainViewModel, Settings

### Community 67 - "Daos.kt"
Cohesion: 0.13
Nodes (6): AlertDao, ReviewStatisticDao, SearchDao, SrsSystemDao, StudyMaterialDao, SyncStateDao

### Community 70 - "QuizController"
Cohesion: 0.08
Nodes (12): CloseEnoughAction, ACCEPT_WITH_TOAST, ACCEPT_WITH_TOAST_NO_LM, REJECT, SHAKE_AND_RETRY, SILENTLY_ACCEPT, QuizCard, QuizController (+4 more)

### Community 71 - "Release builds"
Cohesion: 0.29
Nodes (6): Before the first Play upload, Build, Every upload, Installing over an existing build, Release builds, Signing key

### Community 72 - "LevelsScreen.kt"
Cohesion: 0.12
Nodes (4): BigTile(), LevelPage(), LevelsScreen(), section()

### Community 81 - "AudioCache"
Cohesion: 0.16
Nodes (3): AudioCache, isWaniKaniAudioUrl(), LevelStats

## Knowledge Gaps
- **56 isolated node(s):** `SILENTLY_ACCEPT`, `ACCEPT_WITH_TOAST`, `ACCEPT_WITH_TOAST_NO_LM`, `SHAKE_AND_RETRY`, `REJECT` (+51 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 338 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **30 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Layout (`app/src/main/java/com/example/durtlesreborn`)` connect `Layout (`app/src/main/java/com/example/durtlesreborn`)` to `SessionEngine`, `LessonScreen.kt`, `TokenStore.kt`, `CLAUDE.md`, `AppSettings`, `UiState`, `AppContainer.kt`, `SyncEngineTest`, `SearchTest`, `SessionRepository`, `BreakdownRow`, `DashboardRepository.kt`, `WaniKaniClient`, `Widget Provider`, `assertequals`, `SubjectKind`, `SessionItem`, `Layout (`app/src/main/java/com/example/durtlesreborn`)`, `SyncWorker.kt`, `SubjectContent.kt`, `AndroidNotifications.kt`, `SessionType`, `FakeState`, `AppContainer.kt`, `SyncEngine`, `SettingsScreen.kt`, `MainViewModel`, `QuizController`, `AppDatabase`, `.sync`?**
  _High betweenness centrality (0.176) - this node is a cross-community bridge._
- **Are the 45 inferred relationships involving `Layout (`app/src/main/java/com/example/durtlesreborn`)` (e.g. with `WaniKaniClient` and `.pages()`) actually correct?**
  _`Layout (`app/src/main/java/com/example/durtlesreborn`)` has 45 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SILENTLY_ACCEPT`, `ACCEPT_WITH_TOAST`, `ACCEPT_WITH_TOAST_NO_LM` to the rest of the system?**
  _56 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SessionEngine` be split into smaller, more focused modules?**
  _Cohesion score 0.06606990622335891 - nodes in this community are weakly interconnected._
- **Why does `MainViewModel` connect `MainViewModel` to `QuizController`, `UiState`, `Android Notifications`, `.sync`, `Factory`, `Kana`, `SessionType`, `AppContainer.kt`, `Ported so far`, `.clearSaved`, `SubjectKind`, `MainViewModel.kt`, `Layout (`app/src/main/java/com/example/durtlesreborn`)`, `SettingsScreen.kt`, `SessionItem`?**
  _High betweenness centrality (0.104) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `SessionEngine` (e.g. with `Layout (`app/src/main/java/com/example/durtlesreborn`)` and `Ported so far`) actually correct?**
  _`SessionEngine` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Should `LessonScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.12473118279569892 - nodes in this community are weakly interconnected._