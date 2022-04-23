package com.fern.model.codegen.enums;

import com.fern.EnumTypeDefinition;
import com.fern.EnumValue;
import com.fern.FernFilepath;
import com.fern.NamedType;
import com.fern.Type;
import com.fern.TypeDefinition;
import com.fern.model.codegen.TestConstants;
import org.junit.jupiter.api.Test;

public class EnumGeneratorTest {

    @Test
    public void test_basic() {
        EnumTypeDefinition migrationStatusEnumDef = EnumTypeDefinition.builder()
                .addValues(EnumValue.builder().value("RUNNING").build())
                .addValues(EnumValue.builder().value("FAILED").build())
                .addValues(EnumValue.builder().value("FINISHED").build())
                .build();
        TypeDefinition migrationStatusTypeDef = TypeDefinition.builder()
                .name(NamedType.builder()
                        .name("MigrationStatus")
                        .fernFilepath(FernFilepath.valueOf("com/trace/migration"))
                        .build())
                .shape(Type._enum(migrationStatusEnumDef))
                .build();
        EnumGenerator enumGenerator = new EnumGenerator(
                migrationStatusTypeDef.name(), migrationStatusEnumDef, TestConstants.GENERATOR_CONTEXT);
        GeneratedEnum generatedEnum = enumGenerator.generate();
        System.out.println(generatedEnum.file().toString());
    }
}