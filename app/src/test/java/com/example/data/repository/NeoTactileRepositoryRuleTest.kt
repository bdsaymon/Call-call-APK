package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
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
    fun crossUserProfileWrite_nonOwner_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = NeoTactileRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureOrUpdateUserProfile(
                email = ALICE_EMAIL,
                displayName = "Alice Tactile"
            ).getOrThrow()
        }

        // Bob attempts to overwrite Alice's user profile document -> must fail with PERMISSION_DENIED
        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .update("displayName", "Hijacked By Bob")
                    .await()
            }
            fail("Expected PERMISSION_DENIED when Bob updates Alice's user document")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertNotNull(fsError)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
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
    fun createGroupAndCommunity_alignedMembers_emitsRealtimeAcrossDevices() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bobUid = signInTestUser(BOB_EMAIL)

        signInTestUser(ALICE_EMAIL)
        val aliceRepo = NeoTactileRepository(firestore)

        val groupProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createGroupContact(
                groupName = "Engineering Team",
                memberIds = listOf(aliceUid, bobUid),
                description = "Multi-device sync test"
            ).getOrThrow()
        }

        val commDoc = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCommunity(
                name = "Android Architects",
                description = "Real Firestore Community",
                announcement = "Welcome to v2.0",
                initialGroupNames = listOf("General"),
                memberIds = listOf(aliceUid, bobUid)
            ).getOrThrow()
        }

        // Switch to Bob's session (Device B) and verify Bob sees both the Group and Community
        signInTestUser(BOB_EMAIL)
        val bobRepo = NeoTactileRepository(firestore)

        val bobGroups = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeMyGroups().first { list -> list.any { it.groupId == groupProfile.userId } }
        }
        assertTrue(bobGroups.any { it.groupId == groupProfile.userId && it.name == "Engineering Team" })

        val bobCommunities = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeCommunities().first { list -> list.any { it.communityId == commDoc.communityId } }
        }
        assertTrue(bobCommunities.any { it.communityId == commDoc.communityId && it.name == "Android Architects" })
    }

    @Test
    fun initiateCallSession_emitsRealtimeSignalingToCallee() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bobUid = signInTestUser(BOB_EMAIL)

        signInTestUser(ALICE_EMAIL)
        val aliceRepo = NeoTactileRepository(firestore)

        val callSession = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendWebRtcCallSignal(
                peerId = bobUid,
                peerName = "Bob",
                senderName = "Alice",
                callType = "VIDEO",
                signalState = "RINGING"
            ).getOrThrow()
        }

        // Switch to Bob (Callee Device B) and verify incoming call is observed
        signInTestUser(BOB_EMAIL)
        val bobRepo = NeoTactileRepository(firestore)

        val bobCalls = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeCallLogs().first { list -> list.any { it.callId == callSession.callId } }
        }
        assertTrue(bobCalls.any { it.callId == callSession.callId && it.status == "RINGING" })
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
            assertTrue(e is IllegalStateException || e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
        }
    }

    @Test
    fun registerDeviceHeartbeat_newHardwareId_readsNonExistentAndCreatesDocument() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = NeoTactileRepository(firestore)
        val hwId = "hw_${System.currentTimeMillis()}"

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
