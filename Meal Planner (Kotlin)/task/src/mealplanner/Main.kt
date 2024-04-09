package mealplanner

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

    // Функция для создания таблиц, если они не существуют
            //statement.executeUpdate("drop table if exists ingredients")
            //statement.executeUpdate("drop table if exists meals")
            if (!resultSet.next()) {
                statement.executeUpdate("CREATE TABLE meals (meal_id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, meal TEXT)")
                statement.executeUpdate("CREATE TABLE ingredients(ingredient TEXT, ingredient_id INTEGER PRIMARY KEY AUTOINCREMENT, meal_id INTEGER)")
            }
        }

    // Функция для добавления блюда в базу данных
    fun add(connection: Connection) {
        println("Which meal do you want to add (breakfast, lunch, dinner)?")
        do {
            category = readLine().toString()
                println("Wrong meal category! Choose from: breakfast, lunch, dinner.")
            }

        println("Input the meal's name:")
        while (!nameRegex.matches(name)) {
            println("Wrong format. Use letters only!")
            name = readLine() ?: ""
        }
        println("Input the ingredients:")
        do {
            ingredientsInput = readLine() ?: ""
                println("Wrong format. Use letters only!")
            }

        val checkMealQuery = "SELECT meal_id FROM meals WHERE category = ? AND meal = ?"
        val checkMealPreparedStatement = connection.prepareStatement(checkMealQuery)
        checkMealPreparedStatement.setString(1, category)
        checkMealPreparedStatement.setString(2, name)
        val resultSet = checkMealPreparedStatement.executeQuery()

            // Вставка блюда в таблицу meals, если оно не существует
            val insertMealQuery = "INSERT INTO meals (category, meal) VALUES (?, ?)"
            val mealPreparedStatement = connection.prepareStatement(insertMealQuery)
            mealPreparedStatement.setString(1, category)
            mealPreparedStatement.setString(2, name)
            mealPreparedStatement.executeUpdate()

            val mealIdQuery = "SELECT meal_id FROM meals WHERE meal = ?"
            val mealIdPreparedStatement = connection.prepareStatement(mealIdQuery)
            mealIdPreparedStatement.setString(1, name)
            val mealIdResultSet = mealIdPreparedStatement.executeQuery()

        val insertIngredientsQuery = "INSERT INTO ingredients (ingredient, meal_id) VALUES (?, ?)"
        val ingredientsPreparedStatement = connection.prepareStatement(insertIngredientsQuery)
        for (ingredient in ingredients) {
            ingredientsPreparedStatement.setString(1, ingredient)
            ingredientsPreparedStatement.setInt(2, mealId)
            ingredientsPreparedStatement.executeUpdate()
        }
    }
        var operation: String
            println("Which category do you want to print (breakfast, lunch, dinner)?")
            do {
                operation = readLine().toString()

                    println("Wrong meal category! Choose from: breakfast, lunch, dinner.")
                    continue // Пропускаем остаток цикла и снова запрашиваем категорию
                }

                // Проверяем существует ли введенная пользователем категория в базе данных
                val categoryExistsQuery = "SELECT COUNT(*) AS count FROM meals WHERE category = ?"
                    categoryExistsPreparedStatement.setString(1, operation)
                    val categoryExistsResultSet = categoryExistsPreparedStatement.executeQuery()

                    if (categoryExistsResultSet.next()) {
                        val count = categoryExistsResultSet.getInt("count")
                        if (count == 0) {
                            println("No meals found.")
                            return
                        }
                    }


            val mealIdQuery = "SELECT meal_id, category, meal FROM meals WHERE category = ?"
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
                        }
                    }


    // Функция для выполнения запросов пользователя
    fun ask(connection: Connection) {
        val statement = connection.createStatement()
        do {
            var operation = readln().toString().toLowerCase()
            when (operation) {
                "add" -> {
                    add(connection)
                }
                "show" -> {
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
    } catch (e: SQLException) {
        println("Error: ${e.message}")
    } finally {
        connection.close()
    }
}