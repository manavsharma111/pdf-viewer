package com.antigravity.pdfviewer.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.antigravity.pdfviewer.data.local.dao.AnnotationDao
import com.antigravity.pdfviewer.data.local.dao.BookmarkDao
import com.antigravity.pdfviewer.data.local.dao.RecentDocumentDao
import com.antigravity.pdfviewer.data.local.entity.AnnotationEntity
import com.antigravity.pdfviewer.data.local.entity.BookmarkEntity
import com.antigravity.pdfviewer.data.local.entity.RecentDocumentEntity

@Database(
    entities = [
        RecentDocumentEntity::class,
        BookmarkEntity::class,
        AnnotationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recentDocumentDao(): RecentDocumentDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun annotationDao(): AnnotationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pdf_viewer_pro.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
