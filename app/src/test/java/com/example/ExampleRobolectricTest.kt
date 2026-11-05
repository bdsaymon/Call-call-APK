package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalAliasEntity
import com.example.data.local.NeoTactileDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NeoTactile Chat", appName)
    }

    @Test
    fun `room local alias override persists locally`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, NeoTactileDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.localAliasDao()

        dao.upsertAlias(
            LocalAliasEntity(
                targetUserId = "peer_nova_01",
                overriddenDisplayName = "Custom Tactile Alias",
                overriddenAvatarUri = "preset://avatar_cyan"
            )
        )

        val fetched = dao.observeAliasForUser("peer_nova_01").first()
        assertEquals("Custom Tactile Alias", fetched?.overriddenDisplayName)
        db.close()
    }
}
