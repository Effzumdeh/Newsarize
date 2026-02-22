package com.example.newsarize.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.newsarize.data.local.entity.FeedSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedSourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedSource(feedSource: FeedSourceEntity)

    @Delete
    suspend fun deleteFeedSource(feedSource: FeedSourceEntity)

    @Query("SELECT * FROM feed_sources")
    fun getAllFeedSourcesFlow(): Flow<List<FeedSourceEntity>>

    @Query("SELECT * FROM feed_sources")
    suspend fun getAllFeedSources(): List<FeedSourceEntity>
}
