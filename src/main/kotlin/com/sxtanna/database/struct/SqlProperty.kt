package com.sxtanna.database.struct

import com.sxtanna.database.ext.VARCHAR_SIZE
import com.sxtanna.database.struct.obj.*
import com.sxtanna.database.struct.obj.SqlType.*
import com.sxtanna.database.struct.obj.SqlType.EnumSet
import java.math.BigInteger
import java.sql.Timestamp
import java.util.*
import kotlin.Boolean
import kotlin.Byte
import kotlin.Char
import kotlin.Double
import kotlin.Enum
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.Short
import kotlin.String
import kotlin.Suppress
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure

@Suppress("UNCHECKED_CAST")
object SqlProperty {

	private val adapters = mutableMapOf<KClass<*>, KProperty1<*, *>.() -> SqlType>()
	private val fallback : KProperty1<*, *>.() -> SqlType = { VarChar(VARCHAR_SIZE, isPrimaryKey(), isNotNull()) }

	init {
		adapters[Char::class] = { Char(1, isPrimaryKey(), isNotNull()) }
		adapters[UUID::class] = { Char(36, isPrimaryKey(), isNotNull()) }

		adapters[Boolean::class] = { Bool(isPrimaryKey(), isNotNull()) }

		adapters[String::class] = {
			val fix = findAnnotation<Fixed>()?.length ?: -1

			if (fix > 0) Char(fix, isPrimaryKey(), isNotNull())
			else VarChar(findAnnotation<Size>()?.length ?: VARCHAR_SIZE, isPrimaryKey(), isNotNull())
		}

		adapters[Enum::class] = {
			val serialized = findAnnotation<Serialized>() != null
			if (serialized) VarChar(VARCHAR_SIZE, isPrimaryKey(), isNotNull()) else EnumSet(returnType.jvmErasure as KClass<out Enum<*>>, isPrimaryKey(), isNotNull())
		}

		adapters[Timestamp::class] = {
			val time = findAnnotation<Time>()
			Timestamp(time?.current ?: false, time?.updating ?: false, isPrimaryKey(), isNotNull())
		}

		val numberAdapter : KProperty1<*, *>.() -> SqlType = {

			val size = findAnnotation<Size>()
			val length = (size?.length ?: 100).coerceAtMost(255)

			when (findAnnotation<IntType>()?.value ?: NormInt::class) {
				TinyInt::class -> TinyInt(length, isUnsigned(), isPrimaryKey(), isNotNull())
				SmallInt::class -> SmallInt(length, isUnsigned(), isPrimaryKey(), isNotNull())
				MediumInt::class -> MediumInt(length, isUnsigned(), isPrimaryKey(), isNotNull())
				BigInt::class -> {
					BigInt(length.toString(), isUnsigned(), isPrimaryKey(), isNotNull())
				}
				else -> {
					if (returnType.jvmErasure == BigInteger::class) BigInt(length.toString(), isUnsigned(), isPrimaryKey(), isNotNull())
					else NormInt(length.toLong(), isUnsigned(), isPrimaryKey(), isNotNull())
				}
			}
		}

		adapters.multiplePut(numberAdapter, Byte::class, Short::class, Int::class, Long::class, BigInteger::class)

		val decimalAdapter : KProperty1<*, *>.() -> SqlType = {
			val size = findAnnotation<Size>()
			Decimal(size?.length ?: 10, size?.places ?: 1, isPrimaryKey(), isNotNull())
		}

		adapters.multiplePut(decimalAdapter, Float::class, Double::class)
	}


	operator fun get(property : KProperty1<*, *>) : SqlType = (adapters[property.returnType.jvmErasure] ?: fallback).invoke(property)


	private fun KProperty1<*, *>.isNotNull() = findAnnotation<Nullable>() == null

	private fun KProperty1<*, *>.isUnsigned() = findAnnotation<Unsigned>() != null

	private fun KProperty1<*, *>.isPrimaryKey() = findAnnotation<PrimaryKey>() != null

	private fun <K, V> MutableMap<K, V>.multiplePut(value : V, vararg keys : K) = keys.forEach { put(it, value) }

}