package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Entities
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val unit: String,
    val currentStock: Int = 0
)

enum class TransactionType { IN, OUT }

@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(
        entity = ItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["itemId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("itemId")]
)
data class StockTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemId: Int,
    val type: TransactionType,
    val quantity: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class TransactionWithItem(
    @Embedded val transaction: StockTransactionEntity,
    @Relation(
        parentColumn = "itemId",
        entityColumn = "id"
    )
    val item: ItemEntity
)

// DAO
@Dao
interface StockDao {
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Query("UPDATE items SET currentStock = currentStock + :amount WHERE id = :itemId")
    suspend fun updateStock(itemId: Int, amount: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransactionEntity)

    @Transaction
    suspend fun addTransaction(transaction: StockTransactionEntity) {
        insertTransaction(transaction)
        val amount = if (transaction.type == TransactionType.IN) transaction.quantity else -transaction.quantity
        updateStock(transaction.itemId, amount)
    }

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 30")
    fun getRecentTransactions(): Flow<List<TransactionWithItem>>
}

// Database
@Database(entities = [ItemEntity::class, StockTransactionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stock_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Repository
class StockRepository(private val stockDao: StockDao) {
    val allItems: Flow<List<ItemEntity>> = stockDao.getAllItems()
    val recentTransactions: Flow<List<TransactionWithItem>> = stockDao.getRecentTransactions()

    suspend fun insertItem(item: ItemEntity) = stockDao.insertItem(item)
    suspend fun recordTransaction(transaction: StockTransactionEntity) = stockDao.addTransaction(transaction)
}
