/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.privacysandbox.tools.core.model

fun ParsedApi.getOnlyService(): AnnotatedInterface {
    check(services.size == 1) {
        "Expected to find one annotated service, but found ${services.size}."
    }
    return services.first()
}

fun ParsedApi.hasSuspendFunctions(): Boolean {
    val annotatedInterfaces = services + interfaces
    return annotatedInterfaces
        .flatMap(AnnotatedInterface::methods)
        .any(Method::isSuspend)
}

object Types {
    val unit = Type(packageName = "kotlin", simpleName = "Unit")
    val boolean = Type(packageName = "kotlin", simpleName = "Boolean")
    val int = Type(packageName = "kotlin", simpleName = "Int")
    val long = Type(packageName = "kotlin", simpleName = "Long")
    val float = Type(packageName = "kotlin", simpleName = "Float")
    val double = Type(packageName = "kotlin", simpleName = "Double")
    val string = Type(packageName = "kotlin", simpleName = "String")
    val char = Type(packageName = "kotlin", simpleName = "Char")
    val short = Type(packageName = "kotlin", simpleName = "Short")
    val primitiveTypes = setOf(unit, boolean, int, long, float, double, string, char, short)
    fun list(elementType: Type) = Type(
        packageName = "kotlin.collections",
        simpleName = "List",
        typeParameters = listOf(elementType)
    )
}