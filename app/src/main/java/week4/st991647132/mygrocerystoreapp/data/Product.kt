package week4.st991647132.mygrocerystoreapp.data

import com.google.firebase.firestore.DocumentId

data class Product(
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val category: String = "",
    val imagePath: String = "",

    @DocumentId
    val id: String = ""
)