package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.StockViewModel
import com.example.ui.StockViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.theme_green
import com.example.ui.theme.theme_greenContainer
import com.example.ui.theme.theme_red
import com.example.ui.theme.theme_redContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = AppDatabase.getDatabase(this)
        val repository = StockRepository(db.stockDao())
        val factory = StockViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: StockViewModel = viewModel(factory = factory)
                StockApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockApp(viewModel: StockViewModel) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    var showAddItemDialog by remember { mutableStateOf(false) }
    var activeTransactionItem by remember { mutableStateOf<ItemEntity?>(null) }
    var transactionType by remember { mutableStateOf(TransactionType.IN) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Manajemen Stok", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    )
                )
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Stok Barang", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Inventory, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Riwayat Transaksi", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.History, contentDescription = null) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { showAddItemDialog = true },
                    modifier = Modifier.testTag("add_item_fab"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Barang")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (selectedTabIndex == 0) {
                ItemsList(
                    items = items,
                    onAddTransaction = { item, type ->
                        activeTransactionItem = item
                        transactionType = type
                    }
                )
            } else {
                TransactionsList(transactions = transactions)
            }
        }
    }

    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, unit ->
                viewModel.addItem(name, unit)
                showAddItemDialog = false
            }
        )
    }

    activeTransactionItem?.let { item ->
        AddTransactionDialog(
            item = item,
            type = transactionType,
            onDismiss = { activeTransactionItem = null },
            onConfirm = { quantity ->
                viewModel.recordTransaction(item.id, transactionType, quantity)
                activeTransactionItem = null
            }
        )
    }
}

@Composable
fun ItemsList(
    items: List<ItemEntity>,
    onAddTransaction: (ItemEntity, TransactionType) -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Stok Kosong", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tambah barang baru menggunakan tombol +", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                ItemCard(item = item, onAddTransaction = onAddTransaction)
            }
        }
    }
}

@Composable
fun ItemCard(
    item: ItemEntity,
    onAddTransaction: (ItemEntity, TransactionType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Persediaan: ${item.currentStock} ${item.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(
                    onClick = { onAddTransaction(item, TransactionType.OUT) },
                    modifier = Modifier.size(40.dp).testTag("btn_out_${item.id}"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = theme_redContainer,
                        contentColor = theme_red
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Keluar")
                }
                FilledIconButton(
                    onClick = { onAddTransaction(item, TransactionType.IN) },
                    modifier = Modifier.size(40.dp).testTag("btn_in_${item.id}"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = theme_greenContainer,
                        contentColor = theme_green
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Masuk")
                }
            }
        }
    }
}

@Composable
fun TransactionsList(transactions: List<TransactionWithItem>) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada riwayat transaksi", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(transactions, key = { it.transaction.id }) { t ->
                val isMasuk = t.transaction.type == TransactionType.IN
                val timeString = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(t.transaction.timestamp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isMasuk) theme_greenContainer else theme_redContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMasuk) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isMasuk) theme_green else theme_red,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = t.item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(text = timeString, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "${if(isMasuk) "+" else "-"}${t.transaction.quantity} ${t.item.unit}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isMasuk) theme_green else theme_red
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Barang Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Barang") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_item_name")
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Satuan (Contoh: Pcs, Kg, Box)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_item_unit")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, unit) },
                enabled = name.isNotBlank() && unit.isNotBlank(),
                modifier = Modifier.testTag("btn_save_item")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun AddTransactionDialog(
    item: ItemEntity,
    type: TransactionType,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantityStr by remember { mutableStateOf("") }
    val isMasuk = type == TransactionType.IN
    val titleMsg = if (isMasuk) "Barang Masuk" else "Barang Keluar"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleMsg) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Barang: ${item.name}", fontWeight = FontWeight.Bold)
                Text(text = "Stok Saat Ini: ${item.currentStock} ${item.unit}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Jumlah (${item.unit})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_tx_qty")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    quantityStr.toIntOrNull()?.let { onConfirm(it) } 
                },
                enabled = quantityStr.toIntOrNull() != null && quantityStr.toIntOrNull()!! > 0,
                modifier = Modifier.testTag("btn_save_tx")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

