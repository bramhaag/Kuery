package com.sxtanna.database.struct

data class Duo<out T : Any>(val name : String, val value : T) {


	companion object {

		@JvmStatic
		@SafeVarargs
		fun valueColumns(vararg columns : Duo<Any>) = arrayOf(*columns)

		@JvmStatic
		@SafeVarargs
		fun tableColumns(vararg columns : Duo<SqlType>) = arrayOf(*columns)

	}

}