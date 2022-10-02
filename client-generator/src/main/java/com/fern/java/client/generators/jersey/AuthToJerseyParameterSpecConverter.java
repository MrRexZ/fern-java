/*
 * (c) Copyright 2022 Birch Solutions Inc. All rights reserved.
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

package com.fern.java.client.generators.jersey;

import com.fern.ir.model.auth.ApiAuth;
import com.fern.ir.model.auth.AuthScheme;
import com.fern.ir.model.commons.WithDocs;
import com.fern.ir.model.services.http.HttpEndpoint;
import com.fern.ir.model.services.http.HttpHeader;
import com.fern.java.AbstractGeneratorContext;
import com.fern.java.output.AbstractGeneratedFileOutput;
import com.fern.java.output.GeneratedAuthFilesOutput;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.HeaderParam;

public final class AuthToJerseyParameterSpecConverter {
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    private final AbstractGeneratorContext<?> generatorContext;
    private final GeneratedAuthFilesOutput generatedAuthFilesOutput;

    public AuthToJerseyParameterSpecConverter(
            AbstractGeneratorContext<?> generatorContext, GeneratedAuthFilesOutput generatedAuthFilesOutput) {
        this.generatorContext = generatorContext;
        this.generatedAuthFilesOutput = generatedAuthFilesOutput;
    }

    public List<ParameterSpec> getAuthParameters(HttpEndpoint httpEndpoint) {
        ApiAuth apiAuth = generatorContext.getIr().getAuth();
        if (!httpEndpoint.getAuth() || apiAuth.getSchemes().isEmpty()) {
            return Collections.emptyList();
        } else if (apiAuth.getSchemes().size() == 1) {
            AuthScheme authScheme = apiAuth.getSchemes().get(0);
            ParameterSpec parameterSpec =
                    authScheme.visit(new AuthSchemeParameterSpec(generatedAuthFilesOutput, "auth", false));
            return Collections.singletonList(parameterSpec);
        }
        return Collections.emptyList();
    }

    private static final class AuthSchemeParameterSpec implements AuthScheme.Visitor<ParameterSpec> {

        private final AbstractGeneratedFileOutput generatedFile;
        private final boolean isOptional;
        private final String parameterName;

        private AuthSchemeParameterSpec(
                AbstractGeneratedFileOutput generatedFile, String parameterName, boolean isOptional) {
            this.generatedFile = generatedFile;
            this.isOptional = isOptional;
            this.parameterName = parameterName;
        }

        @Override
        public ParameterSpec visitBearer(WithDocs value) {
            return ParameterSpec.builder(getTypeName(), parameterName)
                    .addAnnotation(AnnotationSpec.builder(HeaderParam.class)
                            .addMember("value", "$S", AUTHORIZATION_HEADER_NAME)
                            .build())
                    .build();
        }

        @Override
        public ParameterSpec visitBasic(WithDocs value) {
            return ParameterSpec.builder(getTypeName(), parameterName)
                    .addAnnotation(AnnotationSpec.builder(HeaderParam.class)
                            .addMember("value", "$S", AUTHORIZATION_HEADER_NAME)
                            .build())
                    .build();
        }

        @Override
        public ParameterSpec visitHeader(HttpHeader value) {
            return ParameterSpec.builder(getTypeName(), parameterName)
                    .addAnnotation(AnnotationSpec.builder(HeaderParam.class)
                            .addMember("value", "$S", value.getName().getWireValue())
                            .build())
                    .build();
        }

        @Override
        public ParameterSpec visitUnknown(String unknownType) {
            throw new RuntimeException("Encountered unknown AuthScheme: " + unknownType);
        }

        private TypeName getTypeName() {
            if (isOptional) {
                return ParameterizedTypeName.get(ClassName.get(Optional.class), generatedFile.getClassName());
            } else {
                return generatedFile.getClassName();
            }
        }
    }
}