package dev.staticvar.vlr.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.staticvar.vlr.data.dao.VlrDao
import dev.staticvar.vlr.data.db.VlrDB
import org.junit.After
import org.junit.Before

class VlrRepositoryTest {

  private lateinit var vlrDao: VlrDao
  private lateinit var db: VlrDB

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, VlrDB::class.java).build()
    vlrDao = db.getVlrDao()
  }

  @After
  fun tearDown() {
    db.close()
  }

//  val repository = VlrRepository(vlrDao, )
}
