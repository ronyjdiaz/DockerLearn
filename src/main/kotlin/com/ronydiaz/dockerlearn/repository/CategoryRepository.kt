package com.ronydiaz.dockerlearn.repository

import com.ronydiaz.dockerlearn.database.CategoriesTable
import com.ronydiaz.dockerlearn.models.Category
import com.ronydiaz.dockerlearn.models.CreateCategoryRequest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

interface CategoryRepository {
    fun allCategories(): List<Category>
    fun categoryById(id: String): Category?
    fun addCategory(request: CreateCategoryRequest): Category
    fun deleteCategory(id: String): Boolean
}

class PostgresCategoryRepository : CategoryRepository {

    private fun resultRowToCategory(row: ResultRow) = Category(
        id = row[CategoriesTable.id],
        name = row[CategoriesTable.name],
        colorHex = row[CategoriesTable.colorHex]
    )

    override fun allCategories(): List<Category> = transaction {
        CategoriesTable.selectAll().map(::resultRowToCategory)
    }

    override fun categoryById(id: String): Category? = transaction {
        CategoriesTable.selectAll()
            .where { CategoriesTable.id eq id }
            .map(::resultRowToCategory)
            .singleOrNull()
    }

    override fun addCategory(request: CreateCategoryRequest): Category {
        val newId = UUID.randomUUID().toString()
        transaction {
            CategoriesTable.insert {
                it[id] = newId
                it[name] = request.name
                it[colorHex] = request.colorHex
            }
        }
        return Category(
            id = newId,
            name = request.name,
            colorHex = request.colorHex
        )
    }

    override fun deleteCategory(id: String): Boolean = transaction {
        CategoriesTable.deleteWhere { CategoriesTable.id eq id } > 0
    }
}

class InMemoryCategoryRepository : CategoryRepository {
    private val categories = java.util.concurrent.ConcurrentHashMap<String, Category>()
    override fun allCategories(): List<Category> = categories.values.toList()
    override fun categoryById(id: String): Category? = categories[id]
    override fun addCategory(request: CreateCategoryRequest): Category {
        val cat = Category(java.util.UUID.randomUUID().toString(), request.name, request.colorHex)
        categories[cat.id] = cat
        return cat
    }
    override fun deleteCategory(id: String): Boolean = categories.remove(id) != null
}
