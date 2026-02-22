package com.example.newsarize.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey

@Entity(
    tableName = "articles",
    indices = [
        Index(value = ["link"], unique = true),
        Index(value = ["feedId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = FeedSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["feedId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val feedId: Int,
    val title: String,
    val link: String,
    val content: String,
    val pubDate: Long,
    val summary: String? = null, // Filled after LLM inference
    val isRead: Boolean = false
)
