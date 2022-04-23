package com.fern.model.codegen.object;

import com.fern.ObjectTypeDefinition;
import com.fern.codegen.GeneratedFileWithDefinition;
import com.fern.immutables.StagedBuilderStyle;
import org.immutables.value.Value;

@Value.Immutable
@StagedBuilderStyle
public interface GeneratedObject extends GeneratedFileWithDefinition<ObjectTypeDefinition> {

    static ImmutableGeneratedObject.FileBuildStage builder() {
        return ImmutableGeneratedObject.builder();
    }
}