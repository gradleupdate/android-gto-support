package org.ccci.gto.android.common.jsonapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute;
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId;
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore;
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType;
import org.ccci.gto.android.common.jsonapi.converter.TypeConverter;
import org.ccci.gto.android.common.jsonapi.model.JsonApiObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.ccci.gto.android.common.jsonapi.model.JsonApiObject.JSON_DATA;
import static org.ccci.gto.android.common.jsonapi.model.JsonApiObject.JSON_DATA_ATTRIBUTES;
import static org.ccci.gto.android.common.jsonapi.model.JsonApiObject.JSON_DATA_ID;
import static org.ccci.gto.android.common.jsonapi.model.JsonApiObject.JSON_DATA_TYPE;

public final class JsonApiConverter {
    public static final class Builder {
        private final List<Class<?>> mClasses = new ArrayList<>();
        private final List<TypeConverter<?>> mConverters = new ArrayList<>();

        @NonNull
        public Builder addClasses(@NonNull final Class<?>... classes) {
            mClasses.addAll(Arrays.asList(classes));
            return this;
        }

        @NonNull
        public Builder addConverters(@NonNull final TypeConverter<?>... converters) {
            mConverters.addAll(Arrays.asList(converters));
            return this;
        }

        @NonNull
        public JsonApiConverter build() {
            return new JsonApiConverter(mClasses, mConverters);
        }
    }

    private final List<TypeConverter<?>> mConverters = new ArrayList<>();
    private final Set<Class<?>> mSupportedClasses = new HashSet<>();
    private final Map<String, Class<?>> mTypes = new HashMap<>();
    private final Map<Class<?>, List<Field>> mFields = new HashMap<>();

    private JsonApiConverter(@NonNull final List<Class<?>> classes, @NonNull final List<TypeConverter<?>> converters) {
        mConverters.addAll(converters);

        for (final Class<?> c : classes) {
            // throw an exception if the provided class is not a valid JsonApiType
            final String type = getType(c);
            if (type == null) {
                throw new IllegalArgumentException("Class " + c + " is not a valid @JsonApiType");
            }

            // throw an exception if the specified type is already defined
            if (mTypes.containsKey(type)) {
                throw new IllegalArgumentException(
                        "Duplicate @JsonApiType(\"" + type + "\") shared by " + mTypes.get(type) + " and " + c);
            }

            // store this type
            mSupportedClasses.add(c);
            mTypes.put(type, c);
            mFields.put(c, getFields(c));
        }
    }

    public boolean supports(@NonNull final Class<?> c) {
        return mSupportedClasses.contains(c);
    }

    @NonNull
    public String toJson(@NonNull final JsonApiObject<?> obj) {
        try {
            final JSONObject json = new JSONObject();
            if (obj.isSingle()) {
                final Object resource = obj.getDataSingle();
                if (resource == null) {
                    json.put(JSON_DATA, JSONObject.NULL);
                } else {
                    json.put(JSON_DATA, resourceToJson(resource));
                }
            } else {
                final JSONArray dataArr = new JSONArray();
                for (final Object resource : obj.getData()) {
                    dataArr.put(resourceToJson(resource));
                }
                json.put(JSON_DATA, dataArr);
            }

            return json.toString();
        } catch (final JSONException e) {
            throw new RuntimeException("Unexpected JSONException", e);
        }
    }

    @NonNull
    @SuppressWarnings("checkstyle:RightCurly")
    public <T> JsonApiObject<T> fromJson(@NonNull final String json, @NonNull final Class<T> clazz)
            throws JSONException {
        final JSONObject jsonObject = new JSONObject(json);
        final JsonApiObject<T> output;
        if (jsonObject.has(JSON_DATA)) {
            // {data: []}
            if (jsonObject.optJSONArray(JSON_DATA) != null) {
                final JSONArray data = jsonObject.optJSONArray(JSON_DATA);
                output = JsonApiObject.of();
                for (int i = 0; i < data.length(); i++) {
                    final Object resource = fromJson(data.optJSONObject(i));
                    if (clazz.isInstance(resource)) {
                        output.addData(clazz.cast(resource));
                    }
                }
            }
            // {data: null} or {data: {}}
            else {
                output = JsonApiObject.single(null);
                final Object resource = fromJson(jsonObject.optJSONObject(JSON_DATA));
                if (clazz.isInstance(resource)) {
                    output.setData(clazz.cast(resource));
                }
            }
        } else {
            throw new UnsupportedOperationException();
        }
        return output;
    }

    @Nullable
    @SuppressWarnings("checkstyle:RightCurly")
    private JSONObject resourceToJson(@Nullable final Object resource) throws JSONException {
        if (resource == null) {
            return null;
        }
        final Class<?> clazz = resource.getClass();
        final String type = getType(clazz);
        if (!clazz.equals(mTypes.get(type))) {
            throw new IllegalArgumentException(clazz + " is not a valid JsonApi resource type for this converter");
        }

        // create base object
        final JSONObject json = new JSONObject();
        final JSONObject attributes = new JSONObject();
        json.put(JSON_DATA_TYPE, type);
        json.put(JSON_DATA_ATTRIBUTES, attributes);

        // attach all fields
        for (final Field field : mFields.get(clazz)) {
            final JsonApiAttribute attr = field.getAnnotation(JsonApiAttribute.class);

            Object value;
            try {
                value = field.get(resource);
            } catch (final IllegalAccessException e) {
                value = null;
            }

            // skip null values
            if (value == null) {
                continue;
            }

            // handle id fields
            if (field.getAnnotation(JsonApiId.class) != null) {
                json.put(JSON_DATA_ID, value);
            }
            // everything else is a regular attribute
            else {
                final String name = attr != null && attr.name().length() > 0 ? attr.name() : field.getName();
                attributes.put(name, value);
            }
        }

        return json;
    }

    @Nullable
    @SuppressWarnings("checkstyle:RightCurly")
    private Object fromJson(@Nullable final JSONObject json) {
        if (json == null) {
            return null;
        }

        // determine the type
        final Class<?> type = mTypes.get(json.optString(JSON_DATA_TYPE));
        if (type == null) {
            return null;
        }

        // create an instance of this type
        final Object instance;
        try {
            instance = type.newInstance();
        } catch (final Exception e) {
            return null;
        }

        // populate fields
        final JSONObject attributes = json.optJSONObject(JSON_DATA_ATTRIBUTES);
        for (final Field field : mFields.get(type)) {
            final Class<?> fieldType = field.getType();

            try {
                // handle id fields
                if (field.getAnnotation(JsonApiId.class) != null) {
                    field.set(instance, convertFromJSONObject(json, JSON_DATA_ID, fieldType));
                }
                // anything else is an attribute
                else {
                    final JsonApiAttribute attr = field.getAnnotation(JsonApiAttribute.class);
                    final String name = attr != null && attr.name().length() > 0 ? attr.name() : field.getName();
                    field.set(instance, convertFromJSONObject(attributes, name, fieldType));
                }
            } catch (final JSONException | IllegalAccessException ignored) {
            }
        }

        // return the object
        return instance;
    }

    @Nullable
    private String getType(@NonNull final Class<?> clazz) {
        final JsonApiType type = clazz.getAnnotation(JsonApiType.class);
        return type != null ? type.value() : null;
    }

    @NonNull
    private List<Field> getFields(@Nullable final Class<?> type) {
        final List<Field> fields = new ArrayList<>();

        if (type != null && !Object.class.equals(type)) {
            for (final Field field : type.getDeclaredFields()) {
                final int modifiers = field.getModifiers();

                // skip static and transient fields
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue;
                }
                // skip ignored fields
                if (field.getAnnotation(JsonApiIgnore.class) != null) {
                    continue;
                }
                // skip fields we don't support
                final Class<?> fieldClass = field.getType();
                if (!isSupportedType(fieldClass) && !supports(fieldClass)) {
                    continue;
                }

                // set field as accessible and track it
                field.setAccessible(true);
                fields.add(field);
            }

            // process the superclass
            fields.addAll(getFields(type.getSuperclass()));
        }

        return fields;
    }

    private boolean isSupportedType(@NonNull final Class<?> type) {
        // check configured TypeConverters
        for (final TypeConverter<?> converter : mConverters) {
            if (converter.supports(type)) {
                return true;
            }
        }

        // is it a native type?
        return type.isAssignableFrom(boolean.class) || type.isAssignableFrom(double.class) ||
                type.isAssignableFrom(int.class) || type.isAssignableFrom(long.class) ||
                type.isAssignableFrom(Boolean.class) || type.isAssignableFrom(Double.class) ||
                type.isAssignableFrom(Integer.class) || type.isAssignableFrom(Long.class) ||
                type.isAssignableFrom(String.class);
    }

    @Nullable
    private Object convertFromJSONObject(@NonNull final JSONObject json, @NonNull final String name,
                                         @NonNull final Class<?> type) throws JSONException, IllegalAccessException {
        // utilize configured TypeConverters first
        for (final TypeConverter<?> converter : mConverters) {
            if (converter.supports(type)) {
                return converter.fromString(json.optString(name, null));
            }
        }

        // handle native types
        if (type.isAssignableFrom(double.class)) {
            return json.getDouble(name);
        } else if (type.isAssignableFrom(int.class)) {
            return json.getInt(name);
        } else if (type.isAssignableFrom(long.class)) {
            return json.getLong(name);
        } else if (type.isAssignableFrom(boolean.class)) {
            return json.getBoolean(name);
        } else if (type.isAssignableFrom(Boolean.class) || type.isAssignableFrom(Double.class) ||
                type.isAssignableFrom(Integer.class) || type.isAssignableFrom(Long.class) ||
                type.isAssignableFrom(String.class)) {
            final String value = json.optString(name, null);
            try {
                if (type.isAssignableFrom(Boolean.class)) {
                    return Boolean.valueOf(value);
                } else if (type.isAssignableFrom(Double.class)) {
                    return Double.valueOf(value);
                } else if (type.isAssignableFrom(Integer.class)) {
                    return Integer.valueOf(value);
                } else if (type.isAssignableFrom(Long.class)) {
                    return Long.valueOf(value);
                } else if (type.isAssignableFrom(String.class)) {
                    return value;
                }
            } catch (final Exception e) {
                return null;
            }
        }

        return null;
    }
}
