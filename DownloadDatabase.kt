package com.edm.downloadmanager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)

    @TypeConverter
    fun fromCategory(category: FileCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): FileCategory = FileCategory.valueOf(value)
}

@Database(
    entities = [DownloadEntity::class, DownloadSegmentEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile private var INSTANCE: DownloadDatabase? = null

        fun getInstance(context: Context): DownloadDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DownloadDatabase::class.java,
                    "edm_downloads.db"
                ).fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
