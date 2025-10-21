package ai.platon.cdt.kt.protocol.support.annotations

import kotlin.String
import kotlin.`annotation`.Retention
import kotlin.`annotation`.Target
import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER,
AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Optional

@Target(AnnotationTarget.FUNCTION,
AnnotationTarget.CLASS,
AnnotationTarget.PROPERTY,
AnnotationTarget.VALUE_PARAMETER,
AnnotationTarget.PROPERTY_GETTER,
AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Experimental

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
public annotation class ParamName(
  public val `value`: String,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Returns(
  public val `value`: String,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class ReturnTypeParameter(
  public vararg val `value`: KClass<*>,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class EventName(
  public val `value`: String,
)
