package org.ccci.gto.android.common.jsonapi;

import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPostCreate;
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType;
import org.ccci.gto.android.common.jsonapi.model.JsonApiObject;
import org.ccci.gto.android.common.jsonapi.model.ModelBase;
import org.ccci.gto.android.common.jsonapi.model.ModelSimple;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JsonApiConverterPostCreateIT {
    @Test(expected = IllegalArgumentException.class)
    public void verifyConverterMultiplePostCreate() throws Exception {
        new JsonApiConverter.Builder().addClasses(ModelDuplicatePostCreate.class).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyConverterMultiplePostCreateParams() throws Exception {
        new JsonApiConverter.Builder().addClasses(ModelPostCreateParameters.class).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyConverterMultiplePostCreateCheckedException() throws Exception {
        new JsonApiConverter.Builder().addClasses(ModelPostCreateCheckedException.class).build();
    }

    @Test
    public void verifyFromJsonSimple() throws Exception {
        final JsonApiConverter converter = new JsonApiConverter.Builder().addClasses(ModelSimple.class).build();

        final ModelSimple source = new ModelSimple(99);
        final JsonApiObject<ModelSimple> output =
                converter.fromJson(converter.toJson(JsonApiObject.single(source)), ModelSimple.class);
        assertThat(output.isSingle(), is(true));
        assertThat(output.getDataSingle(), is(not(nullValue())));
        assertThat(output.getDataSingle().mId, is(99));
        assertTrue(output.getDataSingle().mPostCreateCalled);
    }

    @JsonApiType("duplicate")
    private static class ModelDuplicatePostCreate extends ModelBase {
        @JsonApiPostCreate
        private void create() {}
    }

    @JsonApiType("params")
    private static class ModelPostCreateParameters {
        @JsonApiPostCreate
        private void postCreate(final String param1) {}
    }

    @JsonApiType("exception")
    private static class ModelPostCreateCheckedException {
        @JsonApiPostCreate
        private void postCreate() throws Exception {}
    }
}
