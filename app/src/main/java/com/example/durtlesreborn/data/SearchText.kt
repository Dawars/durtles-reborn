package com.example.durtlesreborn.data

/**
 * Lower-case text a subject is found by: its characters, slug, accepted meanings, whitelisted
 * alternative meanings and all readings, one per line. Stored in the `searchText` column.
 */
fun buildSearchText(characters: String?, slug: String?, content: SubjectContent): String =
    buildList {
        characters?.let(::add)
        slug?.let(::add)
        content.meanings.filter { it.acceptedAnswer }.forEach { add(it.text) }
        content.auxiliaryMeanings.filter { it.whitelist }.forEach { add(it.text) }
        content.readings.forEach { add(it.reading) }
    }.joinToString("\n").lowercase()
