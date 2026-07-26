package com.shyam.autotypex1.domain.typing

/**
 * QWERTY keyboard adjacency map for typo injection.
 * Given a character, returns a list of characters that are physically
 * adjacent on a standard QWERTY layout.
 */
object QwertyAdjacency {

    private val adjacencyMap: Map<Char, List<Char>> = mapOf(
        'q' to listOf('w', 'a'),
        'w' to listOf('q', 'e', 'a', 's'),
        'e' to listOf('w', 'r', 's', 'd'),
        'r' to listOf('e', 't', 'd', 'f'),
        't' to listOf('r', 'y', 'f', 'g'),
        'y' to listOf('t', 'u', 'g', 'h'),
        'u' to listOf('y', 'i', 'h', 'j'),
        'i' to listOf('u', 'o', 'j', 'k'),
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

    /**
     * Returns a random adjacent character for the given character.
     * If the character has no known adjacency, returns the character itself shifted by 1.
     * Preserves case of the original character.
     */
    fun getAdjacentChar(char: Char, random: kotlin.random.Random): Char {
        val lower = char.lowercaseChar()
        val adjacent = adjacencyMap[lower]
        val typoChar = if (adjacent != null && adjacent.isNotEmpty()) {
            adjacent[random.nextInt(adjacent.size)]
        } else {
            // Fallback for characters not in the adjacency map
            if (lower in 'a'..'z') {
                val offset = if (lower < 'z') 1 else -1
                (lower + offset)
            } else {
                lower
            }
        }
        return if (char.isUpperCase()) typoChar.uppercaseChar() else typoChar
    }
}
