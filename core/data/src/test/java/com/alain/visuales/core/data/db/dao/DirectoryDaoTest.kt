package com.alain.visuales.core.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alain.visuales.core.data.db.VisualesDatabase
import com.alain.visuales.core.data.db.entity.DirectoryEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DirectoryDaoTest {

    private lateinit var db: VisualesDatabase
    private lateinit var dao: DirectoryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, VisualesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.directoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun makeDir(path: String, parent: String, name: String, depth: Int = 1): DirectoryEntity {
        return DirectoryEntity(
            path = path,
            url = "https://visuales.uclv.cu$path",
            name = name,
            parentPath = parent,
            depth = depth,
            section = path.trim('/').substringBefore('/'),
            subcategory = null,
            childCount = 0,
            addedAt = System.currentTimeMillis(),
            lastSeenAt = System.currentTimeMillis(),
            isStale = false
        )
    }

    @Test
    fun `upsert and getByPath works`() = runTest {
        val dir = makeDir("/Peliculas/", "/", "Peliculas")
        dao.upsert(dir)
        val fetched = dao.getByPath("/Peliculas/")
        assertThat(fetched).isNotNull()
        assertThat(fetched!!.name).isEqualTo("Peliculas")
    }

    @Test
    fun `children returns directories with matching parent`() = runTest {
        val parent = makeDir("/Peliculas/", "/", "Peliculas")
        val child1 = makeDir("/Peliculas/Cubanas/", "/Peliculas", "Cubanas", 2)
        val child2 = makeDir("/Peliculas/Extranjeras/", "/Peliculas", "Extranjeras", 2)
        dao.upsertAll(listOf(parent, child1, child2))

        val children = dao.children("/Peliculas").first()
        assertThat(children).hasSize(2)
        val names = children.map { it.name }
        assertThat(names).containsExactly("Cubanas", "Extranjeras")
    }

    @Test
    fun `children returns empty list when no matches`() = runTest {
        dao.upsert(makeDir("/Peliculas/", "/", "Peliculas"))
        val children = dao.children("/Cursos").first()
        assertThat(children).isEmpty()
    }

    @Test
    fun `children sorts alphabetically`() = runTest {
        dao.upsertAll(
            listOf(
                makeDir("/C/", "/", "C"),
                makeDir("/A/", "/", "A"),
                makeDir("/B/", "/", "B")
            )
        )
        val children = dao.children("/").first()
        assertThat(children.map { it.name }).containsExactly("A", "B", "C").inOrder()
    }

    @Test
    fun `children is case-insensitive`() = runTest {
        dao.upsertAll(
            listOf(
                makeDir("/B/", "/", "banana"),
                makeDir("/A/", "/", "Apple")
            )
        )
        val children = dao.children("/").first()
        assertThat(children.first().name).isEqualTo("Apple")
    }

    @Test
    fun `search finds by substring`() = runTest {
        dao.upsertAll(
            listOf(
                makeDir("/Peliculas/Cubanas/Viva Cuba/", "/Peliculas/Cubanas", "Viva Cuba", 3),
                makeDir("/Peliculas/Cubanas/Memorias/", "/Peliculas/Cubanas", "Memorias", 3),
                makeDir("/Series/NETFLIX/Squid Game/", "/Series/NETFLIX", "Squid Game", 3)
            )
        )
        val results = dao.search("Cuba").first()
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("Viva Cuba")
    }

    @Test
    fun `search is case-insensitive`() = runTest {
        dao.upsert(makeDir("/Peliculas/Cubanas/VIVA CUBA/", "/Peliculas/Cubanas", "VIVA CUBA", 3))
        val results = dao.search("cuba").first()
        assertThat(results).hasSize(1)
    }

    @Test
    fun `search with blank query returns all entries`() = runTest {
        dao.upsert(makeDir("/Peliculas/", "/", "Peliculas"))
        // LIKE '%%' matches everything — this is expected SQL behavior.
        val results = dao.search("").first()
        assertThat(results).hasSize(1)
    }

    @Test
    fun `recentlyAdded returns most recent first`() = runTest {
        val old = makeDir("/A/", "/", "A").copy(addedAt = 1000L)
        val new = makeDir("/B/", "/", "B").copy(addedAt = 2000L)
        val newest = makeDir("/C/", "/", "C").copy(addedAt = 3000L)
        dao.upsertAll(listOf(old, new, newest))

        val recent = dao.recentlyAdded(2).first()
        assertThat(recent).hasSize(2)
        assertThat(recent[0].name).isEqualTo("C")
        assertThat(recent[1].name).isEqualTo("B")
    }

    @Test
    fun `recentlyAdded respects limit`() = runTest {
        val dirs = (1..10).map { i ->
            makeDir("/$i/", "/", i.toString()).copy(addedAt = i.toLong())
        }
        dao.upsertAll(dirs)
        val recent = dao.recentlyAdded(3).first()
        assertThat(recent).hasSize(3)
    }

    @Test
    fun `count returns total number of directories`() = runTest {
        dao.upsertAll(
            listOf(
                makeDir("/A/", "/", "A"),
                makeDir("/B/", "/", "B"),
                makeDir("/C/", "/", "C")
            )
        )
        assertThat(dao.count()).isEqualTo(3)
    }

    @Test
    fun `countAddedSince returns only newer entries`() = runTest {
        val now = System.currentTimeMillis()
        val old = makeDir("/A/", "/", "A").copy(addedAt = now - 100_000)
        val new1 = makeDir("/B/", "/", "B").copy(addedAt = now - 1_000)
        val new2 = makeDir("/C/", "/", "C").copy(addedAt = now)
        dao.upsertAll(listOf(old, new1, new2))

        assertThat(dao.countAddedSince(now - 50_000)).isEqualTo(2)
    }

    @Test
    fun `markStale sets isStale for non-alive paths`() = runTest {
        val a = makeDir("/A/", "/", "A")
        val b = makeDir("/B/", "/", "B")
        val c = makeDir("/C/", "/", "C")
        dao.upsertAll(listOf(a, b, c))

        // Mark A as alive, B and C as stale.
        dao.markStale(listOf("/A/"))
        dao.deleteStale()

        val remaining = dao.count()
        assertThat(remaining).isEqualTo(1)
        assertThat(dao.getByPath("/A/")).isNotNull()
    }

    @Test
    fun `syncReplaceAll upserts all entries and removes stale ones`() = runTest {
        dao.upsertAll(
            listOf(
                makeDir("/A/", "/", "A"),
                makeDir("/B/", "/", "B")
            )
        )
        // Sync again with only A — B should be removed.
        dao.syncReplaceAll(listOf(makeDir("/A/", "/", "A")), System.currentTimeMillis())

        assertThat(dao.count()).isEqualTo(1)
        assertThat(dao.getByPath("/A/")).isNotNull()
        assertThat(dao.getByPath("/B/")).isNull()
    }

    @Test
    fun `getAll returns all directories`() = runTest {
        dao.upsertAll(
            listOf(
                makeDir("/A/", "/", "A"),
                makeDir("/B/", "/", "B")
            )
        )
        val all = dao.getAll()
        assertThat(all).hasSize(2)
    }

    @Test
    fun `clearAll removes everything`() = runTest {
        dao.upsertAll(
            listOf(
                makeDir("/A/", "/", "A"),
                makeDir("/B/", "/", "B")
            )
        )
        dao.clearAll()
        assertThat(dao.count()).isEqualTo(0)
    }
}
