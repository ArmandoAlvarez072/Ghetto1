package com.example.ghetto.chat

import com.example.ghetto.entities.Message

interface OnChatListener {
    fun deleteMessage(message: Message)
}