package dev.shustoff.dikt

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
/**
 * DI.kt plugin will generate body for function with this annotation using returned type's primary constructor.
 * Values for constructor parameters will be retrieved from function parameters and from functions and properties of containing class.
 *
 * Code generated by this annotation always uses returned type's primary constructor, even if dependency of returned type is available in parameters or in containing class.
 */
annotation class Create
