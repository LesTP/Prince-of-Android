/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by Dávid Nagy, licensed under GPL v3+.

Module 15c: Hardcoded sprite dimensions for headless collision detection.
Extracted from SDLPoP/data/ PNG asset headers.
*/

package com.sdlpop.game

/**
 * Provides sprite (width, height) dimensions for headless replay mode.
 * In the C original, these come from loaded chtab sprite images.
 * For headless replay, we need only the dimensions (not pixel data)
 * so that setCharCollision() computes correct collision bounds.
 */
object SpriteDimensions {
    // Chtab 2: KID sprites (219 images, 1-indexed; entry 0 unused)
    private val kidWidth = intArrayOf(0, 12, 14, 19, 19, 26, 26, 27, 33, 33, 25, 26, 34, 30, 21, 12, 13, 14, 20, 26, 28, 27, 27, 38, 53, 56, 41, 33, 21, 26, 20, 19, 17, 14, 34, 26, 33, 37, 19, 33, 45, 50, 41, 40, 31, 12, 13, 21, 19, 20, 19, 17, 14, 12, 13, 18, 24, 19, 18, 19, 14, 16, 14, 13, 11, 30, 20, 39, 28, 28, 30, 33, 28, 35, 33, 32, 28, 27, 45, 41, 34, 11, 11, 13, 14, 18, 20, 21, 20, 17, 21, 19, 16, 17, 14, 14, 11, 13, 12, 13, 21, 21, 16, 14, 12, 13, 17, 21, 24, 26, 26, 26, 28, 28, 28, 22, 23, 18, 18, 19, 20, 21, 21, 24, 23, 26, 26, 26, 21, 16, 11, 17, 19, 12, 13, 13, 12, 20, 27, 31, 27, 26, 24, 19, 14, 13, 9, 12, 13, 18, 24, 25, 23, 31, 31, 30, 28, 26, 28, 26, 24, 20, 20, 19, 21, 18, 20, 20, 20, 27, 25, 22, 21, 19, 17, 27, 35, 42, 49, 42, 35, 27, 28, 35, 30, 28, 28, 33, 39, 38, 42, 28, 28, 28, 27, 25, 24, 21, 21, 20, 14, 14, 15, 17, 18, 20, 21, 21, 16, 16, 26, 26, 13, 14, 17, 21, 32, 6, 6, 28)
    private val kidHeight = intArrayOf(0, 42, 42, 41, 40, 39, 39, 40, 40, 40, 39, 38, 40, 40, 39, 41, 41, 40, 39, 33, 31, 31, 33, 34, 35, 28, 27, 26, 25, 29, 34, 36, 39, 40, 38, 37, 38, 39, 41, 43, 41, 30, 27, 35, 33, 39, 39, 40, 40, 39, 37, 38, 39, 39, 39, 38, 37, 34, 34, 35, 31, 33, 30, 24, 24, 41, 39, 39, 38, 38, 37, 36, 33, 32, 35, 36, 38, 37, 12, 27, 16, 40, 40, 40, 40, 40, 40, 39, 38, 36, 35, 38, 47, 50, 54, 54, 48, 41, 38, 40, 51, 53, 53, 55, 57, 55, 54, 54, 51, 51, 50, 50, 50, 35, 36, 35, 38, 36, 29, 23, 19, 20, 22, 24, 26, 29, 30, 33, 35, 38, 39, 4, 4, 10, 40, 39, 39, 39, 38, 38, 38, 37, 38, 39, 40, 41, 53, 44, 44, 45, 39, 33, 28, 22, 20, 24, 22, 24, 28, 31, 34, 39, 39, 39, 39, 39, 39, 38, 38, 38, 38, 38, 37, 38, 38, 34, 38, 35, 31, 34, 37, 37, 36, 34, 37, 38, 39, 36, 36, 36, 34, 35, 37, 20, 24, 28, 32, 36, 40, 42, 44, 44, 45, 45, 41, 43, 42, 42, 39, 38, 38, 43, 38, 33, 30, 22, 17, 5, 5, 26)

    // Chtab 5: GUARD sprites (34 images, 1-indexed; entry 0 unused)
    private val guardWidth = intArrayOf(0, 6, 28, 35, 38, 42, 49, 42, 35, 28, 28, 41, 35, 35, 35, 35, 42, 42, 27, 42, 34, 27, 28, 32, 28, 22, 1, 42, 53, 14, 19, 21, 21, 31, 41)
    private val guardHeight = intArrayOf(0, 5, 26, 36, 37, 36, 33, 37, 40, 40, 39, 35, 38, 37, 37, 39, 38, 39, 42, 36, 37, 39, 39, 36, 37, 36, 1, 27, 14, 40, 35, 32, 22, 19, 10)

    /**
     * Returns (width, height) for a sprite in the given chtab, or null if unknown.
     * Chtab mapping: 2 = KID, 3 = KID+sword (same images), 5 = GUARD.
     */
    fun getImageDimensions(chtab: Int, imageId: Int): Pair<Int, Int>? {
        return when (chtab) {
            2, 3 -> { // KID (chtab 3 = KID with sword overlay, same base images)
                if (imageId in 1 until kidWidth.size) {
                    kidWidth[imageId] to kidHeight[imageId]
                } else null
            }
            5 -> { // GUARD
                if (imageId in 1 until guardWidth.size) {
                    guardWidth[imageId] to guardHeight[imageId]
                } else null
            }
            else -> null
        }
    }
}
