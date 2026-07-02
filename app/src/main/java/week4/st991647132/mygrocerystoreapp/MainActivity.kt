package week4.st991647132.mygrocerystoreapp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import week4.st991647132.mygrocerystoreapp.data.Product
import week4.st991647132.mygrocerystoreapp.ui.LoginScreen
import week4.st991647132.mygrocerystoreapp.ui.theme.MyGroceryStoreAppTheme
import week4.st991647132.mygrocerystoreapp.viewmodel.AuthViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)

        setContent {
            MyGroceryStoreAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val authViewModel: AuthViewModel = viewModel()
    val user by authViewModel.user.collectAsState()

    if (user != null) {
        // User is logged in - Show MainScreen with Logout
        MainScreenWithAuth(
            modifier = modifier,
            onLogout = { authViewModel.signOut() }
        )
    } else {
        // User is not logged in - Show Login Screen
        LoginScreen(
            modifier = modifier,
            authViewModel = authViewModel,
            onLoginSuccess = {}
        )
    }
}

// Function to save image to internal storage
fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, fileName)

        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        android.util.Log.d("SaveImage", "Image saved to: ${file.absolutePath}")
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithAuth(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // State for products list
    var products by remember { mutableStateOf(listOf<Product>()) }
    var isLoading by remember { mutableStateOf(true) }

    // State for Add/Edit Dialog
    var showProductDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    // Dialog form fields
    var productName by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var selectedImagePath by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            val savedPath = saveImageToInternalStorage(context, it)
            selectedImagePath = savedPath
            if (savedPath != null) {
                android.widget.Toast.makeText(
                    context,
                    "Image selected successfully!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.widget.Toast.makeText(
                    context,
                    "Failed to save image",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Real-time Firestore listener
    DisposableEffect(Unit) {
        val listener: ListenerRegistration = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("Firestore", "Listen failed: $error")
                    isLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val productList = snapshot.toObjects(Product::class.java)
                    products = productList
                    android.util.Log.d("Firestore", "Loaded ${products.size} products")
                    isLoading = false
                }
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "My Grocery Store",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Logout Button
                    TextButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Logout", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF000CFF),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isEditing = false
                    editingProduct = null
                    productName = ""
                    productDescription = ""
                    productPrice = ""
                    productCategory = ""
                    selectedImagePath = null
                    selectedImageUri = null
                    showProductDialog = true
                },
                containerColor = Color(0xFF000CFF)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF000CFF))
                }
            } else if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No products yet!\nTap the + button to add items",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            context = context,
                            onDelete = {
                                db.collection("products")
                                    .document(product.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        if (product.imagePath.isNotEmpty()) {
                                            File(product.imagePath).delete()
                                        }
                                        android.widget.Toast.makeText(
                                            context,
                                            "${product.name} deleted",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener { e ->
                                        android.widget.Toast.makeText(
                                            context,
                                            "Delete failed: ${e.message}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            },
                            onEdit = {
                                isEditing = true
                                editingProduct = product
                                productName = product.name
                                productDescription = product.description
                                productPrice = product.price
                                productCategory = product.category
                                selectedImagePath = product.imagePath.ifEmpty { null }
                                selectedImageUri = null
                                showProductDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Add/Edit Product Dialog
        if (showProductDialog) {
            Dialog(onDismissRequest = { showProductDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = if (isEditing) "Edit Product" else "➕ Add New Product",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF000CFF)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Image Selection Section
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedImageUri != null || selectedImagePath != null) {
                                    val imageFile = selectedImagePath?.let { File(it) }
                                    if (imageFile?.exists() == true) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(imageFile)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Selected image",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (selectedImageUri != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(selectedImageUri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Selected image",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(File(selectedImagePath ?: ""))
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Existing image",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            selectedImageUri = null
                                            selectedImagePath = null
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Remove",
                                            tint = Color.Red,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Button(
                                            onClick = { imagePickerLauncher.launch("image/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF000CFF))
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Select Image from Gallery")
                                        }
                                        Text(
                                            text = "Tap to choose a photo from your device",
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = productName,
                            onValueChange = { productName = it },
                            label = { Text("Product Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = productDescription,
                            onValueChange = { productDescription = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = productPrice,
                            onValueChange = { productPrice = it },
                            label = { Text("Price * (e.g., 1.99)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = productCategory,
                            onValueChange = { productCategory = it },
                            label = { Text("Category") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showProductDialog = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    if (productName.isNotBlank() && productPrice.isNotBlank()) {
                                        if (isEditing && editingProduct != null) {
                                            val imageToSave = when {
                                                selectedImagePath != null -> selectedImagePath!!
                                                editingProduct?.imagePath?.isNotEmpty() == true -> editingProduct!!.imagePath
                                                else -> ""
                                            }

                                            val updatedProduct = Product(
                                                name = productName,
                                                description = productDescription,
                                                price = productPrice,
                                                category = productCategory,
                                                imagePath = imageToSave,
                                                id = editingProduct!!.id
                                            )

                                            db.collection("products")
                                                .document(editingProduct!!.id)
                                                .set(updatedProduct)
                                                .addOnSuccessListener {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Product updated!",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                    showProductDialog = false
                                                }
                                                .addOnFailureListener { e ->
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Update failed: ${e.message}",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        } else {
                                            if (selectedImagePath == null) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Please select an image first!",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                                return@Button
                                            }

                                            val newProduct = Product(
                                                name = productName,
                                                description = productDescription,
                                                price = productPrice,
                                                category = productCategory,
                                                imagePath = selectedImagePath!!
                                            )

                                            db.collection("products")
                                                .add(newProduct)
                                                .addOnSuccessListener {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Product added successfully!",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                    showProductDialog = false
                                                }
                                                .addOnFailureListener { e ->
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Add failed: ${e.message}",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                        }
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Please fill in Name and Price",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = if (!isEditing) selectedImagePath != null else true,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0056FF),
                                    disabledContainerColor = Color(0xFFA5D6A7)
                                )
                            ) {
                                Text(if (isEditing) "Update" else "Save")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    context: Context,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            val imageFile = if (product.imagePath.isNotEmpty()) File(product.imagePath) else null
            val imageExists = imageFile?.exists() == true

            if (imageExists) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Product Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                if (product.description.isNotBlank()) {
                    Text(
                        text = product.description,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 2
                    )
                }

                Text(
                    text = "$${product.price}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5058DC)
                )

                if (product.category.isNotBlank()) {
                    Text(
                        text = "Category: ${product.category}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // EDIT Button
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF000CFF)),
                    modifier = Modifier.width(70.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Edit", fontSize = 12.sp, color = Color.White)
                }

                // DELETE Button
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.width(90.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Delete", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
