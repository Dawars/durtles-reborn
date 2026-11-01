package com.example.durtlesreborn.ui

/**
 * The full name of an SRS stage index, as WaniKani shows it: Apprentice 1\u20134, Guru 1\u20132, Master,
 * Enlightened, Burned. Indices follow [com.example.durtlesreborn.session.SrsSystem].
 */
internal fun stageName(stage: Int): String = when {
    stage <= 4 -> "Apprentice ${stage.coerceAtLeast(1)}"
    stage <= 6 -> "Guru ${stage - 4}"
    stage == 7 -> "Master"
    stage == 8 -> "Enlightened"
    else -> "Burned"
}
