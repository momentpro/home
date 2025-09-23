package com.example.groupmessenger.data.model

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val isSelected: Boolean = false
)

