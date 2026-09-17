package com.shyam.autotypex1.domain.typing

import kotlin.math.hypot

/**
 * Models the physical US QWERTY keyboard layout for:
 * 1. Adjacent-key typo generation.
 * 2. Key-to-key geometric transition distances for natural timing variations.
 */
object QwertyAdjacency {

    private val adjacencyMap: Map<Char, List<Char>> = mapOf(
        'q' to listOf('w', 'a', 's'),
        'w' to listOf('q', 'e', 'a', 's', 'd'),
        'e' to listOf('w', 'r', 's', 'd', 'f'),
        'r' to listOf('e', 't', 'd', 'f', 'g'),
        't' to listOf('r', 'y', 'f', 'g', 'h'),
        'y' to listOf('t', 'u', 'g', 'h', 'j'),
        'u' to listOf('y', 'i', 'h', 'j', 'k'),
        'i' to listOf('u', 'o', 'j', 'k', 'l'),
        'o' to listOf('i', 'p', 'k', 'l'),
        'p' to listOf('o', 'l'),
        'a' to listOf('q', 'w', 's', 'z'),
        's' to listOf('a', 'w', 'e', 'd', 'z', 'x'),
        'd' to listOf('s', 'e', 'r', 'f', 'x', 'c'),
        'f' to listOf('d', 'r', 't', 'g', 'c', 'v'),
        'g' to listOf('f', 't', 'y', 'h', 'v', 'b'),
        'h' to listOf('g', 'y', 'u', 'j', 'b', 'n'),
        'j' to listOf('h', 'u', 'i', 'k', 'n', 'm'),
        'k' to listOf('j', 'i', 'o', 'l', 'm'),
        'l' to listOf('k', 'o', 'p'),
        'z' to listOf('a', 's', 'x'),
        'x' to listOf('z', 's', 'd', 'c'),
        'c' to listOf('x', 'd', 'f', 'v'),
        'v' to listOf('c', 'f', 'g', 'b'),
        'b' to listOf('v', 'g', 'h', 'n'),
        'n' to listOf('b', 'h', 'j', 'm'),
        'm' to listOf('n', 'j', 'k')
    )

    private val keyCoordinates: Map<Char, Pair<Double, Double>> = mapOf(
        // Row 0: Digits
        '`' to (0.0 to 0.0), '1' to (1.0 to 0.0), '2' to (2.0 to 0.0), '3' to (3.0 to 0.0),
        '4' to (4.0 to 0.0), '5' to (5.0 to 0.0), '6' to (6.0 to 0.0), '7' to (7.0 to 0.0),
        '8' to (8.0 to 0.0), '9' to (9.0 to 0.0), '0' to (10.0 to 0.0), '-' to (11.0 to 0.0), '=' to (12.0 to 0.0),

        // Row 1: Top Row
        'q' to (1.5 to 1.0), 'w' to (2.5 to 1.0), 'e' to (3.5 to 1.0), 'r' to (4.5 to 1.0),
        't' to (5.5 to 1.0), 'y' to (6.5 to 1.0), 'u' to (7.5 to 1.0), 'i' to (8.5 to 1.0),
        'o' to (9.5 to 1.0), 'p' to (10.5 to 1.0), '[' to (11.5 to 1.0), ']' to (12.5 to 1.0), '\\' to (13.5 to 1.0),

        // Row 2: Home Row
        'a' to (1.75 to 2.0), 's' to (2.75 to 2.0), 'd' to (3.75 to 2.0), 'f' to (4.75 to 2.0),
        'g' to (5.75 to 2.0), 'h' to (6.75 to 2.0), 'j' to (7.75 to 2.0), 'k' to (8.75 to 2.0),
        'l' to (9.75 to 2.0), ';' to (10.75 to 2.0), '\'' to (11.75 to 2.0),

        // Row 3: Bottom Row
        'z' to (2.25 to 3.0), 'x' to (3.25 to 3.0), 'c' to (4.25 to 3.0), 'v' to (5.25 to 3.0),
        'b' to (6.25 to 3.0), 'n' to (7.25 to 3.0), 'm' to (8.25 to 3.0), ',' to (9.25 to 3.0),
        '.' to (10.25 to 3.0), '/' to (11.25 to 3.0),

        // Spacebar
        ' ' to (6.0 to 4.0)
    )

    /**
     * Returns a list of all physically adjacent characters for [char].
     */
    fun getAdjacentChars(char: Char): List<Char> {
        val lower = char.lowercaseChar()
        val list = adjacencyMap[lower] ?: emptyList()
        return if (char.isUpperCase()) {
            list.map { it.uppercaseChar() }
        } else {
            list
        }
    }

    /**
     * Picks a random physically adjacent character for [char].
     * Preserves uppercase/lowercase casing.
     */
    fun getAdjacentChar(char: Char, random: TypingRandom): Char {
        val lower = char.lowercaseChar()
        val adjacent = adjacencyMap[lower]
        val typoLower = if (!adjacent.isNullOrEmpty()) {
            adjacent[random.nextInt(0, adjacent.size)]
        } else {
            // Fallback for edge keys
            if (lower in 'a'..'z') {
                if (lower < 'z') lower + 1 else lower - 1
            } else {
                lower
            }
        }
        return if (char.isUpperCase()) typoLower.uppercaseChar() else typoLower
    }

    /**
     * Returns whether two characters are physically adjacent on the keyboard.
     */
    fun isAdjacent(c1: Char, c2: Char): Boolean {
        val l1 = c1.lowercaseChar()
        val l2 = c2.lowercaseChar()
        return adjacencyMap[l1]?.contains(l2) == true
    }

    /**
     * Calculates the Euclidean distance in key units between two keys.
     * Returns a default average distance of 2.0 if either key coordinate is unknown.
     */
    fun distance(c1: Char, c2: Char): Double {
        val p1 = keyCoordinates[c1.lowercaseChar()] ?: return 2.0
        val p2 = keyCoordinates[c2.lowercaseChar()] ?: return 2.0
        return hypot(p1.first - p2.first, p1.second - p2.second)
    }

    /**
     * Calculates a transition factor multiplier based on physical distance between keys.
     * Adjacent/nearby keys get ~0.85–0.95 (faster transition),
     * far-away keys across keyboard get ~1.05–1.25 (slower reach).
     */
    fun transitionFactor(fromChar: Char, toChar: Char): Double {
        val dist = distance(fromChar, toChar)
        // Normalize distance: 0.0 -> 0.85, 1.0 -> 0.90, 5.0 -> 1.05, 10.0 -> 1.20
        val factor = 0.85 + (dist * 0.035)
        return factor.coerceIn(0.80, 1.30)
    }
}
