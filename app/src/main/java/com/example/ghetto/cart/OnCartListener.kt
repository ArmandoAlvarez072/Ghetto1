package com.example.ghetto.cart

import com.example.ghetto.entities.Product

interface OnCartListener {
    fun setQuantity(product: Product)
    fun showTotal(total: Double)
}