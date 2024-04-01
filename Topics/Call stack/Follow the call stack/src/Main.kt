fun printIfPrime(num: Int) {

    var flag = false
    for (i in 2..(num -1)) {
        // condition for nonprime number
        if (num % i == 0) {
            flag = true
            break
        }
    }

    if (flag || num == 1)
        println("$num is not a prime number.")
    else
        println("$num is a prime number.")
}


fun main(args: Array<String>) {
    //do {
        val number = readln().toInt()
        printIfPrime(number)
    //}while (number != 0)

}
