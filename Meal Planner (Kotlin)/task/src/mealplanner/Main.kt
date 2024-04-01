package mealplanner

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

// Функция для создания таблиц, если они не существуют
fun createTableIfNotExists(connection: Connection) {
    val statement = connection.createStatement()

    // Проверяем существование таблицы
    val resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='meals'")
    //statement.executeUpdate("drop table if exists ingredients")
    //statement.executeUpdate("drop table if exists meals")
    if (!resultSet.next()) {
        // Если таблица не существует, создаем её
        statement.executeUpdate("CREATE TABLE meals (meal_id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, meal TEXT)")
        statement.executeUpdate("CREATE TABLE ingredients(ingredient TEXT, ingredient_id INTEGER PRIMARY KEY AUTOINCREMENT, meal_id INTEGER)")
    }

    resultSet.close()
    statement.close()
}

// Функция для добавления блюда в базу данных
fun add(connection: Connection) {
    var category: String
    var name: String
    var ingredientsInput = ""
    println("Which meal do you want to add (breakfast, lunch, dinner)?")
    do {
        category = readLine().toString()

        if (category !in listOf("breakfast", "lunch", "dinner")) {
            println("Wrong meal category! Choose from: breakfast, lunch, dinner.")
            continue
        }
    } while (category !in listOf("breakfast", "lunch", "dinner"))



    println("Input the meal's name:")
    name = readLine() ?: ""
    val nameRegex = Regex("""^[a-zA-Z]+$""")
    while (!nameRegex.matches(name)) {
        println("Wrong format. Use letters only!")
        name = readLine() ?: ""
    }
    println("Input the ingredients:")
    val ingredientsRegex = Regex("""^[a-zA-Z\s]+$""")
    var checkInput = 0
    do {
        checkInput = 0
        ingredientsInput = readLine() ?: ""
        val ingredients = ingredientsInput.split(",").map(String::trim).toTypedArray()
        for (i in ingredients) {
            if (i.isNotBlank() && i.isNotEmpty() && ingredientsRegex.matches(i)) {
                checkInput += 1
            }
        }
        if (checkInput == ingredients.size) {
            println("The meal has been added!")
        } else {
            println("Wrong format. Use letters only!")
        }
    } while (!ingredientsRegex.matches(ingredientsInput) && checkInput < ingredients.size)

    val ingredients = ingredientsInput.split(",").map(String::trim).toTypedArray()

    try {
        // Проверка существования блюда с таким же названием и категорией
        val checkMealQuery = "SELECT meal_id FROM meals WHERE category = ? AND meal = ?"
        val checkMealPreparedStatement = connection.prepareStatement(checkMealQuery)
        checkMealPreparedStatement.setString(1, category)
        checkMealPreparedStatement.setString(2, name)
        val resultSet = checkMealPreparedStatement.executeQuery()

        if (!resultSet.next()) {
            // Вставка блюда в таблицу meals, если оно не существует
            val insertMealQuery = "INSERT INTO meals (category, meal) VALUES (?, ?)"
            val mealPreparedStatement = connection.prepareStatement(insertMealQuery)
            mealPreparedStatement.setString(1, category)
            mealPreparedStatement.setString(2, name)
            mealPreparedStatement.executeUpdate()
        }

        // Получение meal_id вставленного блюда или существующего блюда
        val mealIdQuery = "SELECT meal_id FROM meals WHERE meal = ?"
        val mealIdPreparedStatement = connection.prepareStatement(mealIdQuery)
        mealIdPreparedStatement.setString(1, name)
        val mealIdResultSet = mealIdPreparedStatement.executeQuery()
        val mealId = if (mealIdResultSet.next()) mealIdResultSet.getInt("meal_id") else -1

        // Вставка ингредиентов в таблицу ingredients
        val insertIngredientsQuery = "INSERT INTO ingredients (ingredient, meal_id) VALUES (?, ?)"
        val ingredientsPreparedStatement = connection.prepareStatement(insertIngredientsQuery)
        for (ingredient in ingredients) {
            ingredientsPreparedStatement.setString(1, ingredient)
            ingredientsPreparedStatement.setInt(2, mealId)
            ingredientsPreparedStatement.executeUpdate()
        }
    } catch (e: SQLException) {
        println("Error adding meal: ${e.message}")
    }
}

fun show(connection: Connection) {
    var operation: String
    val statement = connection.createStatement()
    try {
        println("Which category do you want to print (breakfast, lunch, dinner)?")
        do {
            operation = readLine().toString()

            if (operation !in listOf("breakfast", "lunch", "dinner")) {
                println("Wrong meal category! Choose from: breakfast, lunch, dinner.")
                continue // Пропускаем остаток цикла и снова запрашиваем категорию
            }

            // Проверяем существует ли введенная пользователем категория в базе данных
            val categoryExistsQuery = "SELECT COUNT(*) AS count FROM meals WHERE category = ?"
            val categoryExistsPreparedStatement = connection.prepareStatement(categoryExistsQuery)
            categoryExistsPreparedStatement.setString(1, operation)
            val categoryExistsResultSet = categoryExistsPreparedStatement.executeQuery()

            if (categoryExistsResultSet.next()) {
                val count = categoryExistsResultSet.getInt("count")
                if (count == 0) {
                    println("No meals found.")
                    return
                }
            }

        } while (operation !in listOf("breakfast", "lunch", "dinner"))

        val mealIdQuery = "SELECT meal_id, category, meal FROM meals WHERE category = ?"
        val mealIdPreparedStatement = connection.prepareStatement(mealIdQuery)
        mealIdPreparedStatement.setString(1, operation)
        val resultSet = mealIdPreparedStatement.executeQuery()
        val category = resultSet.getString("category")
        println("Category: $category")
        println("")
        while (resultSet.next()) {
            val mealId = resultSet.getInt("meal_id")
            val name = resultSet.getString("meal")
            println("Name: $name")

            val ingredientQuery = "SELECT ingredient FROM ingredients WHERE meal_id = ?"
            val ingredientPreparedStatement = connection.prepareStatement(ingredientQuery)
            ingredientPreparedStatement.setInt(1, mealId)
            val ingredientResultSet = ingredientPreparedStatement.executeQuery()

            val ingredients = mutableListOf<String>()
            while (ingredientResultSet.next()) {
                val ingredient = ingredientResultSet.getString("ingredient")
                ingredients.add(ingredient)
            }

            if (ingredients.isNotEmpty()) {
                println("Ingredients:\n${ingredients.joinToString("\n")}")
            }
            println()
        }
    } catch (e: SQLException) {
        println("Error showing meals: ${e.message}")
    } finally {
        statement.close()
    }
}


// Функция для выполнения запросов пользователя
fun ask(connection: Connection) {
    val statement = connection.createStatement()
    do {
        println("What would you like to do (add, show, exit)?")
        var operation = readln().toString().toLowerCase()
        when (operation) {
            "add" -> {
                add(connection)
            }
            "show" -> {
                show(connection)
                }
            "exit" -> {
                println("Bye!")
                return
            }
        }
    } while (operation != "exit")
    statement.close()
}

fun main() {
    val url = "jdbc:sqlite:meals.db"
    val connection = DriverManager.getConnection(url)
    try {
        createTableIfNotExists(connection)
        ask(connection)
    } catch (e: SQLException) {
        println("Error: ${e.message}")
    } finally {
        connection.close()
    }
}
