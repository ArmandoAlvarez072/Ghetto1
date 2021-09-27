package com.example.ghetto.product

import com.example.ghetto.entities.Product

interface OnProductListener {
    fun onClick(product: Product)
}