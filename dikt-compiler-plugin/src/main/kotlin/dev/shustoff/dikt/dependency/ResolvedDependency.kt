package dev.shustoff.dikt.dependency

import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody

sealed class ResolvedDependency {
    data class Provided(
        val provided: ProvidedDependency,
        val nestedModulesChain: ResolvedDependency? = null,
        val params: List<ResolvedDependency> = emptyList(),
        val extensionParam: ResolvedDependency? = null
    ) : ResolvedDependency()

    data class Constructor(
        val constructor: IrConstructor,
        val params: List<ResolvedDependency> = emptyList(),
    ) : ResolvedDependency()

    data class ParameterDefaultValue(
        val defaultValue: IrExpressionBody
    ) : ResolvedDependency()
}