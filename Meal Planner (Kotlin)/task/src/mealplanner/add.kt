package mealplanner

import java.sql.Connection
import java.sql.SQLException

// Функция для добавления блюда в базу данных
fun add(connection: Connection) {
    val category = readCategory()
    val name = readMealName()
    val ingredients = readIngredients()

    try {
        val mealId = insertOrUpdateMeal(connection, category, name)
        insertIngredients(connection, mealId, ingredients)
        println("The meal has been added!")
    } catch (e: SQLException) {
        println("Error adding meal: ${e.message}")
    }
}

// Функция для ввода категории блюда
fun readCategory(): String {
    println("Which meal do you want to add (breakfast, lunch, dinner)?")
    val categories = arrayOf("breakfast", "lunch", "dinner")
    var category: String
    do {
        category = readLine().toString()
        if (category !in categories) {
            println("Wrong meal category! Choose from: breakfast, lunch, dinner.")
        }
    } while (category !in categories)
    return category
}

// Функция для ввода названия блюда
fun readMealName(): String {
    println("Input the meal's name:")
    var name = readLine() ?: ""
    val nameRegex = Regex("""^[a-zA-Z\s]+$""")
    while (!nameRegex.matches(name)) {
        println("Wrong format. Use letters only!")
        name = readLine() ?: ""
    }
    return name
}

// Функция для ввода ингредиентов
fun readIngredients(): List<String> {
    println("Input the ingredients:")
    var ingredientsInput: String
    do {
        ingredientsInput = readLine() ?: ""
        val ingredients = ingredientsInput.split(",").map(String::trim)
        if (!ingredients.all { it.matches(Regex("""^[a-zA-Z\s]+$""")) }) {
            println("Wrong format. Use letters only!")
        }
    } while (!ingredientsInput.matches(Regex("""^([a-zA-Z\s]+,)*[a-zA-Z\s]+$""")))
    return ingredientsInput.split(",").map(String::trim)
}
