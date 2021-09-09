package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.*
import dev.shustoff.dikt.incremental.IncrementalCompilationHelper
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.isFinalClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import java.util.*

class ModuleDiGeneratorVisitor(
    private val errorCollector: ErrorCollector,
    pluginContext: IrPluginContext,
    private val incrementalHelper: IncrementalCompilationHelper?
) : IrElementVisitorVoid, ErrorCollector by errorCollector {

    private val dependencyCollector = DependencyCollector(this)
    private val injectionBuilder = InjectionBuilder(pluginContext, errorCollector)

    private val providedByConstructorInClassCache = mutableMapOf<IrClass, List<IrType>>()
    private val providedByConstructorInFileCache = mutableMapOf<IrFile, List<IrType>>()

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitClass(declaration: IrClass) {
        if (Annotations.isModule(declaration)) {
            if (declaration.isInterface) {
                declaration.error("Interface modules not supported")
            } else if (!declaration.isFinalClass) {
                declaration.error("Module should be final")
            } else {
                val dependencies = dependencyCollector.collectDependencies(
                    module = declaration,
                    visibilityChecker = VisibilityChecker(declaration),
                    properties = declaration.properties,
                    functions = declaration.functions
                )
                val diFunctions = declaration.functions
                    .filter { function -> Annotations.isProvidedByDi(function) }
                    .map { function ->
                        val providedByConstructor = getProvidedByConstructor(function)
                        function to dependencies.resolveDependency(function.returnType, function, providedByConstructor) }
                    .toList()

                diFunctions.forEach { (function, dependency) ->
                    injectionBuilder.buildModuleFunctionInjections(declaration, function, dependency)
                }

                incrementalHelper?.recordModuleDependency(declaration, diFunctions.mapNotNull { it.second })
                RecursiveCallsDetector(errorCollector).checkForRecursiveCalls(declaration)
            }
        }
        super.visitClass(declaration)
    }

    private fun getProvidedByConstructor(function: IrSimpleFunction): Set<IrType> {
        val inFunction = Annotations.getProvidedByConstructor(function)
        val inParentClasses = getParentClasses(function).flatMap { clazz ->
            providedByConstructorInClassCache.getOrPut(clazz) {
                Annotations.getProvidedByConstructor(clazz)
            }
        }
        val inFile = providedByConstructorInFileCache.getOrPut(function.file) {
            Annotations.getProvidedByConstructor(function.file)
        }
        return (inFile + inParentClasses + inFunction).toSet()
    }

    private fun getParentClasses(function: IrSimpleFunction): List<IrClass> {
        val parent = function.parentClassOrNull ?: return emptyList()
        val result = mutableListOf<IrClass>(parent)
        while (true) {
            result.add(result.lastOrNull()?.parentClassOrNull ?: return result)
        }
    }
}