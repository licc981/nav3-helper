package com.aleyn.nav.compiler.generator

import com.aleyn.nav.compiler.check.checkClassGenerate
import com.aleyn.nav.compiler.model.MetaData
import com.aleyn.nav.compiler.util.DefaultValue
import com.aleyn.nav.compiler.util.getDefaultValue
import com.aleyn.navigation.core.route.NavRegistry
import com.aleyn.navigation.core.route.NavScreen
import com.aleyn.navigation.core.route.routeKey as normalizedRouteKey
import com.aleyn.navigation.core.route.routePatternIdentity
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import java.io.OutputStream

/**
 * @author : Aleyn
 * @date : 2025/11/18 11:24
 */


class NavCodeGenerator(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger,
) {

    fun generateScreen(
        resolver: Resolver,
        registryModels: List<MetaData.RegistryModel>,
        fileSuffix: String
    ) {
        registryModels.forEach { registryModel ->
            registryModel.screenModels.forEach {
                genScreenModel(
                    resolver = resolver,
                    screen = it,
                    fileSuffix = fileSuffix,
                    ksFiles = listOf(it.containingFile)
                )
            }
        }
    }


    fun generateRegistry(
        resolver: Resolver,
        registryModel: MetaData.RegistryModel,
        fileSuffix: String
    ) {
        val screenList = registryModel.screenModels
        val routableScreens = screenList.filter { it.route.isNotBlank() }

        if (screenList.isEmpty()) return

        val duplicatedRoutes = routableScreens.groupingBy { routePatternIdentity(it.route) }.eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicatedRoutes.isNotEmpty()) {
            throw RuntimeException(
                "Routes in the same registry must be unique: ${duplicatedRoutes.joinToString()}"
            )
        }

        val registryPackage = generatedPackageName(registryModel.registryName)
        val registryClassName = "${registryModel.registryName.substringAfterLast(".")}Registry"
        val registryQualifiedName = "$registryPackage.$registryClassName"
        if (resolver.checkClassGenerate(registryQualifiedName)) {
            logger.logging("skip registry generate :$registryQualifiedName")
            return
        }
        val fileSpec = buildRegistryFileSpec(resolver, registryModel, fileSuffix)
        val sources = registryModel.screenModels.map { it.containingFile }.toTypedArray()
        codeGenerator.genKtFile(registryPackage, registryClassName, sources = sources)
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }


    private fun genScreenModel(
        resolver: Resolver,
        screen: MetaData.ScreenModel,
        fileSuffix: String,
        ksFiles: List<KSFile>
    ) {
        val pkgName = generatedPackageName(screen.qualifiedName)
        val className = "${screen.simpleName}${fileSuffix}"
        val classQualifiedName = "$pkgName.$className"
        if (resolver.checkClassGenerate(classQualifiedName)) {
            logger.logging("skip destination generate :$classQualifiedName")
            return
        }
        val fileBuilder = FileSpec.builder(pkgName, className)

        val defaultValueImports = linkedSetOf<String>()
        val navigationParameters = screen.navigationParameters(resolver)

        val genFunMaps = buildMap {
            navigationParameters.forEach { parameter ->
                createScreenParameter(
                    resolver = resolver,
                    screen = screen,
                    parameter = parameter,
                    defaultValueImports = defaultValueImports
                )
            }
        }

        val classSpec = if (genFunMaps.isNotEmpty()) {
            val constructorBuilder = FunSpec.constructorBuilder()
                .addParameters(genFunMaps.keys)
                .build()

            TypeSpec.classBuilder(className)
                .primaryConstructor(constructorBuilder)
                .addProperties(genFunMaps.values)
                .addSuperinterface(NavScreen::class)
                .addAnnotation(ClassName.bestGuess("kotlinx.serialization.Serializable"))
                .addModifiers(KModifier.DATA)
        } else {
            TypeSpec.objectBuilder(className)
                .addModifiers(KModifier.DATA)
                .addSuperinterface(NavScreen::class)
                .addAnnotation(ClassName.bestGuess("kotlinx.serialization.Serializable"))
        }

        classSpec.addProperty(
            PropertySpec.builder("needLogin", BOOLEAN, KModifier.OVERRIDE)
                .initializer("%L", screen.needLogin)
                .build()
        )

        logger.info("gen :$pkgName.$className")

        val fileSpecBuilder = fileBuilder
            .addImports(defaultValueImports)
            .addType(classSpec.build())

        val fileSpec = fileSpecBuilder.build()

        val sources = ksFiles.distinct().toTypedArray()
        codeGenerator.genKtFile(pkgName, className, sources = sources)
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }

    }
}

internal const val GENERATED_PACKAGE_PREFIX = "com.navigation.screen.generated"

internal fun buildRegistryFileSpec(
    resolver: Resolver,
    registryModel: MetaData.RegistryModel,
    fileSuffix: String
): FileSpec {
    val groupName = registryModel.registryName.substringAfterLast(".")
    val registryClassName = "${groupName}Registry"
    val registryPackage = generatedPackageName(registryModel.registryName)
    val screenList = registryModel.screenModels
    val routableScreens = screenList.filter { it.route.isNotBlank() }
    val fileBuilder = FileSpec.builder(registryPackage, registryClassName)
    val defaultValueImports = linkedSetOf<String>()
    val classSpec = TypeSpec.objectBuilder(registryClassName)
        .addSuperinterface(NavRegistry::class)

    screenList.singleOrNull { it.start }?.let { startScreen ->
        val startClass = startScreen.generatedClassName(fileSuffix)
        val startProperty = PropertySpec.builder("defaultStartScreen", startClass)
            .initializer("%T", startClass)
            .build()
        classSpec.addProperty(startProperty)
    }
    classSpec.addProperty(
        PropertySpec.builder(
            "routes",
            ClassName("kotlin.collections", "Set").parameterizedBy(STRING),
            KModifier.OVERRIDE
        )
            .initializer(buildRoutesCode(routableScreens))
            .build()
    )
    classSpec.addProperty(
        PropertySpec.builder(
            "loginRoutes",
            ClassName("kotlin.collections", "Set").parameterizedBy(STRING),
            KModifier.OVERRIDE
        )
            .initializer(buildRoutesCode(routableScreens.filter { it.needLogin }))
            .build()
    )
    classSpec.addProperty(
        PropertySpec.builder(
            "serializersModule",
            ClassName("kotlinx.serialization.modules", "SerializersModule"),
            KModifier.OVERRIDE
        )
            .initializer(buildSerializersModuleCode(screenList, fileSuffix))
            .build()
    )

    val receiverType = ClassName("androidx.navigation3.runtime", "EntryProviderScope")
        .parameterizedBy(NavScreen::class.asTypeName())

    val entryFun = FunSpec.builder("entryProvider")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("scope", receiverType)

    screenList.forEach { model ->
        val screenClass = model.generatedClassName(fileSuffix)
        val screenContent = model.screenContentMember()
        entryFun.addCode(
            CodeBlock.builder()
                .add("scope.entry<%T> {\n", screenClass)
                .indent()
                .add(buildScreenInvocationCode(resolver, model, screenContent))
                .unindent()
                .add("}\n\n")
                .build()
        )
    }

    classSpec.addFunction(entryFun.build())
    classSpec.addFunction(
        FunSpec.builder("resolve")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                "parsedRouteUrl",
                ClassName("com.aleyn.navigation.core.route", "ParsedRouteUrl")
            )
            .returns(NavScreen::class.asClassName().copy(nullable = true))
            .addCode(buildRouteResolveCode(resolver, routableScreens, fileSuffix, defaultValueImports))
            .build()
    )
    return fileBuilder
        .addImports(defaultValueImports)
        .addType(classSpec.build())
        .build()
}

internal fun generatedPackageName(sourceQualifiedName: String): String {
    var sourcePackage = sourceQualifiedName.substringBeforeLast(".", "")
    if (sourcePackage == "com.aleyn.nav.annotations") {
        sourcePackage = GENERATED_PACKAGE_PREFIX
    }
    return sourcePackage.ifBlank { GENERATED_PACKAGE_PREFIX }
}

private fun MetaData.ScreenModel.generatedClassName(fileSuffix: String): ClassName {
    return ClassName(generatedPackageName(qualifiedName), "${simpleName}${fileSuffix}")
}

private fun MetaData.ScreenModel.screenContentMember(): MemberName {
    return MemberName(
        qualifiedName.substringBeforeLast("."),
        qualifiedName.substringAfterLast(".")
    )
}

private fun buildScreenInvocationCode(
    resolver: Resolver,
    screen: MetaData.ScreenModel,
    screenContent: MemberName
): CodeBlock {
    val navigationParameters = screen.navigationParameters(resolver)
    if (navigationParameters.isEmpty()) {
        return CodeBlock.of("%M()\n", screenContent)
    }

    val args = navigationParameters.joinToString(",\n") { parameter ->
        val name = parameter.name?.asString().orEmpty()
        "    $name = it.$name"
    }

    return CodeBlock.of("%M(\n%L\n)\n", screenContent, args)
}

private fun buildSerializersModuleCode(
    screenList: List<MetaData.ScreenModel>,
    fileSuffix: String
): CodeBlock {
    val serializersModule = MemberName("kotlinx.serialization.modules", "SerializersModule")
    val polymorphic = MemberName("kotlinx.serialization.modules", "polymorphic")
    val subclass = MemberName("kotlinx.serialization.modules", "subclass")
    val navKey = ClassName("androidx.navigation3.runtime", "NavKey")

    return CodeBlock.builder()
        .add("%M {\n", serializersModule)
        .indent()
        .add("%M(%T::class) {\n", polymorphic, navKey)
        .indent()
        .apply {
            screenList.forEach { model ->
                val screenClass = model.generatedClassName(fileSuffix)
                add("%M(%T::class, %T.serializer())\n", subclass, screenClass, screenClass)
            }
        }
        .unindent()
        .add("}\n")
        .unindent()
        .add("}")
        .build()
}

private fun buildRoutesCode(screenList: List<MetaData.ScreenModel>): CodeBlock {
    if (screenList.isEmpty()) {
        return CodeBlock.of("emptySet()")
    }

    return CodeBlock.builder()
        .add("setOf(\n")
        .indent()
        .apply {
            screenList.forEachIndexed { index, model ->
                add("%S", normalizedRouteKey(model.route))
                if (index != screenList.lastIndex) add(",\n") else add("\n")
            }
        }
        .unindent()
        .add(")")
        .build()
}

private fun buildRouteResolveCode(
    resolver: Resolver,
    screenList: List<MetaData.ScreenModel>,
    fileSuffix: String,
    defaultValueImports: MutableSet<String>
): CodeBlock {
    if (screenList.isEmpty()) {
        return CodeBlock.of("return null\n")
    }

    return CodeBlock.builder()
        .add("return when (parsedRouteUrl.routeKey) {\n")
        .indent()
        .apply {
            screenList.forEach { model ->
                add(
                    "%S -> %L\n",
                    normalizedRouteKey(model.route),
                    buildRouteDestinationCode(
                        resolver = resolver,
                        screen = model,
                        fileSuffix = fileSuffix,
                        queryParametersName = "parsedRouteUrl.queryParameters",
                        defaultValueImports = defaultValueImports
                    )
                )
            }
        }
        .add("else -> null\n")
        .unindent()
        .add("}\n")
        .build()
}

private fun buildRouteDestinationCode(
    resolver: Resolver,
    screen: MetaData.ScreenModel,
    fileSuffix: String,
    queryParametersName: String,
    defaultValueImports: MutableSet<String>
): CodeBlock {
    val navigationParameters = screen.navigationParameters(resolver)
    if (navigationParameters.isEmpty()) {
        return CodeBlock.of("%T", screen.generatedClassName(fileSuffix))
    }

    val arguments = navigationParameters.joinToString(",\n") { parameter ->
        val parameterName = parameter.name?.asString().orEmpty()
        "$parameterName = ${
            routeArgumentExpression(
                resolver = resolver,
                screen = screen,
                parameter = parameter,
                queryParametersName = queryParametersName,
                defaultValueImports = defaultValueImports
            )
        }"
    }

    return CodeBlock.of(
        "%T(\n%L\n)",
        screen.generatedClassName(fileSuffix),
        arguments.prependIndent("    ")
    )
}

private fun routeArgumentExpression(
    resolver: Resolver,
    screen: MetaData.ScreenModel,
    parameter: KSValueParameter,
    queryParametersName: String,
    defaultValueImports: MutableSet<String>
): String {
    val parameterName = parameter.name?.asString().orEmpty()
    val ksType = parameter.type.resolve()
    val notNullType = ksType.makeNotNullable()
    val builtIns = resolver.builtIns
    val defaultValue = when (val result = parameter.getDefaultValue(resolver)) {
        is DefaultValue.Available -> {
            defaultValueImports += result.imports
            result
        }

        is DefaultValue.Error -> {
            throw RuntimeException(
                "Failed to read default value for route '${screen.route}' on " +
                        "'${screen.simpleName}($parameterName)': ${result.throwable.message}",
                result.throwable
            )
        }

        DefaultValue.NonExistent -> null
    }

    val routeValueExpression = "$queryParametersName[\"$parameterName\"]"
    val fallbackExpression = when {
        defaultValue != null -> defaultValue.code
        ksType.isMarkedNullable -> "null"
        else -> "return null"
    }

    if (notNullType == builtIns.stringType) {
        return "$routeValueExpression ?: $fallbackExpression"
    }

    if (notNullType == builtIns.booleanType) {
        return "$routeValueExpression?.toBooleanStrictOrNull() ?: $fallbackExpression"
    }

    if (notNullType == builtIns.byteType) {
        return "$routeValueExpression?.toByteOrNull() ?: $fallbackExpression"
    }

    if (notNullType == builtIns.shortType) {
        return "$routeValueExpression?.toShortOrNull() ?: $fallbackExpression"
    }

    if (notNullType == builtIns.intType) {
        return "$routeValueExpression?.toIntOrNull() ?: $fallbackExpression"
    }

    if (notNullType == builtIns.longType) {
        return "$routeValueExpression?.toLongOrNull() ?: $fallbackExpression"
    }

    if (notNullType == builtIns.floatType) {
        return "$routeValueExpression?.toFloatOrNull() ?: $fallbackExpression"
    }

    if (notNullType == builtIns.doubleType) {
        return "$routeValueExpression?.toDoubleOrNull() ?: $fallbackExpression"
    }

    if (notNullType == builtIns.charType) {
        return "$routeValueExpression?.singleOrNull() ?: $fallbackExpression"
    }

    if (notNullType.isKotlinxSerializableType()) {
        return "com.aleyn.navigation.core.route.deserializeRouteQueryValue<${notNullType.toTypeName()}>($routeValueExpression) ?: $fallbackExpression"
    }

    if (defaultValue != null || ksType.isMarkedNullable) {
        return fallbackExpression
    }

    throw RuntimeException(
        "Screen '${screen.simpleName}' declares parameter '$parameterName', but " +
                "its type '${parameter.type.toTypeName()}' is not supported for route query parsing. " +
                "Only primitive, String, and @Serializable types can be restored from query parameters. " +
                "If this value should not come from the route, make it nullable, give it a default value, " +
                "or load it from inside the screen."
    )
}

private fun MutableMap<ParameterSpec, PropertySpec>.createScreenParameter(
    resolver: Resolver,
    screen: MetaData.ScreenModel,
    parameter: KSValueParameter,
    defaultValueImports: MutableSet<String>
) {
    val name = parameter.name?.asString().orEmpty()
    val type = parameter.type
    val typeName = type.toTypeName()
    val ksType = type.resolve()
    val builtIns = resolver.builtIns
    val primitiveTypes = setOf(
        builtIns.byteType,
        builtIns.shortType,
        builtIns.intType,
        builtIns.longType,
        builtIns.floatType,
        builtIns.doubleType,
        builtIns.charType,
        builtIns.booleanType,
        builtIns.stringType
    )

    val notNullType = ksType.makeNotNullable()
    val isIterable = builtIns.iterableType.isAssignableFrom(notNullType)
            || builtIns.arrayType.isAssignableFrom(notNullType)
    val isPrimitive = notNullType in primitiveTypes
    val isSerializable = notNullType.isKotlinxSerializableType()

    if (!isPrimitive && !isIterable && !isSerializable) {
        throw RuntimeException(
            "Unsupported navigation parameter '${screen.simpleName}($name:$typeName)'. " +
                    "Only primitive, String, array, Iterable, and @Serializable types are supported in generated destinations. " +
                    "Non-serializable complex objects should stay out of the route and be loaded from inside the screen."
        )
    }

    val parameterSpec = ParameterSpec.builder(name, typeName).apply {
        when (val result = parameter.getDefaultValue(resolver)) {
            is DefaultValue.Available -> {
                defaultValueImports += result.imports
                defaultValue("%L", result.code)
            }

            is DefaultValue.Error -> {
                throw RuntimeException(
                    "Failed to read default value for '${screen.simpleName}($name:$typeName)': ${result.throwable.message}",
                    result.throwable
                )
            }

            DefaultValue.NonExistent -> Unit
        }
    }.build()

    val propertySpec = PropertySpec.builder(name, typeName)
        .initializer(name)
        .build()

    put(parameterSpec, propertySpec)
}

private fun MetaData.ScreenModel.navigationParameters(
    resolver: Resolver
): List<KSValueParameter> {
    return funParams.filter { parameter ->
        val ksType = parameter.type.resolve()
        if (ksType.isSupportedDestinationType(resolver)) {
            return@filter true
        }

        if (parameter.hasDefault) {
            false
        } else {
            throw RuntimeException(
                "Unsupported navigation parameter '${simpleName}(${parameter.name?.asString()}:${parameter.type.toTypeName()})'. " +
                        "Non-serializable parameters must have a default value so generated code can omit them."
            )
        }
    }
}

private fun KSType.isSupportedDestinationType(resolver: Resolver): Boolean {
    val builtIns = resolver.builtIns
    val notNullType = makeNotNullable()
    val primitiveTypes = setOf(
        builtIns.byteType,
        builtIns.shortType,
        builtIns.intType,
        builtIns.longType,
        builtIns.floatType,
        builtIns.doubleType,
        builtIns.charType,
        builtIns.booleanType,
        builtIns.stringType
    )
    return notNullType in primitiveTypes ||
            builtIns.iterableType.isAssignableFrom(notNullType) ||
            builtIns.arrayType.isAssignableFrom(notNullType) ||
            notNullType.isKotlinxSerializableType()
}

private fun FileSpec.Builder.addImports(imports: Set<String>): FileSpec.Builder {
    imports.forEach { importValue ->
        val packageName = importValue.substringBeforeLast(".", "")
        val symbolName = importValue.substringAfterLast(".")
        if (packageName.isNotBlank()) {
            addImport(packageName, symbolName)
        }
    }
    return this
}

private fun KSType.isKotlinxSerializableType(): Boolean {
    return declaration.annotations.any { annotation ->
        annotation.annotationType.resolve().declaration.qualifiedName?.asString() == "kotlinx.serialization.Serializable"
    }
}


internal fun CodeGenerator.genKtFile(
    packageName: String,
    fileName: String,
    vararg sources: KSFile
): OutputStream {
    return try {
        createNewFile(
            Dependencies(aggregating = true, sources = sources),
            packageName,
            fileName,
        )
    } catch (ex: FileAlreadyExistsException) {
        ex.file.outputStream()
    }
}
