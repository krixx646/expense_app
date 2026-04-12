package com.chelumore.xpenze.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `auto_capture_drafts` (
                    `id` TEXT NOT NULL,
                    `source` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `confidence` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `rawText` TEXT,
                    `sourcePackage` TEXT,
                    `sourceLabel` TEXT,
                    `amountCents` INTEGER,
                    `currency` TEXT NOT NULL,
                    `occurredAt` INTEGER NOT NULL,
                    `suggestedType` TEXT,
                    `accountId` TEXT,
                    `toAccountId` TEXT,
                    `categoryId` TEXT,
                    `notes` TEXT,
                    `receiptUri` TEXT,
                    `tags` TEXT NOT NULL,
                    `needsReviewReason` TEXT,
                    `fingerprint` TEXT NOT NULL,
                    `resolution` TEXT NOT NULL,
                    `createdTransactionId` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_auto_capture_drafts_status` ON `auto_capture_drafts` (`status`)"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_auto_capture_drafts_occurredAt` ON `auto_capture_drafts` (`occurredAt`)"
            )
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_auto_capture_drafts_fingerprint` ON `auto_capture_drafts` (`fingerprint`)"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE `budgets` ADD COLUMN `isRecurring` INTEGER NOT NULL DEFAULT 0"
            )
            database.execSQL(
                "ALTER TABLE `budgets` ADD COLUMN `seriesId` TEXT"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_budgets_seriesId` ON `budgets` (`seriesId`)"
            )
        }
    }
}
