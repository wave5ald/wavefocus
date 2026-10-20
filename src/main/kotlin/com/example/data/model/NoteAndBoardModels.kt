package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val boardId: Long? = null, // null for standalone notes, or board ID for notes on a board
    val title: String = "",
    val content: String = "",
    val imageUri: String? = null,
    val colorHex: String = "#1E293B", // Card background
    val textColorHex: String = "#F8FAFC", // Text color
    val posX: Float = 40f, // Canvas position inside board
    val posY: Float = 40f,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "boards")
data class BoardItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val backgroundColorHex: String = "#0F172A",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "board_elements")
data class BoardElement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val boardId: Long,
    val elementType: String, // "BUTTON", "TEXT", "IMAGE"
    val text: String = "", // Button label or text content
    val colorHex: String = "#3B82F6", // Button or card tint
    val textColorHex: String = "#FFFFFF", // Text color
    val linkedNoteIds: String = "", // Comma-separated note IDs to show when button is tapped
    val imageUri: String? = null,
    val posX: Float = 100f,
    val posY: Float = 100f,
    val createdAt: Long = System.currentTimeMillis()
)
