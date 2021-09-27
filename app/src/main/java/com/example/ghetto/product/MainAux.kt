package com.example.ghetto.product

import com.example.ghetto.entities.Product

interface MainAux {
    fun getProductsCart(): MutableList<Product>
    fun getProductSelected() : Product?
    fun showButton(isVisible : Boolean)
    fun addProductToCart(product: Product)
    fun updateTotal()
    fun clearCart()
}