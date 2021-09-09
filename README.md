[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.sergeshustoff.dikt/dikt-compiler-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.sergeshustoff.dikt/dikt-compiler-plugin)
[![gradle plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/io/github/sergeshustoff/dikt/dikt-gradle-plugin/maven-metadata.xml.svg?label=gradle%20plugin)](https://plugins.gradle.org/plugin/io.github.sergeshustoff.dikt)
[![IDEA plugin](https://img.shields.io/jetbrains/plugin/v/17533-di-kt.svg)](https://plugins.jetbrains.com/plugin/17533-di-kt)

# DI.kt
Simple DI with compile-time dependency graph validation for kotlin multiplatform.
It uses IR to create method's bodies with dependency injection.

Limitations: all annotations required for generating functions should be available in the same file as generated function. It can use methods and constructors from outside, but not annotations, because adding and removing annotations in other files would not trigger recompilation for generated function and combined with incremental compilation it would cause errors.

### Why another DI?
Variety is a good thing, and this library is a bit different from other available multiplatform solutions. It's not better, just different and might be more useful in some cases and less useful in others.

#### Other viable solutions:

[Kotlin-inject](https://github.com/evant/kotlin-inject) - incredibly powerful DI framework with Dagger-like api;

[Koin](https://github.com/InsertKoinIO/koin), [Kodein-DI](https://github.com/Kodein-Framework/Kodein-DI) and [PopKorn](https://github.com/corbella83/PopKorn) - service locators with great versatility, but without compile time error detection that we used to have in Dagger;

[Dagger](https://github.com/google/dagger) - most popular DI framework for Android, but doesn't support multiplatform yet.

## Installation

#### Gradle plugin:
In build.gradle file in module add plugin:

    plugins {
        ...
        id 'io.github.sergeshustoff.dikt' version '1.0.0-aplha5'
    }

Also add runtime library to dependency if it isn't added by plugin ([bug](https://github.com/sergeshustoff/dikt/issues/2)):

    dependencies {
        ...
        implementation "com.github.sergeshustoff.dikt:dikt-annotations:1.0.0-alpha6"
    }

#### IDEA plugin

Install [idea plugin](https://plugins.jetbrains.com/plugin/17533-di-kt), it will remove errors from ide for methods with generated body.

## Usage

Create module and declare provided dependencies. Use @Create, @Provide, @CreateSingle and @ProvideSingle to generate function's bodies. Use @UseModules and @UseConstructors to control how dependencies are provided and what classes can be created by primary constructors.

    class SomethingModule(
        val externalDependency: Something,
    ) {
        @CreateSingle fun someSingleton(): SomeSingleton
        @Create fun provideSomethingElse(): SomethingElse
    }
  
Under the hood primary constructor will be called for SomethingElse and SomeSingleton. If constructor requires some parameters - they will be retrieved form module's properties and functions.

## Annotations

### @Create

Magical annotation that tells compiler plugin to generate method body using returned type's primary constructor.
Values for constructor parameters will be retrieved from function parameters and from functions and properties of containing class.

Code generated by this annotation always uses returned type's primary constructor, even if dependency of returned type is available in parameters or in containing class.

#### Example:
    
    class Something(val name: String)

    @Create fun provideSomething(name: String): Something

Code above will be transformed into

    fun provideSomething(name: String) = Something(name)

### @Provide

Tells compiler plugin to generate method body that returns value of specified type retrieved from dependencies. For example from containing class properties or functions. 

It's useful for elevating dependencies from nested modules.
Doesn't call constructor for returned type unless it's listed in @UseConstructors.

#### Example:

    class Something(val name: String)

    class ExternalModule(
        val something: Something
    )

    @UseModules(ExternalModule::class)
    class MyModule(val external: ExternalModule) {
        @Provide fun provideSomething(): Something
    }

### @CreateSingle and @ProvideSingle

Same as @Create and @Provide, but creates a lazy property and return value from it. Functions marked with @CreateSingle and @ProvideSingle don't support parameters.

### @UseConstructors

Dependencies of types listed in this annotation parameters will be provided by constructor when required.

Might be applied to file, class, or @Create or @Provide function.

When constructor called for returned type of @Create function requires parameter of type listed in @UseConstructors it's constructor will be called instead of looking for provided dependency of that type.

#### Example:

    class SomeDependency

    class Something(val dependency: SomeDependency)

    @UseConstructors(SomeDependency::class)
    class MyModule {
        @Create fun provideSomething(): Something
    }

### @UseModules

Marks types that should provide all visible properties and functions as dependencies. Such dependencies can be used in @Create function as constructor parameters or in @Provide function as returned type.

Listed type should be available from DI function in order to provide type's properties and functions.

WARNING: This annotation doesn't work recursively. It means that function can only use modules listed in its own annotation or in its class annotation or in its file annotation. 

#### Example:

    class ExternalModule(val name: String)

    class Something(val name: String)

    @UseModules(ExternalModule::class)
    class MyModule(
        private val external: ExternalModule
    ) {
        @Create fun provideSomething(): Something // will call constructor using external.name as parameter
    }