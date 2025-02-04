/*
 * (c) Copyright 2023 Birch Solutions Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fern.java.client.generators;

import com.fern.java.AbstractGeneratorContext;
import com.fern.java.client.GeneratedClientOptions;
import com.fern.java.client.GeneratedEnvironmentsClass;
import com.fern.java.generators.AbstractFileGenerator;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.lang.model.element.Modifier;
import okhttp3.OkHttpClient;

public final class ClientOptionsGenerator extends AbstractFileGenerator {

    private static final String CLIENT_OPTIONS_CLASS_NAME = "ClientOptions";

    private static final FieldSpec HEADERS_FIELD = FieldSpec.builder(
                    ParameterizedTypeName.get(Map.class, String.class, String.class),
                    "headers",
                    Modifier.PRIVATE,
                    Modifier.FINAL)
            .build();

    private static final FieldSpec HEADER_SUPPLIERS_FIELD = FieldSpec.builder(
                    ParameterizedTypeName.get(
                            ClassName.get(Map.class),
                            ClassName.get(String.class),
                            ParameterizedTypeName.get(Supplier.class, String.class)),
                    "headerSuppliers",
                    Modifier.PRIVATE,
                    Modifier.FINAL)
            .build();
    private static final FieldSpec OKHTTP_CLIENT_FIELD = FieldSpec.builder(
                    OkHttpClient.class, "httpClient", Modifier.PRIVATE, Modifier.FINAL)
            .build();

    private final ClassName builderClassName;
    private final FieldSpec environmentField;

    public ClientOptionsGenerator(
            AbstractGeneratorContext<?> generatorContext, GeneratedEnvironmentsClass generatedEnvironmentsClass) {
        super(generatorContext.getPoetClassNameFactory().getCoreClassName(CLIENT_OPTIONS_CLASS_NAME), generatorContext);
        this.builderClassName = className.nestedClass("Builder");
        this.environmentField = FieldSpec.builder(
                        generatedEnvironmentsClass.getClassName(), "environment", Modifier.PRIVATE, Modifier.FINAL)
                .addModifiers()
                .build();
    }

    @Override
    public GeneratedClientOptions generateFile() {
        MethodSpec environmentGetter = createGetter(environmentField);
        MethodSpec headersGetter = getHeadersGetter();
        MethodSpec httpClientGetter = createGetter(OKHTTP_CLIENT_FIELD);
        TypeSpec clientOptionsTypeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(environmentField)
                .addField(HEADERS_FIELD)
                .addField(HEADER_SUPPLIERS_FIELD)
                .addField(OKHTTP_CLIENT_FIELD)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .addParameter(ParameterSpec.builder(environmentField.type, environmentField.name)
                                .build())
                        .addParameter(ParameterSpec.builder(HEADERS_FIELD.type, HEADERS_FIELD.name)
                                .build())
                        .addParameter(ParameterSpec.builder(HEADER_SUPPLIERS_FIELD.type, HEADER_SUPPLIERS_FIELD.name)
                                .build())
                        .addParameter(ParameterSpec.builder(OKHTTP_CLIENT_FIELD.type, OKHTTP_CLIENT_FIELD.name)
                                .build())
                        .addStatement("this.$L = $L", environmentField.name, environmentField.name)
                        .addStatement("this.$L = $L", HEADERS_FIELD.name, HEADERS_FIELD.name)
                        .addStatement("this.$L = $L", HEADER_SUPPLIERS_FIELD.name, HEADER_SUPPLIERS_FIELD.name)
                        .addStatement("this.$L = $L", OKHTTP_CLIENT_FIELD.name, OKHTTP_CLIENT_FIELD.name)
                        .build())
                .addMethod(environmentGetter)
                .addMethod(headersGetter)
                .addMethod(httpClientGetter)
                .addMethod(MethodSpec.methodBuilder("builder")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(builderClassName)
                        .addStatement("return new $T()", builderClassName)
                        .build())
                .addType(createBuilder())
                .build();
        JavaFile environmentsFile =
                JavaFile.builder(className.packageName(), clientOptionsTypeSpec).build();
        return GeneratedClientOptions.builder()
                .className(className)
                .javaFile(environmentsFile)
                .environment(environmentGetter)
                .headers(headersGetter)
                .httpClient(httpClientGetter)
                .builderClassName(builderClassName)
                .build();
    }

    private MethodSpec getHeadersGetter() {
        return MethodSpec.methodBuilder(HEADERS_FIELD.name)
                .addModifiers(Modifier.PUBLIC)
                .returns(HEADERS_FIELD.type)
                .addStatement("$T values = new $T<>(this.$L)", HEADERS_FIELD.type, HashMap.class, HEADERS_FIELD.name)
                .beginControlFlow("$L.forEach((key, supplier) -> ", HEADER_SUPPLIERS_FIELD.name)
                .addStatement("values.put(key, supplier.get())")
                .endControlFlow(")")
                .addStatement("return values")
                .build();
    }

    private TypeSpec createBuilder() {
        return TypeSpec.classBuilder(builderClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addField(FieldSpec.builder(environmentField.type, environmentField.name)
                        .addModifiers(Modifier.PRIVATE)
                        .build())
                .addField(HEADERS_FIELD.toBuilder()
                        .initializer("new $T<>()", HashMap.class)
                        .build())
                .addField(HEADER_SUPPLIERS_FIELD.toBuilder()
                        .initializer("new $T<>()", HashMap.class)
                        .build())
                .addMethod(getEnvironmentBuilder())
                .addMethod(getHeaderBuilder())
                .addMethod(getHeaderSupplierBuilder())
                .addMethod(getBuildMethod())
                .build();
    }

    private MethodSpec getEnvironmentBuilder() {
        return MethodSpec.methodBuilder(environmentField.name)
                .addModifiers(Modifier.PUBLIC)
                .returns(builderClassName)
                .addParameter(environmentField.type, environmentField.name)
                .addStatement("this.$L = $L", environmentField.name, environmentField.name)
                .addStatement("return this")
                .build();
    }

    private MethodSpec getHeaderBuilder() {
        return MethodSpec.methodBuilder("addHeader")
                .addModifiers(Modifier.PUBLIC)
                .returns(builderClassName)
                .addParameter(String.class, "key")
                .addParameter(String.class, "value")
                .addStatement("this.$L.put($L, $L)", HEADERS_FIELD.name, "key", "value")
                .addStatement("return this")
                .build();
    }

    private MethodSpec getHeaderSupplierBuilder() {
        return MethodSpec.methodBuilder("addHeader")
                .addModifiers(Modifier.PUBLIC)
                .returns(builderClassName)
                .addParameter(String.class, "key")
                .addParameter(ParameterizedTypeName.get(Supplier.class, String.class), "value")
                .addStatement("this.$L.put($L, $L)", HEADER_SUPPLIERS_FIELD.name, "key", "value")
                .addStatement("return this")
                .build();
    }

    private MethodSpec getBuildMethod() {
        return MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(className)
                .addStatement(
                        "return new $T($L, $L, $L, new $T())",
                        className,
                        environmentField.name,
                        HEADERS_FIELD.name,
                        HEADER_SUPPLIERS_FIELD.name,
                        OkHttpClient.class)
                .build();
    }

    private static MethodSpec createGetter(FieldSpec fieldSpec) {
        return MethodSpec.methodBuilder(fieldSpec.name)
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldSpec.type)
                .addStatement("return this.$L", fieldSpec.name)
                .build();
    }
}
