# Graph Report - DurtlesReborn  (2026-10-30)

## Corpus Check
- 84 files · ~46,866 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 24 file(s) not represented in the graph (top: .xml 15, .properties 3, (none) 2)

## Summary
- 1370 nodes · 3592 edges · 52 communities (31 shown, 21 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 285 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Session Engine
- Session Planning
- Sync Engine and API Client
- TokenStore.kt
- Activity and Theming
- AppSettings
- Search and Stage Groups
- Notification Policy
- Stats Repository
- Sync Queue Tests
- Layout (`app/src/main/java/com/example/durtlesreborn`)
- Screens.kt
- AppDatabase
- LessonScreen.kt
- Dashboard.kt
- Layout (`app/src/main/java/com/example/durtlesreborn`)
- WaniKaniClient
- LevelsScreen.kt
- Widget Provider
- UiState
- Settings Screen
- SessionRepository
- Answer Check Tests
- Settings parity checklist
- MainViewModel
- MainViewModel.kt
- Android Notifications
- .refreshHome
- StatsScreen.kt
- Answer Verdicts
- Background Sync Worker
- .beginSession
- Quiz Controller Tests
- CLAUDE.md
- Notification Sink and Alarm
- Question Kinds
- App and Recorder Wiring
- SyncEngineTest.kt
- Kana Input
- Close-Enough Actions
- Readings and Digraphs
- Time Formatting
- Instrumented Test Stub
- Unit Test Stub
- Feature parity inventory (Phase 0)

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 59 edges
2. `Layout (`app/src/main/java/com/example/durtlesreborn`)` - 37 edges
3. `SessionEngine` - 34 edges
4. `QuizController` - 30 edges
5. `SessionItem` - 28 edges
6. `SessionRepository` - 27 edges
7. `SyncEngine` - 27 edges
8. `SubjectKind` - 27 edges
9. `SyncEngineTest` - 27 edges
10. `Ported so far` - 27 edges

## Surprising Connections (you probably didn't know these)
- `Progress and stats` --references--> `SettingsBackup`  [INFERRED]
  docs/parity-features.md → app/src/main/java/com/example/durtlesreborn/settings/SettingsBackup.kt
- `Ported so far` --references--> `CloseEnoughAction`  [INFERRED]
  docs/parity-features.md → app/src/main/java/com/example/durtlesreborn/answer/AnswerVerdict.kt
- `Ported so far` --references--> `AnswerVerdict`  [INFERRED]
  docs/parity-features.md → app/src/main/java/com/example/durtlesreborn/answer/AnswerVerdict.kt
- `Layout (`app/src/main/java/com/example/durtlesreborn`)` --references--> `WaniKaniClient`  [INFERRED]
  CLAUDE.md → app/src/main/java/com/example/durtlesreborn/api/WaniKaniClient.kt
- `Ported so far` --references--> `WaniKaniClient`  [INFERRED]
  docs/parity-features.md → app/src/main/java/com/example/durtlesreborn/api/WaniKaniClient.kt

## Import Cycles
- None detected.

## Communities (52 total, 21 thin omitted)

### Community 0 - "Session Engine"
Cohesion: 0.05
Nodes (19): SessionEngine, SessionListener, Question, SessionItem, SessionItemState, ABANDONED, ACTIVE, PENDING (+11 more)

### Community 1 - "Session Planning"
Cohesion: 0.07
Nodes (13): QuizController, SessionConfig, SessionPlanner, SortKey, LEVEL, RADICALS_FIRST, SRS, TYPE (+5 more)

### Community 2 - "Sync Engine and API Client"
Cohesion: 0.08
Nodes (28): AssignmentData, Collection, Pages, Resource, ResourcesUpdated, ReviewBody, ReviewRequest, ReviewResponse (+20 more)

### Community 3 - "TokenStore.kt"
Cohesion: 0.06
Nodes (9): Keys, SettingsStore, KeystoreTokenCipher, TokenCipher, TokenStore, InMemoryDataStore, SettingsStoreTest, FakeCipher (+1 more)

### Community 4 - "Activity and Theming"
Cohesion: 0.05
Nodes (19): MainActivity, AppIcons, ActionPalette, AppTheme, BLACK, DARK, DYNAMIC, LIGHT (+11 more)

### Community 5 - "AppSettings"
Cohesion: 0.20
Nodes (6): BackupFormatException, ImportResult, SettingsBackup, Spec, AppSettings, SettingsBackupTest

### Community 6 - "Search and Stage Groups"
Cohesion: 0.07
Nodes (28): SearchDao, SearchRow, ItemDetail, queryVariants(), SearchCriteria, SearchRepository, SearchResult, StageGroup (+20 more)

### Community 8 - "Notification Policy"
Cohesion: 0.06
Nodes (19): AppContainer, AlertCounts, currentAlertContext(), AlertContext, NotificationDecision, NotificationFrequency, CONTINUOUSLY, ONCE_PER_HOUR (+11 more)

### Community 9 - "Stats Repository"
Cohesion: 0.09
Nodes (11): FuzzyMatching, optimalStringAlignmentDistance(), BreakdownRow, LevelKanjiCounts, StageCount, StatsDao, StatsMath, StatsRepository (+3 more)

### Community 10 - "Sync Queue Tests"
Cohesion: 0.10
Nodes (7): PendingKind, REVIEW, START_LESSON, PendingReview, PendingReviewDao, FakePending, SyncEngineTest

### Community 11 - "Layout (`app/src/main/java/com/example/durtlesreborn`)"
Cohesion: 0.09
Nodes (11): LocalProgress, ReviewOutcome, Report, SessionRecorder, SessionType, LESSON, REVIEW, SELF_STUDY (+3 more)

### Community 13 - "AppDatabase"
Cohesion: 0.05
Nodes (19): AppDatabase, AlertDao, AssignmentDao, ReviewStatisticDao, SrsSystemDao, StudyMaterialDao, SubjectDao, SyncStateDao (+11 more)

### Community 14 - "LessonScreen.kt"
Cohesion: 0.16
Nodes (6): CardSection(), kindLabel(), LessonScreen(), readingsText(), SubjectCardContent(), subjectColor()

### Community 15 - "Dashboard.kt"
Cohesion: 0.13
Nodes (17): LevelTile, ActionCard(), DashboardActions, DashboardContent(), DashboardScreen(), DashboardTopBar(), filledSegments(), ForecastCard() (+9 more)

### Community 16 - "Layout (`app/src/main/java/com/example/durtlesreborn`)"
Cohesion: 0.08
Nodes (18): buildLevelContent(), Dashboard, DashboardRepository, DayForecast, forecastDays(), HourRow, lessonPositionOf(), LevelContent (+10 more)

### Community 17 - "WaniKaniClient"
Cohesion: 0.09
Nodes (10): ApiResult, CacheValidators, HttpError, NetworkError, NotModified, RateLimited, Success, Unauthorized (+2 more)

### Community 18 - "LevelsScreen.kt"
Cohesion: 0.11
Nodes (4): BigTile(), LevelPage(), LevelsScreen(), section()

### Community 19 - "Widget Provider"
Cohesion: 0.14
Nodes (4): SessionWidgetProvider, WidgetUpdater, Background and system, Open items for the next read

### Community 20 - "UiState"
Cohesion: 0.21
Nodes (11): Detail, Loading, StatsView, UiState, AppRoot(), FeedbackPanel(), kindColor(), QuizScreen() (+3 more)

### Community 21 - "Settings Screen"
Cohesion: 0.14
Nodes (6): ChoiceRow(), pretty(), Section(), SettingsScreen(), StepperRow(), SwitchRow()

### Community 22 - "SessionRepository"
Cohesion: 0.09
Nodes (12): HomeCounts, SessionRepository, SrsSystem, CurrentLevelFirst, CurrentLevelRadicalKanjiFirst, LevelUpFirst, None, PriorityContext (+4 more)

### Community 24 - "Settings parity checklist"
Cohesion: 0.11
Nodes (17): Advanced settings, API and audio download settings, Audio settings, Dashboard settings, Display settings, Experimental (temporary) settings, Font settings, Keyboard input settings (+9 more)

### Community 25 - "MainViewModel"
Cohesion: 0.13
Nodes (4): Factory, Levels, MainViewModel, Settings

### Community 26 - "MainViewModel.kt"
Cohesion: 0.14
Nodes (5): QuizCard, QuizFeedback, QuizProgress, Quiz, acceptedAnswers()

### Community 27 - "Android Notifications"
Cohesion: 0.15
Nodes (4): AlarmReceiver, categoryOf(), compatPriorityOf(), importanceOf()

### Community 29 - "StatsScreen.kt"
Cohesion: 0.13
Nodes (12): ForecastBucket, LevelProgress, Stats, BarChart(), BreakdownSection(), Cell(), dayLabels(), ForecastSection() (+4 more)

### Community 34 - "CLAUDE.md"
Cohesion: 0.17
Nodes (11): Behaviour decisions to keep, Current state, Design direction, Not done yet (see `docs/parity-features.md` and `docs/parity-settings.md`), Open decisions (ask the user, do not assume), Planned stack (confirm in Phase 0), Principles, Roadmap (+3 more)

### Community 36 - "Question Kinds"
Cohesion: 0.18
Nodes (10): QuestionKind, KANA_VOCAB_MEANING, KANJI_KUNYOMI, KANJI_MEANING, KANJI_ONYOMI, KANJI_READING, RADICAL_NAME, VOCAB_MEANING (+2 more)

### Community 40 - "Close-Enough Actions"
Cohesion: 0.25
Nodes (7): CloseEnoughAction, ACCEPT_WITH_TOAST, ACCEPT_WITH_TOAST_NO_LM, REJECT, SHAKE_AND_RETRY, SILENTLY_ACCEPT, closeEnoughLabel()

### Community 41 - "Readings and Digraphs"
Cohesion: 0.30
Nodes (6): AuxiliaryMeaning, Meaning, SubjectAnswers, DigraphMatch, Reading, QuestionChecksTest

### Community 52 - "Feature parity inventory (Phase 0)"
Cohesion: 0.25
Nodes (7): API tasks to reproduce, Dashboard and level browser, Feature parity inventory (Phase 0), Findings that change the outline, Progress and stats, Quiz rules and options (enums and model), Screens (activities)

## Knowledge Gaps
- **50 isolated node(s):** `SILENTLY_ACCEPT`, `ACCEPT_WITH_TOAST`, `ACCEPT_WITH_TOAST_NO_LM`, `SHAKE_AND_RETRY`, `REJECT` (+45 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 268 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **21 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Layout (`app/src/main/java/com/example/durtlesreborn`)` connect `Layout (`app/src/main/java/com/example/durtlesreborn`)` to `Session Engine`, `Session Planning`, `Sync Engine and API Client`, `TokenStore.kt`, `Activity and Theming`, `AppSettings`, `Search and Stage Groups`, `Notification Policy`, `Stats Repository`, `Sync Queue Tests`, `Layout (`app/src/main/java/com/example/durtlesreborn`)`, `AppDatabase`, `WaniKaniClient`, `Widget Provider`, `Settings Screen`, `SessionRepository`, `MainViewModel`, `Android Notifications`, `Background Sync Worker`, `CLAUDE.md`, `Notification Sink and Alarm`?**
  _High betweenness centrality (0.214) - this node is a cross-community bridge._
- **Are the 36 inferred relationships involving `Layout (`app/src/main/java/com/example/durtlesreborn`)` (e.g. with `WaniKaniClient` and `AppDatabase`) actually correct?**
  _`Layout (`app/src/main/java/com/example/durtlesreborn`)` has 36 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SILENTLY_ACCEPT`, `ACCEPT_WITH_TOAST`, `ACCEPT_WITH_TOAST_NO_LM` to the rest of the system?**
  _50 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Session Engine` be split into smaller, more focused modules?**
  _Cohesion score 0.05163511187607573 - nodes in this community are weakly interconnected._
- **Why does `Ported so far` connect `Stats Repository` to `Session Engine`, `Session Planning`, `Sync Engine and API Client`, `Search and Stage Groups`, `Close-Enough Actions`, `Readings and Digraphs`, `Layout (`app/src/main/java/com/example/durtlesreborn`)`, `LessonScreen.kt`, `WaniKaniClient`, `Feature parity inventory (Phase 0)`, `UiState`, `SessionRepository`, `Answer Check Tests`, `StatsScreen.kt`, `Answer Verdicts`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `SessionEngine` (e.g. with `Layout (`app/src/main/java/com/example/durtlesreborn`)` and `Ported so far`) actually correct?**
  _`SessionEngine` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Should `Session Planning` be split into smaller, more focused modules?**
  _Cohesion score 0.06994535519125683 - nodes in this community are weakly interconnected._