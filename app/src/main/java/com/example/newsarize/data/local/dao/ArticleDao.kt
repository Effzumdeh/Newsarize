package com.example.newsarize.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.newsarize.data.local.entity.ArticleEntity
import com.example.newsarize.data.local.entity.ArticleUiModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<ArticleEntity>): List<Long>

    @Query("SELECT id, feedId, title, link, pubDate, summary, isRead, category FROM articles ORDER BY pubDate DESC")
    fun getAllArticlesFlow(): Flow<List<ArticleUiModel>>

    @Query("SELECT id, feedId, title, link, pubDate, summary, isRead, category FROM articles WHERE (:feedId IS NULL OR feedId = :feedId) AND (:filterState = 'ALL' OR (:filterState = 'READ' AND isRead = 1) OR (:filterState = 'UNREAD' AND isRead = 0)) AND (:category IS NULL OR category = :category) ORDER BY pubDate DESC")
    fun getFilteredArticlesFlow(feedId: Int?, filterState: String, category: String?): Flow<List<ArticleUiModel>>

    @Query("SELECT id, feedId, title, link, pubDate, summary, isRead, category FROM articles WHERE pubDate >= :startOfDay ORDER BY pubDate DESC")
    fun getArticlesSinceFlow(startOfDay: Long): Flow<List<ArticleUiModel>>

    @Query("SELECT * FROM articles WHERE summary IS NULL OR category IS NULL")
    suspend fun getUnprocessedArticles(): List<ArticleEntity>

    @Query("SELECT id FROM articles WHERE summary IS NULL OR category IS NULL ORDER BY pubDate DESC LIMIT 1")
    suspend fun getNextUnprocessedArticleId(): Int?

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: Int): ArticleEntity?

    @Update
    suspend fun updateArticle(article: ArticleEntity)
    
    @Query("UPDATE articles SET isRead = :isRead WHERE id = :articleId")
    suspend fun updateArticleReadStatus(articleId: Int, isRead: Boolean)

    @Query("SELECT DISTINCT category FROM articles WHERE category IS NOT NULL")
    fun getUsedCategoriesFlow(): Flow<List<String>>
}
