package com.example.newsarize.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "articles",
    indices = [Index(value = ["link"], unique = true)]
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val feedId: Int,
    val title: String,
    val link: String,
    val content: String,
    val pubDate: Long,
    val summary: String? = null // Filled after ONNX inference
)
