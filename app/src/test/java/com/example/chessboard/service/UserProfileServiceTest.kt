package com.example.chessboard.service

import com.example.chessboard.entity.UserProfileEntity
import com.example.chessboard.repository.UserProfileDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileServiceTest {

    // Verifies the persisted prompt settings start in the non-intrusive default state.
    @Test
    fun `getProfile returns default simple view prompt settings when profile is missing`() = runBlocking {
        val service = UserProfileService(FakeUserProfileDao())

        val profile = service.getProfile()

        assertFalse(profile.disableSimpleViewUpgradePrompt)
        assertEquals(SimpleViewUpgradePromptIntervalDefault, profile.simpleViewUpgradePromptInterval)
    }

    // Verifies both prompt settings are saved together so the settings screen cannot drift.
    @Test
    fun `updateSimpleViewUpgradePromptSettings persists disabled flag and interval`() = runBlocking {
        val dao = FakeUserProfileDao(UserProfileEntity())
        val service = UserProfileService(dao)

        service.updateSimpleViewUpgradePromptSettings(
            disabled = true,
            interval = 50,
        )

        val profile = service.getProfile()
        assertTrue(profile.disableSimpleViewUpgradePrompt)
        assertEquals(50, profile.simpleViewUpgradePromptInterval)
    }

    // Verifies invalid values from future callers are clamped before reaching storage.
    @Test
    fun `updateSimpleViewUpgradePromptSettings clamps interval`() = runBlocking {
        val dao = FakeUserProfileDao(UserProfileEntity())
        val service = UserProfileService(dao)

        service.updateSimpleViewUpgradePromptSettings(
            disabled = false,
            interval = 1,
        )
        assertEquals(SimpleViewUpgradePromptIntervalMin, service.getProfile().simpleViewUpgradePromptInterval)

        service.updateSimpleViewUpgradePromptSettings(
            disabled = false,
            interval = 200,
        )
        assertEquals(SimpleViewUpgradePromptIntervalMax, service.getProfile().simpleViewUpgradePromptInterval)
    }

    private class FakeUserProfileDao(
        private var profile: UserProfileEntity? = null,
    ) : UserProfileDao {
        override suspend fun getProfile(): UserProfileEntity? {
            return profile
        }

        override suspend fun upsertProfile(profile: UserProfileEntity) {
            this.profile = profile
        }
    }
}
