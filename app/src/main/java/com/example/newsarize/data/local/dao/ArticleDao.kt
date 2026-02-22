package com.example.newsarize.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.newsarize.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("SELECT * FROM articles ORDER BY pubDate DESC")
    fun getAllArticlesFlow(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE pubDate >= :startOfDay ORDER BY pubDate DESC")
    fun getArticlesSinceFlow(startOfDay: Long): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE summary IS NULL")
    suspend fun getUnsummarizedArticles(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE summary IS NULL ORDER BY pubDate DESC LIMIT 1")
    suspend fun getNextUnsummarizedArticle(): ArticleEntity?

    @Update
    suspend fun updateArticle(article: ArticleEntity)
}
