package com.aleyn.nav.compiler

import com.aleyn.nav.compiler.generator.NavCodeGenerator
import com.aleyn.nav.compiler.scanner.NavDataScanner
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import kotlin.time.TimeSource.Monotonic.markNow

/**
 * @author : Aleyn
 * @date : 2025/11/18 11:23
 */


class NavProcessor(
    codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    private val koinCodeGenerator = NavCodeGenerator(codeGenerator, logger)

    private val screenDataScanner = NavDataScanner(logger)

    private val isLogTime by lazy {
        options.getOrDefault("OPEN_TIME_LOG", false.toString()).toBooleanStrictOrNull() ?: false
    }

    private val fileSuffix by lazy {
        options.getOrDefault("FILE_SUFFIX", "Destination")
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {

        val mainTime = if (isLogTime) markNow() else null
        val moduleName = resolver.getModuleName().asString()
        logger.logging("start scan symbols ...")

        logger.warn("$moduleName ")

        val invalidSymbols = screenDataScanner.findInvalidSymbols(resolver)
        if (invalidSymbols.isNotEmpty()) {
            logger.logging("Invalid symbols found")
            return invalidSymbols
        }

        logger.logging("Scanner @Screen  data...")

        val navGroupModels = screenDataScanner.scannerScreenFun(moduleName, resolver)

        logger.logging("Generate code ...")

        koinCodeGenerator.generateScreen(resolver, navGroupModels, fileSuffix)
        navGroupModels.forEach {
            koinCodeGenerator.generateRegistry(resolver, it, fileSuffix)
        }

        if (isLogTime && mainTime != null) {
            val time = mainTime.elapsedNow()
            logger.warn("Generated in $time")
        }

        return emptyList()
    }

}

class NavProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) =
        NavProcessor(environment.codeGenerator, environment.logger, environment.options)
}
