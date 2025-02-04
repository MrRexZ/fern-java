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

import com.fern.irV12.model.auth.AuthScheme;
import com.fern.irV12.model.auth.HeaderAuthScheme;
import com.fern.irV12.model.commons.TypeId;
import com.fern.irV12.model.commons.WithDocs;
import com.fern.java.AbstractGeneratorContext;
import com.fern.java.client.ClientGeneratorContext;
import com.fern.java.client.GeneratedClientOptions;
import com.fern.java.client.GeneratedEnvironmentsClass;
import com.fern.java.client.GeneratedEnvironmentsClass.SingleUrlEnvironmentClass;
import com.fern.java.client.GeneratedRootClient;
import com.fern.java.client.generators.ClientGeneratorUtils.Result;
import com.fern.java.generators.AbstractFileGenerator;
import com.fern.java.output.GeneratedJavaFile;
import com.fern.java.output.GeneratedJavaInterface;
import com.fern.java.output.GeneratedObjectMapper;
import com.fern.java.utils.CasingUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.Base64;
import java.util.Map;
import javax.lang.model.element.Modifier;

public final class RootClientGenerator extends AbstractFileGenerator {

    private static final String CLIENT_OPTIONS_BUILDER_NAME = "clientOptionsBuilder";
    private static final String ENVIRONMENT_FIELD_NAME = "environment";
    private final GeneratedObjectMapper generatedObjectMapper;
    private final ClientGeneratorContext clientGeneratorContext;
    private final GeneratedClientOptions generatedClientOptions;
    private final Map<TypeId, GeneratedJavaInterface> allGeneratedInterfaces;
    private final GeneratedJavaFile generatedSuppliersFile;
    private final GeneratedEnvironmentsClass generatedEnvironmentsClass;
    private final ClassName builderName;

    public RootClientGenerator(
            AbstractGeneratorContext<?> generatorContext,
            GeneratedObjectMapper generatedObjectMapper,
            ClientGeneratorContext clientGeneratorContext,
            GeneratedClientOptions generatedClientOptions,
            GeneratedJavaFile generatedSuppliersFile,
            GeneratedEnvironmentsClass generatedEnvironmentsClass,
            Map<TypeId, GeneratedJavaInterface> allGeneratedInterfaces) {
        super(
                generatorContext.getPoetClassNameFactory().getRootClassName(getRootClientName(generatorContext)),
                generatorContext);
        this.generatedObjectMapper = generatedObjectMapper;
        this.clientGeneratorContext = clientGeneratorContext;
        this.generatedClientOptions = generatedClientOptions;
        this.generatedSuppliersFile = generatedSuppliersFile;
        this.generatedEnvironmentsClass = generatedEnvironmentsClass;
        this.allGeneratedInterfaces = allGeneratedInterfaces;
        this.builderName = ClassName.get(className.packageName(), className.simpleName() + "Builder");
    }

    @Override
    public GeneratedRootClient generateFile() {
        ClientGeneratorUtils clientGeneratorUtils = new ClientGeneratorUtils(
                className,
                clientGeneratorContext,
                generatedClientOptions,
                generatedObjectMapper,
                generatedEnvironmentsClass,
                allGeneratedInterfaces,
                generatedSuppliersFile,
                generatorContext.getIr().getRootPackage());
        Result result = clientGeneratorUtils.buildClients();

        TypeSpec builderTypeSpec = getClientBuilder();

        result.getClientImpl()
                .addMethod(MethodSpec.methodBuilder("builder")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(builderName)
                        .addStatement("return new $T()", builderName)
                        .build());

        return GeneratedRootClient.builder()
                .className(className)
                .javaFile(JavaFile.builder(
                                className.packageName(), result.getClientImpl().build())
                        .build())
                .builderClass(GeneratedJavaFile.builder()
                        .className(builderName)
                        .javaFile(JavaFile.builder(builderName.packageName(), builderTypeSpec)
                                .build())
                        .build())
                .addAllWrappedRequests(result.getGeneratedWrappedRequests())
                .build();
    }

    private TypeSpec getClientBuilder() {
        TypeSpec.Builder typeSpecBuilder =
                TypeSpec.classBuilder(builderName).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        typeSpecBuilder.addField(
                FieldSpec.builder(generatedClientOptions.builderClassName(), CLIENT_OPTIONS_BUILDER_NAME)
                        .addModifiers(Modifier.PRIVATE)
                        .initializer("$T.builder()", generatedClientOptions.getClassName())
                        .build());

        FieldSpec.Builder environmentFieldBuilder = FieldSpec.builder(
                        generatedEnvironmentsClass.getClassName(), ENVIRONMENT_FIELD_NAME)
                .addModifiers(Modifier.PRIVATE);

        AuthSchemeHandler authSchemeHandler = new AuthSchemeHandler(typeSpecBuilder);
        generatorContext.getIr().getAuth().getSchemes().forEach(authScheme -> authScheme.visit(authSchemeHandler));
        generatorContext.getIr().getHeaders().forEach(httpHeader -> {
            authSchemeHandler.visitHeader(HeaderAuthScheme.builder()
                    .name(httpHeader.getName().getName())
                    .header(httpHeader.getName().getWireValue())
                    .valueType(httpHeader.getValueType())
                    .docs(httpHeader.getDocs())
                    .build());
        });

        if (generatedEnvironmentsClass.defaultEnvironmentConstant().isPresent()) {
            environmentFieldBuilder.initializer(
                    "$T.$L",
                    generatedEnvironmentsClass.getClassName(),
                    generatedEnvironmentsClass.defaultEnvironmentConstant().get());
        }
        if (generatedEnvironmentsClass.optionsPresent()) {
            MethodSpec environmentMethod = MethodSpec.methodBuilder("environment")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(generatedEnvironmentsClass.getClassName(), "environment")
                    .returns(builderName)
                    .build();
            typeSpecBuilder.addMethod(environmentMethod.toBuilder()
                    .addStatement("this.$L = $L", ENVIRONMENT_FIELD_NAME, "environment")
                    .addStatement("return this")
                    .build());
        }

        FieldSpec environmentField = environmentFieldBuilder.build();
        typeSpecBuilder.addField(environmentField);

        if (generatedEnvironmentsClass.info() instanceof SingleUrlEnvironmentClass) {
            SingleUrlEnvironmentClass singleUrlEnvironmentClass =
                    ((SingleUrlEnvironmentClass) generatedEnvironmentsClass.info());
            MethodSpec urlMethod = MethodSpec.methodBuilder("url")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(String.class, "url")
                    .returns(builderName)
                    .build();
            typeSpecBuilder.addMethod(urlMethod.toBuilder()
                    .addStatement(
                            "this.$L = $T.$N($L)",
                            ENVIRONMENT_FIELD_NAME,
                            generatedEnvironmentsClass.getClassName(),
                            singleUrlEnvironmentClass.getCustomMethod(),
                            "url")
                    .addStatement("return this")
                    .build());
        }

        MethodSpec buildMethod = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(className)
                .build();
        typeSpecBuilder.addMethod(buildMethod.toBuilder()
                .addStatement(
                        "$L.$N(this.$N)",
                        CLIENT_OPTIONS_BUILDER_NAME,
                        generatedClientOptions.environment(),
                        environmentField)
                .addStatement("return new $T($L.build())", className, CLIENT_OPTIONS_BUILDER_NAME)
                .build());

        return typeSpecBuilder.build();
    }

    private static String getRootClientName(AbstractGeneratorContext<?> generatorContext) {
        return getRootClientPrefix(generatorContext) + "Client";
    }

    private static String getRootClientPrefix(AbstractGeneratorContext<?> generatorContext) {
        return CasingUtils.convertKebabCaseToUpperCamelCase(
                        generatorContext.getGeneratorConfig().getOrganization())
                + CasingUtils.convertKebabCaseToUpperCamelCase(
                        generatorContext.getGeneratorConfig().getWorkspaceName());
    }

    private class AuthSchemeHandler implements AuthScheme.Visitor<Void> {

        private static final String BEARER_TOKEN_NAME = "token";

        private static final String BASIC_USERNAME_NAME = "username";
        private static final String BASIC_PASSWORD_NAME = "password";

        private final TypeSpec.Builder builder;

        private AuthSchemeHandler(TypeSpec.Builder builder) {
            this.builder = builder;
        }

        @Override
        public Void visitBearer(WithDocs bearer) {
            MethodSpec tokenMethod = MethodSpec.methodBuilder(BEARER_TOKEN_NAME)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(String.class, BEARER_TOKEN_NAME)
                    .returns(builderName)
                    .build();
            builder.addMethod(tokenMethod.toBuilder()
                    .addStatement(
                            "this.$L.addHeader($S, $S + $L)",
                            CLIENT_OPTIONS_BUILDER_NAME,
                            "Authorization",
                            "Bearer ",
                            BEARER_TOKEN_NAME)
                    .addStatement("return this")
                    .build());
            return null;
        }

        @Override
        public Void visitBasic(WithDocs basic) {
            MethodSpec tokenMethod = MethodSpec.methodBuilder("credentials")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(String.class, BASIC_USERNAME_NAME)
                    .addParameter(String.class, BASIC_PASSWORD_NAME)
                    .returns(builderName)
                    .build();
            builder.addMethod(tokenMethod.toBuilder()
                    .addStatement("String unencodedToken = $L + \":\" + $L", BASIC_USERNAME_NAME, BASIC_PASSWORD_NAME)
                    .addStatement(
                            "String encodedToken = $T.getEncoder().encodeToString(unencodedToken.getBytes())",
                            Base64.class)
                    .addStatement(
                            "this.$L.addHeader($S, $S + $L)",
                            CLIENT_OPTIONS_BUILDER_NAME,
                            "Authorization",
                            "Basic ",
                            "encodedToken")
                    .addStatement("return this")
                    .build());
            return null;
        }

        @Override
        public Void visitHeader(HeaderAuthScheme header) {
            String headerCamelCase = header.getName().getCamelCase().getSafeName();
            MethodSpec tokenMethod = MethodSpec.methodBuilder(headerCamelCase)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(String.class, headerCamelCase)
                    .returns(builderName)
                    .build();
            if (header.getPrefix().isPresent()) {
                builder.addMethod(tokenMethod.toBuilder()
                        .addStatement(
                                "this.$L.addHeader($S, $S + $L)",
                                CLIENT_OPTIONS_BUILDER_NAME,
                                header.getHeader(),
                                header.getPrefix().get(),
                                headerCamelCase)
                        .addStatement("return this")
                        .build());
            } else {
                builder.addMethod(tokenMethod.toBuilder()
                        .addStatement(
                                "this.$L.addHeader($S, $L)",
                                CLIENT_OPTIONS_BUILDER_NAME,
                                header.getHeader(),
                                headerCamelCase)
                        .addStatement("return this")
                        .build());
            }
            return null;
        }

        @Override
        public Void _visitUnknown(Object unknownType) {
            throw new RuntimeException("Encountered unknown auth scheme");
        }
    }
}
