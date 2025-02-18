package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.incremental.incrementalHelper
import dev.shustoff.dikt.message_collector.errorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor

@OptIn(ExperimentalCompilerApi::class)
class DiktComponentRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean get() = false // todo: implement suppressing "Function 'X' without a body must be abstract" from FIR

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val errorCollector = errorCollector(configuration)
        val incrementalCache = incrementalHelper(configuration)
        StorageComponentContainerContributor.registerExtension(DiktStorageComponentContainerContributor())
        IrGenerationExtension.registerExtension(DiktIrGenerationExtension(errorCollector, incrementalCache))
    }
}