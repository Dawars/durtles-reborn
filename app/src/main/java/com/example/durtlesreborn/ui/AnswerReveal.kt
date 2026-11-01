package com.example.durtlesreborn.ui

/**
 * The reading shown above the item once a question is answered. Only a correct answer reveals it:
 * a wrong answer must not give the reading away above the character, whatever the answer setting
 * says, because the accepted answer is what the wrong answer is hiding.
 */
internal fun confirmationReading(answered: Boolean, correct: Boolean?, reading: String?): String? =
    reading?.takeIf { answered && correct == true }
