package com.aleyn.nav.compiler.check

import com.google.devtools.ksp.processing.Resolver

/**
 * @author : Aleyn
 * @date : 2025/11/20 11:53
 */


fun Resolver.checkClassGenerate(className: String): Boolean {
    val declaration = this.getClassDeclarationByName(this.getKSNameFromString(className))
    return declaration != null
}