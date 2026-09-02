package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.TransactionEntity
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            settingsDao = db.settingsDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test app name string is Wealth Tracker`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Wealth Tracker", appName)
    }

    @Test
    fun `test dynamic net worth and portfolio calculations from single source of truth`() = runBlocking {
        // 1. Insert Asset category (included in portfolio)
        val stockCatId = repository.insertCategory(
            CategoryEntity(
                name = "Stock Portfolio",
                type = CategoryType.ASSET,
                showInPortfolio = true,
                iconName = "show_chart",
                colorHex = 0xFF10B981
            )
        )

        // 2. Insert Asset category (NOT in portfolio, e.g. Cash)
        val cashCatId = repository.insertCategory(
            CategoryEntity(
                name = "Emergency Cash",
                type = CategoryType.ASSET,
                showInPortfolio = false,
                iconName = "payments",
                colorHex = 0xFF059669
            )
        )

        // 3. Insert Liability category
        val loanCatId = repository.insertCategory(
            CategoryEntity(
                name = "Car Loan",
                type = CategoryType.LIABILITY,
                showInPortfolio = false,
                iconName = "directions_car",
                colorHex = 0xFFEF4444
            )
        )

        // 4. Insert Expense category
        val foodCatId = repository.insertCategory(
            CategoryEntity(
                name = "Groceries",
                type = CategoryType.EXPENSE,
                showInPortfolio = false,
                iconName = "restaurant",
                colorHex = 0xFFD97706
            )
        )

        val now = System.currentTimeMillis()

        // Insert transactions
        repository.insertTransaction(TransactionEntity(categoryId = stockCatId, amount = 10000.0, date = now, notes = "Index fund"))
        repository.insertTransaction(TransactionEntity(categoryId = cashCatId, amount = 5000.0, date = now, notes = "Checking account"))
        repository.insertTransaction(TransactionEntity(categoryId = loanCatId, amount = 3000.0, date = now, notes = "Auto financing"))
        repository.insertTransaction(TransactionEntity(categoryId = foodCatId, amount = 450.0, date = now, notes = "Weekly food"))

        // Check Dashboard Data
        val dashboard = repository.dashboardData.first()
        assertEquals(15000.0, dashboard.totalAssets, 0.001)
        assertEquals(3000.0, dashboard.totalLiabilities, 0.001)
        assertEquals(12000.0, dashboard.netWorth, 0.001) // 15000 - 3000

        // Check Portfolio Data: ONLY Stock Portfolio should be in visibleAssetCategories
        val portfolio = repository.portfolioData.first()
        assertEquals(10000.0, portfolio.totalPortfolioValue, 0.001)
        assertEquals(1, portfolio.visibleAssetCategories.size)
        assertEquals("Stock Portfolio", portfolio.visibleAssetCategories[0].category.name)

        // Test Backup Export & Restore
        val backupJson = repository.exportBackupJson()
        assertTrue(backupJson.contains("Stock Portfolio"))
        assertTrue(backupJson.contains("10000.0"))

        val restoreResult = repository.validateAndRestoreBackupJson(backupJson)
        assertTrue(restoreResult.isSuccess)

        val restoredDashboard = repository.dashboardData.first()
        assertEquals(12000.0, restoredDashboard.netWorth, 0.001)
    }
}
