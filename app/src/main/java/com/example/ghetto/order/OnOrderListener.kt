package com.example.ghetto.order

import com.example.ghetto.entities.Order

interface OnOrderListener {
    fun onTrack(order : Order)
    fun onStartChat(order : Order)
}