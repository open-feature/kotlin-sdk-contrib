package dev.openfeature.kotlin.contrib.providers.envvar

/**
 * Converts an underscore-separated string to camel case.
 */
internal fun String.camelCase(): String {
    // Split by underscores
    val words =
        split('_')
            .filter { it.isNotEmpty() } // Remove empty strings that might result from splitting

    if (words.isEmpty()) {
        return ""
    }

    // The first word is converted to lowercase
    val firstWord = words.first().lowercase()

    // Subsequent words are capitalized (first letter uppercase, rest lowercase)
    val restOfWords =
        words.drop(1).joinToString("") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

    return firstWord + restOfWords
}
