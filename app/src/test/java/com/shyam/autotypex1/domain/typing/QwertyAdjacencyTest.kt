package com.shyam.autotypex1.domain.typing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [QwertyAdjacency].
 */
class QwertyAdjacencyTest {

    @Test
    fun `adjacent keys return expected physical neighbors`() {
        val neighborsA = QwertyAdjacency.getAdjacentChars('a')
        assertTrue(neighborsA.contains('s'))
        assertTrue(neighborsA.contains('q'))
        assertTrue(neighborsA.contains('w'))
        assertTrue(neighborsA.contains('z'))
        assertFalse(neighborsA.contains('p'))

        val neighborsG = QwertyAdjacency.getAdjacentChars('g')
        assertTrue(neighborsG.contains('f'))
        assertTrue(neighborsG.contains('h'))
        assertTrue(neighborsG.contains('t'))
        assertTrue(neighborsG.contains('y'))
        assertTrue(neighborsG.contains('v'))
        assertTrue(neighborsG.contains('b'))
    }

    @Test
    fun `uppercase characters preserve uppercase in adjacency lookups`() {
        val neighborsA = QwertyAdjacency.getAdjacentChars('A')
        assertTrue(neighborsA.contains('S'))
        assertTrue(neighborsA.contains('Q'))
        assertTrue(neighborsA.all { it.isUpperCase() })

        val random = SeededTypingRandom(12345L)
        val typoA = QwertyAdjacency.getAdjacentChar('A', random)
        assertTrue(typoA.isUpperCase())
    }

    @Test
    fun `isAdjacent returns true only for direct neighbors`() {
        assertTrue(QwertyAdjacency.isAdjacent('q', 'w'))
        assertTrue(QwertyAdjacency.isAdjacent('f', 'g'))
        assertTrue(QwertyAdjacency.isAdjacent('k', 'l'))

        assertFalse(QwertyAdjacency.isAdjacent('q', 'p'))
        assertFalse(QwertyAdjacency.isAdjacent('a', 'l'))
        assertFalse(QwertyAdjacency.isAdjacent('z', 'm'))
    }

    @Test
    fun `distance between adjacent keys is smaller than distant keys`() {
        val distAS = QwertyAdjacency.distance('a', 's')
        val distAP = QwertyAdjacency.distance('a', 'p')

        assertTrue("Distance a->s ($distAS) should be < a->p ($distAP)", distAS < distAP)

        val factorAS = QwertyAdjacency.transitionFactor('a', 's')
        val factorAP = QwertyAdjacency.transitionFactor('a', 'p')

        assertTrue("Transition factor a->s ($factorAS) should be < a->p ($factorAP)", factorAS < factorAP)
    }
}
