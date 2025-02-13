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

package androidx.room.compiler.codegen

import androidx.room.compiler.codegen.java.JavaPropertySpec
import androidx.room.compiler.codegen.java.NONNULL_ANNOTATION
import androidx.room.compiler.codegen.java.NULLABLE_ANNOTATION
import androidx.room.compiler.codegen.java.toJavaVisibilityModifier
import androidx.room.compiler.codegen.kotlin.KotlinPropertySpec
import androidx.room.compiler.codegen.kotlin.toKotlinVisibilityModifier
import androidx.room.compiler.processing.PropertySpecHelper
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType

interface XPropertySpec : TargetLanguage {

    val name: String

    interface Builder : TargetLanguage {
        fun addAnnotation(annotation: XAnnotationSpec): Builder

        fun initializer(initExpr: XCodeBlock): Builder

        fun getter(code: XCodeBlock): Builder

        fun build(): XPropertySpec
    }

    companion object {
        @JvmStatic
        fun builder(
            language: CodeLanguage,
            name: String,
            typeName: XTypeName,
            visibility: VisibilityModifier,
            isMutable: Boolean = false,
        ): Builder {
            return when (language) {
                CodeLanguage.JAVA ->
                    JavaPropertySpec.Builder(
                        JPropertySpec.builder(typeName.java, name).apply {
                            val visibilityModifier = visibility.toJavaVisibilityModifier()
                            // TODO(b/247242374) Add nullability annotations for non-private fields
                            if (visibilityModifier != JModifier.PRIVATE) {
                                if (typeName.nullability == XNullability.NULLABLE) {
                                    addAnnotation(NULLABLE_ANNOTATION)
                                } else if (typeName.nullability == XNullability.NONNULL) {
                                    addAnnotation(NONNULL_ANNOTATION)
                                }
                            }
                            addModifiers(visibilityModifier)
                            if (!isMutable) {
                                addModifiers(JModifier.FINAL)
                            }
                        }
                    )
                CodeLanguage.KOTLIN ->
                    KotlinPropertySpec.Builder(
                        KPropertySpec.builder(name, typeName.kotlin).apply {
                            mutable(isMutable)
                            addModifiers(visibility.toKotlinVisibilityModifier())
                        }
                    )
            }
        }

        @JvmStatic
        fun overridingBuilder(
            language: CodeLanguage,
            element: XMethodElement,
            owner: XType
        ): Builder {
            require(element.isKotlinPropertyMethod())
            return when (language) {
                CodeLanguage.JAVA ->
                    error("Overriding a property is not supported when code language is Java.")
                CodeLanguage.KOTLIN ->
                    KotlinPropertySpec.Builder(PropertySpecHelper.overriding(element, owner))
            }
        }
    }
}
