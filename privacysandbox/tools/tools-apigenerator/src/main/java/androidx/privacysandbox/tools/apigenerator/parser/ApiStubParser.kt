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

package androidx.privacysandbox.tools.apigenerator.parser

import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.Parameter
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.Type
import androidx.privacysandbox.tools.core.model.ValueProperty
import androidx.privacysandbox.tools.core.validator.ModelValidator
import java.nio.file.Path
import kotlinx.metadata.ClassName
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import kotlinx.metadata.KmType

internal object ApiStubParser {
    /**
     * Parses the API annotated by a Privacy Sandbox SDK from its compiled classes.
     *
     * @param sdkStubsClasspath root directory of SDK classpath.
     */
    internal fun parse(sdkStubsClasspath: Path): ParsedApi {
        val (services, values, callbacks, interfaces) =
            AnnotatedClassReader.readAnnotatedClasses(sdkStubsClasspath)
        if (services.isEmpty()) throw PrivacySandboxParsingException(
            "Unable to find valid interfaces annotated with @PrivacySandboxService."
        )
        return ParsedApi(
            services.map { parseInterface(it, "PrivacySandboxService") }.toSet(),
            values.map(::parseValue).toSet(),
            callbacks.map { parseInterface(it, "PrivacySandboxCallback") }.toSet(),
            interfaces.map { parseInterface(it, "PrivacySandboxInterface") }.toSet(),
        ).also(::validate)
    }

    private fun parseInterface(service: KmClass, annotationName: String): AnnotatedInterface {
        val type = parseClassName(service.name)

        if (!Flag.Class.IS_INTERFACE(service.flags)) {
            throw PrivacySandboxParsingException(
                "${type.qualifiedName} is not a Kotlin interface but it's annotated with " +
                    "@$annotationName."
            )
        }

        return AnnotatedInterface(
            type = type,
            service.functions.map(this::parseMethod),
        )
    }

    private fun parseValue(value: KmClass): AnnotatedValue {
        val type = parseClassName(value.name)

        if (!Flag.Class.IS_DATA(value.flags)) {
            throw PrivacySandboxParsingException(
                "${type.qualifiedName} is not a Kotlin data class but it's annotated with " +
                    "@PrivacySandboxValue."
            )
        }
        return AnnotatedValue(
            type,
            value.properties.map { parseProperty(type, it) },
        )
    }

    private fun parseProperty(containerType: Type, property: KmProperty): ValueProperty {
        val qualifiedName = "${containerType.qualifiedName}.${property.name}"
        if (Flag.Property.IS_VAR(property.flags)) {
            throw PrivacySandboxParsingException(
                "Error in $qualifiedName: mutable properties are not allowed in data classes " +
                    "annotated with @PrivacySandboxValue."
            )
        }
        return ValueProperty(property.name, parseType(property.returnType))
    }

    private fun parseMethod(function: KmFunction): Method {
        return Method(
            function.name,
            function.valueParameters.map { Parameter(it.name, parseType(it.type)) },
            parseType(function.returnType),
            Flag.Function.IS_SUSPEND(function.flags)
        )
    }

    private fun parseType(type: KmType): Type {
        val classifier = type.classifier
        if (classifier !is KmClassifier.Class) {
            throw PrivacySandboxParsingException("Unsupported type in API description: $type")
        }
        val typeArguments = type.arguments.map { parseType(it.type!!) }
        return parseClassName(classifier.name, typeArguments)
    }

    private fun parseClassName(
        className: ClassName,
        typeArguments: List<Type> = emptyList()
    ): Type {
        // Package names are separated with slashes and nested classes are separated with dots.
        // (e.g com/example/OuterClass.InnerClass).
        val (packageName, simpleName) = className.split('/').run {
            dropLast(1).joinToString(separator = ".") to last()
        }

        if (simpleName.contains('.')) {
            throw PrivacySandboxParsingException(
                "Error in $packageName.$simpleName: Inner types are not supported in API " +
                    "definitions."
            )
        }

        return Type(packageName, simpleName, typeArguments)
    }

    private fun validate(api: ParsedApi) {
        val validationResult = ModelValidator.validate(api)
        if (validationResult.isFailure) {
            throw PrivacySandboxParsingException(
                "Invalid API descriptors:\n" +
                    validationResult.errors.joinToString("\n")
            )
        }
    }
}

class PrivacySandboxParsingException(message: String) : Exception(message)
