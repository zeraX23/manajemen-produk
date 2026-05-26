package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StockViewModel(private val repository: StockRepository) : ViewModel() {
    val items: StateFlow<List<ItemEntity>> = repository.allItems.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val transactions: StateFlow<List<TransactionWithItem>> = repository.recentTransactions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun addItem(name: String, unit: String) {
        if (name.isBlank() || unit.isBlank()) return
        viewModelScope.launch {
            repository.insertItem(ItemEntity(name = name.trim(), unit = unit.trim()))
        }
    }

    fun recordTransaction(itemId: Int, type: TransactionType, quantity: Int) {
        if (quantity <= 0) return
        viewModelScope.launch {
            repository.recordTransaction(
                StockTransactionEntity(itemId = itemId, type = type, quantity = quantity)
            )
        }
    }
}

class StockViewModelFactory(private val repository: StockRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
