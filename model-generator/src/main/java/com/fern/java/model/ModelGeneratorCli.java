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

package com.fern.java.model;

import com.fern.generator.exec.model.config.GeneratorConfig;
import com.fern.ir.model.ir.IntermediateRepresentation;
import com.fern.java.AbstractGeneratorCli;
import com.fern.java.DefaultGeneratorExecClient;
import com.fern.java.generators.TypesGenerator;
import com.fern.java.generators.TypesGenerator.Result;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModelGeneratorCli extends AbstractGeneratorCli {

    private static final String UTILS_VERSION = "0.0.82";

    private static final Logger log = LoggerFactory.getLogger(ModelGeneratorCli.class);

    @Override
    public void run(
            DefaultGeneratorExecClient defaultGeneratorExecClient,
            GeneratorConfig generatorConfig,
            IntermediateRepresentation ir) {
        ModelGeneratorContext context = new ModelGeneratorContext(ir, generatorConfig);

        // types
        TypesGenerator typesGenerator = new TypesGenerator(context);
        Result generatedTypes = typesGenerator.generateFiles();
        generatedTypes.getTypes().values().forEach(addGeneratedFile);
        generatedTypes.getInterfaces().values().forEach(addGeneratedFile);
    }

    @Override
    public List<String> getBuildGradleDependencies() {
        return List.of(
                "    api 'io.github.fern-api:jackson-utils:" + UTILS_VERSION + "'",
                "    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.12.3'");
    }

    public static void main(String... args) {
        ModelGeneratorCli cli = new ModelGeneratorCli();
        cli.run(args);
    }
}
