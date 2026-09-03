package com.ronydiaz.dockerlearn.models

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val colorHex: String = "#3498db"
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val colorHex: String = "#3498db"
)
