package com.aleyn.nav.compiler.model

import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSValueParameter

/**
 * @author : Aleyn
 * @date : 2025/11/19 15:44
 */
sealed class MetaData {

    data class RegistryModel(
        val registryName: String,
        val screenModels: List<ScreenModel>
    ) : MetaData()

    data class ScreenModel(
        val qualifiedName: String,
        val simpleName: String,
        val funParams: List<KSValueParameter>,
        val route: String,
        val start: Boolean,
        val needLogin: Boolean,
        val containingFile: KSFile,
    ) : MetaData()

}
