package com.example.durtlesreborn.session

import kotlin.random.Random

enum class SubjectKind(val order: Int) {
    RADICAL(10),
    KANJI(20),
    VOCABULARY(30),
    KANA_VOCABULARY(30),
    ;

    /** Question kinds asked for this subject. In on'yomi/kun'yomi mode kanji get separate readings. */
    fun questionKinds(onkun: Boolean): List<QuestionKind> = when (this) {
        RADICAL -> listOf(QuestionKind.RADICAL_NAME)
        KANJI -> if (onkun) {
            listOf(QuestionKind.KANJI_MEANING, QuestionKind.KANJI_ONYOMI, QuestionKind.KANJI_KUNYOMI)
        } else {
            listOf(QuestionKind.KANJI_MEANING, QuestionKind.KANJI_READING)
        }
        VOCABULARY -> listOf(QuestionKind.VOCAB_MEANING, QuestionKind.VOCAB_READING)
        KANA_VOCABULARY -> listOf(QuestionKind.KANA_VOCAB_MEANING)
    }
}

/** What the planner needs to know about a subject and its assignment. */
data class SessionSubject(
    val id: Long,
    val assignmentId: Long,
    val kind: SubjectKind,
    val level: Int,
    val srsStageIndex: Int,
    val srs: SrsSystem,
    val characters: String? = null,
    val overdue: Boolean = false,
    /** When the assignment became due, epoch milliseconds; 0 when unknown. */
    val dueAt: Long = 0,
    val hasAcceptedMeanings: Boolean = true,
    val hasAcceptedReadings: Boolean = true,
    val hasOnYomi: Boolean = false,
    val hasKunYomi: Boolean = false,
) {
    val passed get() = srsStageIndex >= srs.passedIndex
}

/**
 * The sort keys the original's order enums are made of, plus DUE for the earliest due first and
 * HIGHEST_LEVEL for Tsurukame's "Current level first".
 */
enum class SortKey { LEVEL, TYPE, SRS, DUE, RADICALS_FIRST, HIGHEST_LEVEL }

/**
 * An ordering of subjects. An empty key list is "shuffle": every subject compares equal, so they
 * all end up in one bucket and are asked in random order.
 */
class SubjectOrder(val keys: List<SortKey>, val shuffle: Boolean = keys.isEmpty()) {
    constructor(vararg keys: SortKey) : this(keys.toList())

    fun comparator(): Comparator<SessionSubject> {
        var result: Comparator<SessionSubject> = Comparator { _, _ -> 0 }
        for (key in keys) {
            val next = when (key) {
                SortKey.LEVEL -> compareBy<SessionSubject> { it.level }
                SortKey.TYPE -> compareBy { it.kind.order }
                SortKey.SRS -> compareBy { it.srsStageIndex }
                SortKey.DUE -> compareBy { it.dueAt }
                SortKey.RADICALS_FIRST -> compareByDescending { it.kind == SubjectKind.RADICAL }
                SortKey.HIGHEST_LEVEL -> compareByDescending { it.level }
            }
            result = result.then(next)
        }
        return result
    }

    companion object {
        /**
         * Parses the original's order names such as "SHUFFLE" or "LEVEL_THEN_TYPE_THEN_SRS".
         * Returns null for a name that is not one of the sort-key orders.
         */
        fun fromName(name: String): SubjectOrder? {
            if (name == "SHUFFLE") return SHUFFLE
            var parts = name.split("_THEN_")
            // "LEVEL_THEN_SHUFFLE" sorts by level and shuffles within it.
            val shuffle = parts.size > 1 && parts.last() == "SHUFFLE"
            if (shuffle) parts = parts.dropLast(1)
            val keys = parts.map { part ->
                // The original names the radicals-first lesson order "RADICALS_THEN_LEVEL_THEN_TYPE".
                if (part == "RADICALS") SortKey.RADICALS_FIRST else SortKey.entries.firstOrNull { it.name == part } ?: return null
            }
            return if (keys.toSet().size == keys.size) SubjectOrder(keys, shuffle) else null
        }

        // The review orders are every ordering of level, type and SRS stage, plus shuffle.
        val SHUFFLE = SubjectOrder()
        val LEVEL = SubjectOrder(SortKey.LEVEL)
        val TYPE = SubjectOrder(SortKey.TYPE)
        val SRS = SubjectOrder(SortKey.SRS)
        val LEVEL_THEN_TYPE = SubjectOrder(SortKey.LEVEL, SortKey.TYPE)
        val LEVEL_THEN_SRS = SubjectOrder(SortKey.LEVEL, SortKey.SRS)
        val TYPE_THEN_LEVEL = SubjectOrder(SortKey.TYPE, SortKey.LEVEL)
        val TYPE_THEN_SRS = SubjectOrder(SortKey.TYPE, SortKey.SRS)
        val SRS_THEN_TYPE = SubjectOrder(SortKey.SRS, SortKey.TYPE)
        val SRS_THEN_LEVEL = SubjectOrder(SortKey.SRS, SortKey.LEVEL)
        val DUE = SubjectOrder(SortKey.DUE)

        // Lesson orders that shuffle within the sort key.
        val LEVEL_THEN_SHUFFLE = SubjectOrder(listOf(SortKey.LEVEL), shuffle = true)
        val TYPE_THEN_SHUFFLE = SubjectOrder(listOf(SortKey.TYPE), shuffle = true)
    }
}

/** Extra grouping applied on top of the chosen order, like the original's session priority. */
sealed interface SessionPriority {
    fun wrap(base: Comparator<SessionSubject>, context: PriorityContext): Comparator<SessionSubject>

    data object None : SessionPriority {
        override fun wrap(base: Comparator<SessionSubject>, context: PriorityContext) = base
    }

    data object RadicalsFirst : SessionPriority {
        override fun wrap(base: Comparator<SessionSubject>, context: PriorityContext) =
            first({ it.kind == SubjectKind.RADICAL }, base)
    }

    /** Subjects that are not yet passed and are needed to level up come first. */
    data object LevelUpFirst : SessionPriority {
        override fun wrap(base: Comparator<SessionSubject>, context: PriorityContext) =
            if (context.userLevel >= context.maxLevel) {
                base
            } else {
                first({ !it.passed && it.id in context.levelUpIds }, base)
            }
    }

    data object CurrentLevelRadicalKanjiFirst : SessionPriority {
        override fun wrap(base: Comparator<SessionSubject>, context: PriorityContext) =
            first({ it.level == context.userLevel && it.kind != SubjectKind.VOCABULARY && it.kind != SubjectKind.KANA_VOCABULARY }, base)
    }

    data object CurrentLevelFirst : SessionPriority {
        override fun wrap(base: Comparator<SessionSubject>, context: PriorityContext) =
            first({ it.level == context.userLevel }, base)
    }

    companion object {
        /** Parses the original's priority names such as "RADICALS_FIRST"; unknown names mean no priority. */
        fun fromName(name: String): SessionPriority = when (name) {
            "RADICALS_FIRST" -> RadicalsFirst
            "LEVEL_UP_FIRST" -> LevelUpFirst
            "CURRENT_LEVEL_RADICAL_KANJI_FIRST" -> CurrentLevelRadicalKanjiFirst
            "CURRENT_LEVEL_FIRST" -> CurrentLevelFirst
            else -> None
        }

        /** Subjects matching [predicate] sort before the others; ties fall back to [base]. */
        fun first(predicate: (SessionSubject) -> Boolean, base: Comparator<SessionSubject>) =
            compareByDescending<SessionSubject> { predicate(it) }.then(base)
    }
}

data class PriorityContext(
    val userLevel: Int = 0,
    val maxLevel: Int = 60,
    /** Ids of the subjects that count toward leveling up. */
    val levelUpIds: Set<Long> = emptySet(),
)

/** Settings of one kind of session. */
data class SessionConfig(
    val type: SessionType,
    val order: SubjectOrder,
    val orderReversed: Boolean = false,
    /** Overdue subjects first (reviews and self-study only). */
    val overdueFirst: Boolean = false,
    val priority: SessionPriority = SessionPriority.None,
    val priorityContext: PriorityContext = PriorityContext(),
    val maxSize: Int = Int.MAX_VALUE,
    /** After picking the subjects, ask them in random order instead of the sorted one. */
    val shuffleAfterSelection: Boolean = false,
    val onkun: Boolean = false,
    val delayed: Boolean = false,
    val backToBack: Boolean = false,
    val readingFirst: Boolean = false,
    val meaningFirst: Boolean = false,
)

/**
 * Builds a session the way the original's Session.populateItems and startNew*Session do:
 * order the subjects, trim to the maximum size, group equal-ranked subjects into buckets (lower
 * buckets are asked first, random within a bucket) and create the questions each item needs.
 *
 * Not included: the original's subject selection rules (minimum/maximum per level and type).
 */
object SessionPlanner {
    fun plan(
        subjects: List<SessionSubject>,
        config: SessionConfig,
        listener: SessionListener,
        random: Random = Random.Default,
        clock: () -> Long = System::currentTimeMillis,
    ): SessionEngine {
        require(subjects.isNotEmpty()) { "A session needs at least one subject" }

        var comparator = config.order.comparator()
        if (config.orderReversed) comparator = comparator.reversed()
        if (config.overdueFirst && config.type != SessionType.LESSON) {
            comparator = compareByDescending<SessionSubject> { it.overdue }.then(comparator)
        }
        comparator = config.priority.wrap(comparator, config.priorityContext)

        // Reviews and self-study always shuffle first so ties come out random; sorting is stable.
        val shuffleFirst = config.type != SessionType.LESSON || config.order.shuffle
        var list = if (shuffleFirst) subjects.shuffled(random) else subjects
        list = list.sortedWith(comparator)
        if (list.size > config.maxSize) list = list.take(config.maxSize)
        if (config.shuffleAfterSelection) comparator = Comparator { _, _ -> 0 }

        val items = ArrayList<SessionItem>(list.size)
        var bucket = 0
        var previous: SessionSubject? = null
        for (subject in list) {
            if (previous != null && comparator.compare(previous, subject) != 0) bucket++
            val item = SessionItem(
                id = subject.id,
                bucket = bucket,
                srs = subject.srs,
                srsStageIndex = subject.srsStageIndex,
                characters = subject.characters,
                assignmentId = subject.assignmentId,
            )
            // Slots a subject has nothing to ask for count as done.
            item.setDone(1, !(subject.hasAcceptedMeanings))
            item.setDone(2, !needsReading(subject, config.onkun, onkunSlot = false))
            item.setDone(3, !needsReading(subject, config.onkun, onkunSlot = true) || !subject.hasOnYomi)
            item.setDone(4, !needsReading(subject, config.onkun, onkunSlot = true) || !subject.hasKunYomi)
            items += item
            previous = subject
        }

        val questions = items.zip(list).flatMap { (item, subject) ->
            subject.kind.questionKinds(config.onkun)
                .map { Question(item, it) }
                .filter { !it.finished }
        }

        return SessionEngine(
            type = config.type,
            items = items,
            questions = questions,
            delayed = config.delayed,
            backToBack = config.backToBack,
            readingFirst = config.readingFirst,
            meaningFirst = config.meaningFirst,
            random = { bound -> random.nextInt(bound) },
            clock = clock,
            listener = listener,
            startInPresentation = config.type == SessionType.LESSON,
        )
    }

    /**
     * Slot 2 is the plain reading, slots 3 and 4 the on'yomi and kun'yomi readings. Which slots a
     * subject uses depends on its kind and on the on'yomi/kun'yomi mode (kanji only).
     */
    private fun needsReading(subject: SessionSubject, onkun: Boolean, onkunSlot: Boolean): Boolean = when (subject.kind) {
        SubjectKind.RADICAL -> false
        SubjectKind.KANJI -> if (onkunSlot) onkun else !onkun && subject.hasAcceptedReadings
        SubjectKind.VOCABULARY -> !onkunSlot && subject.hasAcceptedReadings
        // Kana-only vocabulary has no reading question: the word is its own reading.
        SubjectKind.KANA_VOCABULARY -> false
    }
}
