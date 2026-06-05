package com.example.chessboard.ui.screen

/*
 * Unit tests for profile localization identifiers.
 * Keep pure id, lookup, and resource-mapping contract tests here.
 * Do not add Compose UI tests, database-backed profile flow tests, or translation wording checks.
 * Validation date: 2026-06-05
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileLocalizationTest {

    @Test
    fun `rank title storage and legacy text resolve to the same title id`() {
        // Protects the persisted profile contract: new stable storage keys and old English
        // values from existing installs must both resolve to the same rank title id.
        ProfileRankTitleId.entries.forEach { titleId ->
            assertEquals(
                titleId,
                ProfileLocalization.rankTitleIdFromStorage(titleId.storageKey),
            )
            assertEquals(
                titleId,
                ProfileLocalization.rankTitleIdFromStorage(titleId.legacyText),
            )
        }

        assertNull(ProfileLocalization.rankTitleIdFromStorage(""))
        assertNull(ProfileLocalization.rankTitleIdFromStorage("unknown_title"))
    }

    @Test
    fun `rank title ids are assigned to exactly one player tier`() {
        // Keeps random title selection safe: every title must be reachable from one tier,
        // and no title should accidentally appear in multiple tier pools.
        val assignedTitleIds = PlayerTier.entries.flatMap { tier -> tier.titleIds }

        assertEquals(ProfileRankTitleId.entries.toSet(), assignedTitleIds.toSet())
        assertEquals(assignedTitleIds.size, assignedTitleIds.toSet().size)
        PlayerTier.entries.forEach { tier ->
            assertTrue("${tier.name} must have rank titles", tier.titleIds.isNotEmpty())
        }
    }

    @Test
    fun `profile localization ids resolve to string resources`() {
        // Catches missing branches in ProfileLocalization when a new tier, title, or
        // achievement id is added but not wired to an Android string resource.
        PlayerTier.entries.forEach { tier ->
            assertNotEquals(0, ProfileLocalization.tierLabelResId(tier))
        }
        ProfileRankTitleId.entries.forEach { titleId ->
            assertNotEquals(0, ProfileLocalization.rankTitleResId(titleId))
        }
        ProfileAchievementId.entries.forEach { achievementId ->
            assertNotEquals(0, ProfileLocalization.achievementTitleResId(achievementId))
            assertNotEquals(0, ProfileLocalization.achievementDescriptionResId(achievementId))
        }
    }

    @Test
    fun `storage keys are stable and unique`() {
        // Storage keys are written to the database, so this guards against duplicate,
        // blank, or display-text-like values that would make future lookups fragile.
        val storageKeys = ProfileRankTitleId.entries.map { titleId -> titleId.storageKey }

        assertEquals(storageKeys.size, storageKeys.toSet().size)
        storageKeys.forEach { storageKey ->
            assertTrue(storageKey.isNotBlank())
            assertEquals(storageKey, storageKey.lowercase())
            assertTrue(storageKey.all { character -> character == '_' || character in 'a'..'z' })
        }
    }
}
