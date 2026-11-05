package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NeoTactileRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureOrUpdateUserProfile_authenticatedOwner_createsAndReadsOwnProfile() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NeoTactileRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureOrUpdateUserProfile(
                email = ALICE_EMAIL,
                displayName = "Alice Tactile",
                bio = "Hardware Synth Lead"
            )
        }
        assertTrue(saveResult.isSuccess)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfileById(aliceUid)
        }
        assertTrue(profileResult.isSuccess)
        assertEquals("Alice Tactile", profileResult.getOrThrow()?.displayName)
    }

    @Test
    fun getUserProfileById_crossUserPiiAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = NeoTactileRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureOrUpdateUserProfile(
                email = ALICE_EMAIL,
                displayName = "Alice Tactile"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = NeoTactileRepository(firestore)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getUserProfileById(aliceUid)
        }
        assertTrue(crossReadResult.isFailure)
        val fsError = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsError)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
    }

    @Test
    fun sendMessageAndObserve_alignedParticipants_emitsRealtimeMessage() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bobUid = signInTestUser(BOB_EMAIL)

        signInTestUser(ALICE_EMAIL)
        val aliceRepo = NeoTactileRepository(firestore)
        val chatId = aliceRepo.buildConversationId(aliceUid, bobUid)

        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendMessage(
                chatId = chatId,
                receiverId = bobUid,
                senderName = "Alice",
                content = "Encrypted Neo-Tactile Ping"
            ).getOrThrow()
        }

        val emittedMessages = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeMessages(chatId).first { list -> list.any { it.messageId == msgId } }
        }
        assertTrue(emittedMessages.any { it.messageId == msgId && it.content == "Encrypted Neo-Tactile Ping" })
    }

    @Test
    fun publishAndObserveStories_alignedVisibleToUids_emitsActiveStory() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = NeoTactileRepository(firestore)

        val storyId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.publishStory(
                authorName = "Alice",
                authorAvatarUrl = null,
                mediaUrl = "preset://cyan_pulse",
                caption = "24h Pulse Test"
            ).getOrThrow()
        }

        val emittedStories = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeActiveStories().first { list -> list.any { it.storyId == storyId } }
        }
        assertTrue(emittedStories.any { it.storyId == storyId })
    }

    @Test
    fun observeActiveStories_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = NeoTactileRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeActiveStories().first()
            }
            fail("Expected exception for unauthenticated user")
        } catch (e: Exception) {
            // Expected failure (IllegalStateException from requireUserId() or PERMISSION_DENIED)
            assertTrue(e is IllegalStateException || e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
        }
    }

    @Test
    fun registerDeviceHeartbeat_newHardwareId_readsNonExistentAndCreatesDocument() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = NeoTactileRepository(firestore)
        val hwId = "104530351a2b137a"

        val initialGatekeeper = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDeviceGatekeeper(hwId).first()
        }
        assertEquals(null, initialGatekeeper)

        val heartbeatResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.registerDeviceHeartbeat(hwId, "Cloud Streaming Emulator")
        }
        assertTrue(heartbeatResult.isSuccess)

        val registered = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDeviceGatekeeper(hwId).first { it != null }
        }
        assertNotNull(registered)
        assertEquals(false, registered?.banned)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_neo@test.com"
        const val BOB_EMAIL = "bob_neo@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
