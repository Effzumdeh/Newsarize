package com.example.newsarize.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.newsarize.data.local.dao.ArticleDao
import com.example.newsarize.data.local.dao.FeedSourceDao
import com.example.newsarize.data.local.entity.ArticleEntity
import com.example.newsarize.data.local.entity.FeedSourceEntity

@Database(
    entities = [ArticleEntity::class, FeedSourceEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun feedSourceDao(): FeedSourceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "newsarize_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
