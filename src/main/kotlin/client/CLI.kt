package client

import core.Connection
import core.Metadata

fun getSourceCode(fileName: String): String {
    /*
        - First check if the file is already existent or not.
        - Read the content of the file.
            - Check if the file is empty
        - Return a string representing the file content
            - Check if the return string is empty at higher-order functions, and if so, then don't send a request.
     */

    /*
        - Three testing strings.
            - They mock the possible different scenarios i.e., valid source, compile time error, runtime error.
     */
    val validSource = """
        fun main() {
            println("valid")
        }
    """.trimIndent()
    val compileTimeError = """
        fun main() {
            printtl("valid")
            println("valid)
        }
    """.trimIndent()
    val runTimeError = """
        fun main() {
            println("Hello, world!")
            val names = listOf(1, 2, 3)
            for (i in 0..names.size) {
                println(names[i])
            }
        }
    """.trimIndent()
    return (validSource)
}

/*
    - ClI client
        - This class should be response for all of the CLI operations e.g., printing out to stdout, text formatting, and highlighting, and parsing
        - It's also (optionally) responsible for parsing & pass command line arguments.
 */
class CLI {
    init {
        val connection: Connection = Connection(Metadata().versions.last())
        connection.run(getSourceCode("placeHolderFile.kt"))
    }
}