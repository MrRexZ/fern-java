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

package com.fern.java.client;

import com.fern.ir.model.services.http.HttpService;
import com.fern.java.immutables.StagedBuilderImmutablesStyle;
import com.fern.java.output.IGeneratedJavaFile;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@StagedBuilderImmutablesStyle
public abstract class GeneratedServiceClient implements IGeneratedJavaFile {

    public abstract HttpService httpService();

    public abstract GeneratedJerseyServiceInterface jerseyServiceInterfaceOutput();

    public abstract List<GeneratedEndpointRequest> generatedEndpointRequestOutputs();

    public static ImmutableGeneratedServiceClient.ClassNameBuildStage builder() {
        return ImmutableGeneratedServiceClient.builder();
    }
}