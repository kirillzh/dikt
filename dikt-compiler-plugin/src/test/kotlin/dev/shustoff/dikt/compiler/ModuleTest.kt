package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@Ignore
class ModuleTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `fail with duplicated property dependencies`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create

            class Dependency

            class Injectable(val dependency: Dependency)

            class MyModule(
                val dependency1: Dependency,
                val dependency2: Dependency
            ) {
                @Create fun injectable(): Injectable
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("Multiple dependencies provided with type dev.shustoff.dikt.compiler.Dependency: dependency1, dependency2")
    }

    @Test
    fun `fail with duplicated function dependencies`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create

            class Dependency

            class Injectable(val dependency: Dependency)

            class MyModule {
                @Create fun injectable(): Injectable

                fun provide1() = Dependency()
                fun provide2() = Dependency()
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("Multiple dependencies provided with type dev.shustoff.dikt.compiler.Dependency: provide1, provide2")
    }

    @Test
    fun `interface modules not supported`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create

            class Dependency

            class Injectable(val dependency: Dependency)
            
            interface Module {
                val dependency: Dependency
                @Create fun injectable(): Injectable            
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("Only final functions can have generated body")
    }

    @Test
    fun `allow module functions for dependency resolution`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create

            class Dependency

            class Injectable(val dependency: Dependency)

            class MyModule {
                @Create fun injectable(): Injectable

                private fun provideDependency(): Dependency {
                    return Dependency()
                }
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `allow module extension function for dependency resolution`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create

            class Dependency

            class Injectable(val dependency: Dependency)

            class MyModule {
                @Create fun injectable(name: String): Injectable

                private fun String.provideDependency(): Dependency {
                    return Dependency()
                }
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}