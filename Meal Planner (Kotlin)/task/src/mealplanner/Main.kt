package mealplanner

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.io.File

class MealPlanner(private val connection: Connection) {
    private val daysOfWeek = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val categories = arrayOf("breakfast", "lunch", "dinner")
    private var ingredientsTable: List<String> = listOf()
    private var planSaved: Boolean = false
    //был ли сохранен план. доступ между сессиями через файл
    init {
        planSaved = readPlanSavedFromFile()
    }

    private fun readPlanSavedFromFile(): Boolean {
        val file = File("planSaved.txt")
        return file.exists() && file.readText() == "true"
    }

    private fun writePlanSavedToFile() {
        File("planSaved.txt").writeText(planSaved.toString())
    }

    // Функция для создания таблиц, если они не существуют
    fun createTableIfNotExists() {
        connection.createStatement().use { statement ->
            //Очистка базы данных
            //statement.executeUpdate("drop table if exists ingredients")
            //statement.executeUpdate("drop table if exists meals")
            //statement.executeUpdate("drop table if exists plan")
            /*statement.executeUpdate("DELETE FROM meals")
            statement.executeUpdate("DELETE FROM ingredients")
            statement.executeUpdate("DELETE FROM plan")
             */
            //Создание таблиц
            val resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='meals'")
            if (!resultSet.next()) {
                statement.executeUpdate("CREATE TABLE meals (meal_id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, meal TEXT)")
                statement.executeUpdate("CREATE TABLE ingredients (ingredient TEXT, ingredient_id INTEGER PRIMARY KEY AUTOINCREMENT, meal_id INTEGER)")
                statement.executeUpdate("CREATE TABLE plan (option TEXT, category TEXT, meal_id INTEGER)")
            }
        }
    }

    /*fun createTableIfNotExists() {
        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='meals'")
            if (!resultSet.next()) {
                // Создаем таблицу meals
                statement.executeUpdate("CREATE TABLE meals (meal_id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, meal TEXT)")
                // Вставляем примеры данных в таблицу meals
                statement.executeUpdate("INSERT INTO meals (category, meal) VALUES ('breakfast', 'Scrambled Eggs')")
                statement.executeUpdate("INSERT INTO meals (category, meal) VALUES ('lunch', 'Chicken Salad')")
                statement.executeUpdate("INSERT INTO meals (category, meal) VALUES ('dinner', 'Spaghetti Bolognese')")

                // Создаем таблицу ingredients
                statement.executeUpdate("CREATE TABLE ingredients(ingredient TEXT, ingredient_id INTEGER PRIMARY KEY AUTOINCREMENT, meal_id INTEGER)")
                // Вставляем примеры данных в таблицу ingredients
                // Для каждого блюда вставляем примеры ингредиентов
                statement.executeUpdate("INSERT INTO ingredients (ingredient, meal_id) VALUES ('eggs', 1)")
                statement.executeUpdate("INSERT INTO ingredients (ingredient, meal_id) VALUES ('milk', 1)")
                statement.executeUpdate("INSERT INTO ingredients (ingredient, meal_id) VALUES ('salt', 1)")
                statement.executeUpdate("INSERT INTO ingredients (ingredient, meal_id) VALUES ('chicken breast', 2)")
                statement.executeUpdate("INSERT INTO ingredients (ingredient, meal_id) VALUES ('lettuce', 2)")
                statement.executeUpdate("INSERT INTO ingredients (ingredient, meal_id) VALUES ('tomato', 2)")
                statement.executeUpdate("INSERT INTO ingredients (ingredient, meal_id) VALUES ('spaghetti', 3)")
                statement.executeUpdate("INSERT INTO ingredients (ingredient, meal_id) VALUES ('minced meat', 3)")
                statement.executeUpdate("INSERT INTO ingredients (ingredient, meal_id) VALUES ('tomato sauce', 3)")

                // Создаем таблицу plan
                statement.executeUpdate("CREATE TABLE plan (option TEXT, category TEXT, meal_id INTEGER)")
            }
        }
    }
    */

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

    // Функция для вставки или обновления записи о блюде в таблице meals
    fun insertOrUpdateMeal(connection: Connection, category: String, name: String): Int {
        val checkMealQuery = "SELECT meal_id FROM meals WHERE category = ? AND meal = ?"
        val checkMealPreparedStatement = connection.prepareStatement(checkMealQuery)
        checkMealPreparedStatement.setString(1, category)
        checkMealPreparedStatement.setString(2, name)
        val resultSet = checkMealPreparedStatement.executeQuery()

        return if (!resultSet.next()) {
            // Вставка блюда в таблицу meals, если оно не существует
            val insertMealQuery = "INSERT INTO meals (category, meal) VALUES (?, ?)"
            val mealPreparedStatement = connection.prepareStatement(insertMealQuery)
            mealPreparedStatement.setString(1, category)
            mealPreparedStatement.setString(2, name)
            mealPreparedStatement.executeUpdate()

            // Получение meal_id вставленного блюда
            val mealIdQuery = "SELECT meal_id FROM meals WHERE meal = ?"
            val mealIdPreparedStatement = connection.prepareStatement(mealIdQuery)
            mealIdPreparedStatement.setString(1, name)
            val mealIdResultSet = mealIdPreparedStatement.executeQuery()
            mealIdResultSet.getInt("meal_id")
        } else {
            resultSet.getInt("meal_id")
        }
    }

    // Функция для вставки ингредиентов в таблицу ingredients
    fun insertIngredients(connection: Connection, mealId: Int, ingredients: List<String>) {
        val insertIngredientsQuery = "INSERT INTO ingredients (ingredient, meal_id) VALUES (?, ?)"
        val ingredientsPreparedStatement = connection.prepareStatement(insertIngredientsQuery)
        for (ingredient in ingredients) {
            ingredientsPreparedStatement.setString(1, ingredient)
            ingredientsPreparedStatement.setInt(2, mealId)
            ingredientsPreparedStatement.executeUpdate()
        }
    }
    // Функция для вывода блюд из базы данных
    fun show() {
        var operation: String
        connection.createStatement().use { statement ->
            println("Which category do you want to print (breakfast, lunch, dinner)?")
            do {
                operation = readLine().toString()

                if (operation !in categories) {
                    println("Wrong meal category! Choose from: breakfast, lunch, dinner.")
                    continue // Пропускаем остаток цикла и снова запрашиваем категорию
                }

                // Проверяем существует ли введенная пользователем категория в базе данных
                val categoryExistsQuery = "SELECT COUNT(*) AS count FROM meals WHERE category = ?"
                connection.prepareStatement(categoryExistsQuery).use { categoryExistsPreparedStatement ->
                    categoryExistsPreparedStatement.setString(1, operation)
                    val categoryExistsResultSet = categoryExistsPreparedStatement.executeQuery()

                    if (categoryExistsResultSet.next()) {
                        val count = categoryExistsResultSet.getInt("count")
                        if (count == 0) {
                            println("No meals found.")
                            return
                        }
                    }
                }

            } while (operation !in categories)

            val mealIdQuery = "SELECT meal_id, category, meal FROM meals WHERE category = ?"
            connection.prepareStatement(mealIdQuery).use { mealIdPreparedStatement ->
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
                    connection.prepareStatement(ingredientQuery).use { ingredientPreparedStatement ->
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
        }
    }


    fun plan(connection: Connection): List<String> {
        val ingredientsList = mutableListOf<String>()
        // Weekly plan map to store the plan for each day
        val weeklyPlan = mutableMapOf<String, MutableMap<String, String>>()
        connection.use { conn ->
            // Delete existing plan for the week
            //val deletePlanQuery = "DELETE FROM plan"
            //val preparedDeletePlanStatement = conn.prepareStatement(deletePlanQuery)
            //preparedDeletePlanStatement.executeUpdate()

            daysOfWeek.forEach { day ->
                println(day)
                val planMap = mutableMapOf<String, String>()//_
                categories.forEach { category ->
                    val mealsQuery = "SELECT meal_id, meal FROM meals WHERE category = ? ORDER BY meal"
                    val preparedMealsStatement = conn.prepareStatement(mealsQuery)
                    preparedMealsStatement.setString(1, category)
                    val mealsResult = preparedMealsStatement.executeQuery()

                    val mealsList = mutableListOf<Pair<Int, String>>()
                    while (mealsResult.next()) {
                        val mealName = mealsResult.getString("meal")
                        val mealId = mealsResult.getInt("meal_id")
                        mealsList.add(mealId to mealName)
                        println(mealName)
                    }

                    println("Choose the $category for $day from the list above:")
                    var chosenMeal: String? = null
                    var chosenMealId: Int? = null
                    do {
                        val selection = readLine().toString()
                        val foundMeal = mealsList.find { it.second == selection }
                        if (foundMeal == null) {
                            println("This meal doesn’t exist. Choose a meal from the list above.")
                        } else {
                            chosenMeal = selection
                            chosenMealId = foundMeal.first
                        }
                    } while (chosenMeal == null || chosenMealId == null)

                    planMap[category] = chosenMeal!!

                    // Save the chosen meal and its meal_id to the plan table
                    val insertPlanQuery = "INSERT INTO plan (option, category, meal_id) VALUES (?, ?, ?)"
                    val preparedInsertPlanStatement = conn.prepareStatement(insertPlanQuery)
                    preparedInsertPlanStatement.setString(1, chosenMeal)
                    preparedInsertPlanStatement.setString(2, category)
                    preparedInsertPlanStatement.setInt(3, chosenMealId)
                    preparedInsertPlanStatement.executeUpdate()

                    // Retrieve ingredients for the chosen meal and add them to the ingredientsList
                    val ingredientsQuery = "SELECT ingredient FROM ingredients WHERE meal_id = ?"
                    val preparedIngredientsStatement = conn.prepareStatement(ingredientsQuery)
                    preparedIngredientsStatement.setInt(1, chosenMealId)
                    val ingredientsResult = preparedIngredientsStatement.executeQuery()
                        while (ingredientsResult.next()) {
                            val ingredient = ingredientsResult.getString("ingredient")
                            ingredientsList.add(ingredient)
                        }
                    }
                    // Print the plan for the day
                    println("Yeah! We planned the meals for $day.")

                    // Add the daily plan to the weekly plan
                    weeklyPlan[day] = planMap
                }
            }
        // Print the whole plan for the week
        weeklyPlan.forEach { (day, plan) ->
            println("$day")
            println("Breakfast: ${plan["breakfast"]}")
            println("Lunch: ${plan["lunch"]}")
            println("Dinner: ${plan["dinner"]}")
            println()
        }
        planSaved = true
        writePlanSavedToFile()
        return ingredientsList
    }

    fun save(ingredientsList: List<String> ) {
        println("Input a filename:")
        val filename = readLine().toString()
        val myFile = File("$filename")
        val ingredientOccurrences = mutableMapOf<String, Int>()
        //повторяющиеся ингридиенты пересмотреть и подсчитать
        for (ingredient in ingredientsList) {
            val count = ingredientOccurrences.getOrDefault(ingredient, 0)
            ingredientOccurrences[ingredient] = count + 1
        }
        ingredientOccurrences.forEach {(ingredient, count) ->
            myFile.appendText("$ingredient x$count")
            myFile.appendText("\n")
        }
        println("Saved!")
    }

    // Функция для выполнения запросов пользователя
    fun ask(connection: Connection) {
        val statement = connection.createStatement()
        //var isPlanSaved: Boolean = false
        do {
            println("What would you like to do (add, show, plan, save, exit)?")
            var operation = readln().toString().toLowerCase()
            when (operation) {
                "add" -> {
                    add(connection)
                }

                "show" -> {
                    show()
                }

                "plan" -> {
                    ingredientsTable = plan(connection)
                }

                "save" -> {
                    if(planSaved){
                        save(ingredientsTable)
                    } else {
                        println("Unable to save. Plan your meals first.")
                    }
                }
                "exit" -> {
                    println("Bye!")
                    return
                }
            }
        } while (operation != "exit")
        statement.close()
    }
}

fun main() {
    val url = "jdbc:sqlite:meals.db"
    val connection = DriverManager.getConnection(url)
    val mealPlanner = MealPlanner(connection)
    mealPlanner.createTableIfNotExists()

    try {
        mealPlanner.ask(connection)
    } catch (e: SQLException) {
        println("Error: ${e.message}")
    } finally {
        connection.close()
    }
}