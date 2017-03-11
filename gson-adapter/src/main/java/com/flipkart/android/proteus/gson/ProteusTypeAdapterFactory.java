/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION
 *
 * Copyright (c) 2017 Flipkart Internet Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.flipkart.android.proteus.gson;

import android.content.Context;
import android.support.annotation.Nullable;

import com.flipkart.android.proteus.FormatterManager;
import com.flipkart.android.proteus.Proteus;
import com.flipkart.android.proteus.ProteusConstants;
import com.flipkart.android.proteus.ViewTypeParser;
import com.flipkart.android.proteus.value.Array;
import com.flipkart.android.proteus.value.AttributeResource;
import com.flipkart.android.proteus.value.Binding;
import com.flipkart.android.proteus.value.Color;
import com.flipkart.android.proteus.value.Dimension;
import com.flipkart.android.proteus.value.DrawableValue;
import com.flipkart.android.proteus.value.DrawableValue.LevelListValue;
import com.flipkart.android.proteus.value.DrawableValue.RippleValue;
import com.flipkart.android.proteus.value.Layout;
import com.flipkart.android.proteus.value.NestedBinding;
import com.flipkart.android.proteus.value.Null;
import com.flipkart.android.proteus.value.ObjectValue;
import com.flipkart.android.proteus.value.Primitive;
import com.flipkart.android.proteus.value.Resource;
import com.flipkart.android.proteus.value.StyleResource;
import com.flipkart.android.proteus.value.Value;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.JsonReaderInternalAccess;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * ProteusTypeAdapterFactory
 *
 * @author aditya.sharat
 */
public class ProteusTypeAdapterFactory implements TypeAdapterFactory {

    public static final ProteusInstanceHolder PROTEUS_INSTANCE_HOLDER = new ProteusInstanceHolder();

    private Context context;

    /**
     *
     */
    public final TypeAdapter<Value> VALUE_TYPE_ADAPTER = new TypeAdapter<Value>() {
        @Override
        public void write(JsonWriter out, Value value) throws IOException {
            throw new UnsupportedOperationException("Use ProteusTypeAdapterFactory.COMPILED_VALUE_TYPE_ADAPTER instead");
        }

        @Override
        public Value read(JsonReader in) throws IOException {
            switch (in.peek()) {
                case STRING:
                    return compileString(in.nextString());
                case NUMBER:
                    String number = in.nextString();
                    return new Primitive(new LazilyParsedNumber(number));
                case BOOLEAN:
                    return new Primitive(in.nextBoolean());
                case NULL:
                    in.nextNull();
                    return Null.INSTANCE;
                case BEGIN_ARRAY:
                    Array array = new Array();
                    in.beginArray();
                    while (in.hasNext()) {
                        array.add(read(in));
                    }
                    in.endArray();
                    return array;
                case BEGIN_OBJECT:
                    ObjectValue object = new ObjectValue();
                    in.beginObject();
                    if (in.hasNext()) {
                        String name = in.nextName();
                        if (ProteusConstants.TYPE.equals(name) && JsonToken.STRING.equals(in.peek())) {
                            String type = in.nextString();
                            if (PROTEUS_INSTANCE_HOLDER.isLayout(type)) {
                                Layout layout = LAYOUT_TYPE_ADAPTER.read(type, PROTEUS_INSTANCE_HOLDER.getProteus(), in);
                                in.endObject();
                                return layout;
                            } else {
                                object.add(name, compileString(type));
                            }
                        } else {
                            object.add(name, read(in));
                        }
                    }
                    while (in.hasNext()) {
                        object.add(in.nextName(), read(in));
                    }
                    in.endObject();
                    return object;
                case END_DOCUMENT:
                case NAME:
                case END_OBJECT:
                case END_ARRAY:
                default:
                    throw new IllegalArgumentException();
            }
        }
    }.nullSafe();

    /**
     *
     */
    public final TypeAdapter<Primitive> PRIMITIVE_TYPE_ADAPTER = new TypeAdapter<Primitive>() {

        @Override
        public void write(JsonWriter out, Primitive value) throws IOException {
            VALUE_TYPE_ADAPTER.write(out, value);
        }

        @Override
        public Primitive read(JsonReader in) throws IOException {
            Value value = VALUE_TYPE_ADAPTER.read(in);
            return value != null && value.isPrimitive() ? value.getAsPrimitive() : null;
        }
    }.nullSafe();

    /**
     *
     */
    public final TypeAdapter<ObjectValue> OBJECT_TYPE_ADAPTER = new TypeAdapter<ObjectValue>() {
        @Override
        public void write(JsonWriter out, ObjectValue value) throws IOException {
            VALUE_TYPE_ADAPTER.write(out, value);
        }

        @Override
        public ObjectValue read(JsonReader in) throws IOException {
            Value value = VALUE_TYPE_ADAPTER.read(in);
            return value != null && value.isObject() ? value.getAsObject() : null;
        }
    }.nullSafe();

    /**
     *
     */
    public final TypeAdapter<Array> ARRAY_TYPE_ADAPTER = new TypeAdapter<Array>() {
        @Override
        public void write(JsonWriter out, Array value) throws IOException {
            VALUE_TYPE_ADAPTER.write(out, value);
        }

        @Override
        public Array read(JsonReader in) throws IOException {
            Value value = VALUE_TYPE_ADAPTER.read(in);
            return value != null && value.isArray() ? value.getAsArray() : null;
        }
    }.nullSafe();

    /**
     *
     */
    public final TypeAdapter<Null> NULL_TYPE_ADAPTER = new TypeAdapter<Null>() {

        @Override
        public void write(JsonWriter out, Null value) throws IOException {
            VALUE_TYPE_ADAPTER.write(out, value);
        }

        @Override
        public Null read(JsonReader in) throws IOException {
            Value value = VALUE_TYPE_ADAPTER.read(in);
            return value != null && value.isNull() ? value.getAsNull() : null;
        }
    }.nullSafe();

    /**
     *
     */
    public final LayoutTypeAdapter LAYOUT_TYPE_ADAPTER = new LayoutTypeAdapter();

    /**
     *
     */
    public final TypeAdapter<Value> COMPILED_VALUE_TYPE_ADAPTER = new TypeAdapter<Value>() {

        public static final String TYPE = "$t";
        public static final String VALUE = "$v";

        @Override
        public void write(JsonWriter out, Value value) throws IOException {
            if (value == null || value.isNull()) {
                out.nullValue();
            } else if (value.isPrimitive()) {
                Primitive primitive = value.getAsPrimitive();
                if (primitive.isNumber()) {
                    out.value(primitive.getAsNumber());
                } else if (primitive.isBoolean()) {
                    out.value(primitive.getAsBoolean());
                } else {
                    out.value(primitive.getAsString());
                }
            } else if (value.isObject()) {
                out.beginObject();
                for (Map.Entry<String, Value> e : value.getAsObject().entrySet()) {
                    out.name(e.getKey());
                    write(out, e.getValue());
                }
                out.endObject();
            } else if (value.isArray()) {
                out.beginArray();
                Iterator<Value> iterator = value.getAsArray().iterator();
                while (iterator.hasNext()) {
                    write(out, iterator.next());
                }
                out.endArray();
            } else {
                CustomValueTypeAdapter adapter = getCustomValueTypeAdapter(value.getClass());

                out.beginObject();

                out.name(TYPE);
                out.value(adapter.type);

                out.name(VALUE);
                //noinspection unchecked
                adapter.write(out, value);

                out.endObject();
            }
        }

        @Override
        public Value read(JsonReader in) throws IOException {
            switch (in.peek()) {
                case STRING:
                    return compileString(in.nextString());
                case NUMBER:
                    String number = in.nextString();
                    return new Primitive(new LazilyParsedNumber(number));
                case BOOLEAN:
                    return new Primitive(in.nextBoolean());
                case NULL:
                    in.nextNull();
                    return Null.INSTANCE;
                case BEGIN_ARRAY:
                    Array array = new Array();
                    in.beginArray();
                    while (in.hasNext()) {
                        array.add(read(in));
                    }
                    in.endArray();
                    return array;
                case BEGIN_OBJECT:
                    ObjectValue object = new ObjectValue();
                    in.beginObject();
                    if (in.hasNext()) {
                        String name = in.nextName();
                        if (TYPE.equals(name) && JsonToken.NUMBER.equals(in.peek())) {
                            int type = Integer.parseInt(in.nextString());
                            CustomValueTypeAdapter<? extends Value> adapter = getCustomValueTypeAdapter(type);
                            in.nextName();
                            Value value = adapter.read(in);
                            in.endObject();
                            return value;
                        } else {
                            object.add(name, read(in));
                        }
                    }
                    while (in.hasNext()) {
                        object.add(in.nextName(), read(in));
                    }
                    in.endObject();
                    return object;
                case END_DOCUMENT:
                case NAME:
                case END_OBJECT:
                case END_ARRAY:
                default:
                    throw new IllegalArgumentException();
            }
        }

    };

    /**
     *
     */
    public final CustomValueTypeAdapterCreator<AttributeResource> ATTRIBUTE_RESOURCE = new CustomValueTypeAdapterCreator<AttributeResource>() {
        @Override
        public CustomValueTypeAdapter<AttributeResource> create(int type) {
            return new CustomValueTypeAdapter<AttributeResource>(type) {
                @Override
                public void write(JsonWriter out, AttributeResource value) throws IOException {
                    out.value(value.attributeId);
                }

                @Override
                public AttributeResource read(JsonReader in) throws IOException {
                    return AttributeResource.valueOf(Integer.parseInt(in.nextString()));
                }
            };
        }
    };

    /**
     *
     */
    public final CustomValueTypeAdapterCreator<Binding> BINDING = new CustomValueTypeAdapterCreator<Binding>() {
        @Override
        public CustomValueTypeAdapter<Binding> create(int type) {
            return new CustomValueTypeAdapter<Binding>(type) {

                @Override
                public void write(JsonWriter out, Binding value) throws IOException {
                    out.value(value.toString());
                }

                @Override
                public Binding read(JsonReader in) throws IOException {
                    return Binding.valueOf(in.nextString(), PROTEUS_INSTANCE_HOLDER.getProteus().formatterManager);
                }
            };
        }
    };

    /**
     *
     */
    public final CustomValueTypeAdapterCreator<Color.Int> COLOR_INT = new CustomValueTypeAdapterCreator<Color.Int>() {
        @Override
        public CustomValueTypeAdapter<Color.Int> create(int type) {
            return new CustomValueTypeAdapter<Color.Int>(type) {
                @Override
                public void write(JsonWriter out, Color.Int color) throws IOException {
                    out.value(color.value);
                }

                @Override
                public Color.Int read(JsonReader in) throws IOException {
                    return Color.Int.valueOf(Integer.parseInt(in.nextString()));
                }
            };
        }
    };

    /**
     *
     */
    public final CustomValueTypeAdapterCreator<Color.StateList> COLOR_STATE_LIST = new CustomValueTypeAdapterCreator<Color.StateList>() {
        @Override
        public CustomValueTypeAdapter<Color.StateList> create(int type) {
            return new CustomValueTypeAdapter<Color.StateList>(type) {

                private final String KEY_STATES = "s";
                private final String KEY_COLORS = "c";

                @Override
                public void write(JsonWriter out, Color.StateList value) throws IOException {
                    out.beginObject();

                    out.name(KEY_STATES);
                    out.value(writeArrayOfIntArrays(value.states));

                    out.name(KEY_COLORS);
                    out.value(writeArrayOfInts(value.colors));

                    out.endObject();
                }

                @Override
                public Color.StateList read(JsonReader in) throws IOException {
                    in.beginObject();

                    in.nextName();
                    int[][] states = readArrayOfIntArrays(in.nextString());

                    in.nextName();
                    int colors[] = readArrayOfInts(in.nextString());

                    Color.StateList color = Color.StateList.valueOf(states, colors);

                    in.endObject();
                    return color;
                }
            };
        }
    };

    /**
     *
     */
    public final CustomValueTypeAdapterCreator<Dimension> DIMENSION = new CustomValueTypeAdapterCreator<Dimension>() {
        @Override
        public CustomValueTypeAdapter<Dimension> create(int type) {
            return new CustomValueTypeAdapter<Dimension>(type) {
                @Override
                public void write(JsonWriter out, Dimension value) throws IOException {
                    out.value(value.toString());
                }

                @Override
                public Dimension read(JsonReader in) throws IOException {
                    return Dimension.valueOf(in.nextString());
                }
            };
        }
    };

    /**
     *
     */
    public final CustomValueTypeAdapterCreator<DrawableValue.ColorValue> DRAWABLE_COLOR = new CustomValueTypeAdapterCreator<DrawableValue.ColorValue>() {
        @Override
        public CustomValueTypeAdapter<DrawableValue.ColorValue> create(int type) {
            return new CustomValueTypeAdapter<DrawableValue.ColorValue>(type) {
                @Override
                public void write(JsonWriter out, DrawableValue.ColorValue value) throws IOException {
                    COMPILED_VALUE_TYPE_ADAPTER.write(out, value.color);
                }

                @Override
                public DrawableValue.ColorValue read(JsonReader in) throws IOException {
                    return DrawableValue.ColorValue.valueOf(COMPILED_VALUE_TYPE_ADAPTER.read(in), getContext());
                }
            };
        }
    };

    public final CustomValueTypeAdapterCreator<DrawableValue.LayerListValue> DRAWABLE_LAYER_LIST = new CustomValueTypeAdapterCreator<DrawableValue.LayerListValue>() {
        @Override
        public CustomValueTypeAdapter<DrawableValue.LayerListValue> create(int type) {
            return new CustomValueTypeAdapter<DrawableValue.LayerListValue>(type) {

                private static final String KEY_IDS = "i";
                private static final String KEY_LAYERS = "l";

                @Override
                public void write(JsonWriter out, DrawableValue.LayerListValue value) throws IOException {

                    out.beginObject();

                    out.name(KEY_IDS);
                    Iterator<Integer> i = value.getIds();
                    out.beginArray();
                    while (i.hasNext()) {
                        out.value(i.next());
                    }
                    out.endArray();

                    out.name(KEY_LAYERS);
                    Iterator<Value> l = value.getLayers();
                    out.beginArray();
                    while (l.hasNext()) {
                        COMPILED_VALUE_TYPE_ADAPTER.write(out, l.next());
                    }
                    out.endArray();

                    out.endObject();
                }

                @Override
                public DrawableValue.LayerListValue read(JsonReader in) throws IOException {

                    in.beginObject();

                    in.nextName();
                    int[] ids = new int[0];
                    in.beginArray();
                    while (in.hasNext()) {
                        ids = Arrays.copyOf(ids, ids.length + 1);
                        ids[ids.length - 1] = Integer.parseInt(in.nextString());
                    }
                    in.endArray();

                    in.nextName();
                    Value[] layers = new Value[0];
                    in.beginArray();
                    while (in.hasNext()) {
                        layers = Arrays.copyOf(layers, layers.length + 1);
                        layers[layers.length - 1] = COMPILED_VALUE_TYPE_ADAPTER.read(in);
                    }
                    in.endArray();

                    in.endObject();

                    return DrawableValue.LayerListValue.valueOf(ids, layers);
                }
            };
        }
    };

    public final CustomValueTypeAdapterCreator<DrawableValue.LevelListValue> DRAWABLE_LEVEL_LIST = new CustomValueTypeAdapterCreator<DrawableValue.LevelListValue>() {
        @Override
        public CustomValueTypeAdapter<DrawableValue.LevelListValue> create(int type) {
            return new CustomValueTypeAdapter<DrawableValue.LevelListValue>(type) {

                private static final String KEY_MIN_LEVEL = "i";
                private static final String KEY_MAX_LEVEL = "a";
                private static final String KEY_DRAWABLE = "d";

                @Override
                public void write(JsonWriter out, DrawableValue.LevelListValue value) throws IOException {
                    out.beginArray();
                    Iterator<LevelListValue.Level> iterator = value.getLevels();
                    LevelListValue.Level level;
                    while (iterator.hasNext()) {
                        level = iterator.next();

                        out.beginObject();

                        out.name(KEY_MIN_LEVEL);
                        out.value(level.minLevel);

                        out.name(KEY_MAX_LEVEL);
                        out.value(level.maxLevel);

                        out.name(KEY_DRAWABLE);
                        COMPILED_VALUE_TYPE_ADAPTER.write(out, level.drawable);

                        out.endObject();
                    }
                    out.endArray();
                }

                @Override
                public DrawableValue.LevelListValue read(JsonReader in) throws IOException {
                    LevelListValue.Level[] levels = new LevelListValue.Level[0];

                    in.beginArray();
                    int minLevel, maxLevel;
                    Value drawable;
                    LevelListValue.Level level;

                    while (in.hasNext()) {
                        in.beginObject();

                        in.nextName();
                        minLevel = Integer.parseInt(in.nextString());

                        in.nextName();
                        maxLevel = Integer.parseInt(in.nextString());

                        in.nextName();
                        drawable = COMPILED_VALUE_TYPE_ADAPTER.read(in);

                        level = LevelListValue.Level.valueOf(minLevel, maxLevel, drawable, getContext());

                        levels = Arrays.copyOf(levels, levels.length + 1);
                        levels[levels.length - 1] = level;

                        in.endObject();
                    }

                    in.endArray();

                    return LevelListValue.value(levels);
                }
            };
        }
    };

    /**
     *
     */
    public final CustomValueTypeAdapterCreator<DrawableValue> DRAWABLE_VALUE = new CustomValueTypeAdapterCreator<DrawableValue>() {
        @Override
        public CustomValueTypeAdapter<DrawableValue> create(int type) {
            return new CustomValueTypeAdapter<DrawableValue>(type) {
                @Override
                public void write(JsonWriter out, DrawableValue value) throws IOException {
                    out.value("#00000000");
                }

                @Override
                public DrawableValue read(JsonReader in) throws IOException {
                    return DrawableValue.valueOf(in.nextString(), getContext());
                }
            };
        }
    };

    public final CustomValueTypeAdapterCreator<RippleValue> DRAWABLE_RIPPLE = new CustomValueTypeAdapterCreator<RippleValue>() {
        @Override
        public CustomValueTypeAdapter<RippleValue> create(int type) {
            return new CustomValueTypeAdapter<RippleValue>(type) {

                private static final String KEY_COLOR = "c";
                private static final String KEY_MASK = "m";
                private static final String KEY_CONTENT = "t";
                private static final String KEY_DEFAULT_BACKGROUND = "d";

                @Override
                public void write(JsonWriter out, RippleValue value) throws IOException {
                    out.beginObject();

                    out.name(KEY_COLOR);
                    COMPILED_VALUE_TYPE_ADAPTER.write(out, value.color);

                    if (value.mask != null) {
                        out.name(KEY_MASK);
                        COMPILED_VALUE_TYPE_ADAPTER.write(out, value.mask);
                    }

                    if (value.content != null) {
                        out.name(KEY_CONTENT);
                        COMPILED_VALUE_TYPE_ADAPTER.write(out, value.content);
                    }

                    if (value.defaultBackground != null) {
                        out.name(KEY_DEFAULT_BACKGROUND);
                        COMPILED_VALUE_TYPE_ADAPTER.write(out, value.defaultBackground);
                    }

                    out.endObject();
                }

                @Override
                public RippleValue read(JsonReader in) throws IOException {

                    in.beginObject();

                    String name;
                    Value color = null, mask = null, content = null, defaultBackground = null;

                    while (in.hasNext()) {
                        name = in.nextName();
                        switch (name) {
                            case KEY_COLOR:
                                color = COMPILED_VALUE_TYPE_ADAPTER.read(in);
                                break;
                            case KEY_MASK:
                                mask = COMPILED_VALUE_TYPE_ADAPTER.read(in);
                                break;
                            case KEY_CONTENT:
                                content = COMPILED_VALUE_TYPE_ADAPTER.read(in);
                                break;
                            case KEY_DEFAULT_BACKGROUND:
                                defaultBackground = COMPILED_VALUE_TYPE_ADAPTER.read(in);
                                break;
                            default:
                                throw new IllegalStateException("Bad attribute '" + name + "'");
                        }
                    }

                    in.endObject();

                    if (color == null) {
                        throw new IllegalStateException("color is a required attribute in Ripple Drawable");
                    }

                    return RippleValue.valueOf(color, mask, content, defaultBackground);
                }
            };
        }
    };

    public final CustomValueTypeAdapterCreator<DrawableValue.UrlValue> DRAWABLE_URL = new CustomValueTypeAdapterCreator<DrawableValue.UrlValue>() {
        @Override
        public CustomValueTypeAdapter<DrawableValue.UrlValue> create(int type) {
            return new CustomValueTypeAdapter<DrawableValue.UrlValue>(type) {
                @Override
                public void write(JsonWriter out, DrawableValue.UrlValue value) throws IOException {
                    out.value(value.url);
                }

                @Override
                public DrawableValue.UrlValue read(JsonReader in) throws IOException {
                    return DrawableValue.UrlValue.valueOf(in.nextString());
                }
            };
        }
    };

    /**
     *
     */
    public final CustomValueTypeAdapterCreator<Layout> LAYOUT = new CustomValueTypeAdapterCreator<Layout>() {
        @Override
        public CustomValueTypeAdapter<Layout> create(int type) {
            return new CustomValueTypeAdapter<Layout>(type) {

                private static final String KEY_TYPE = "t";
                private static final String KEY_DATA = "d";
                private static final String KEY_ATTRIBUTES = "a";
                private static final String KEY_EXTRAS = "e";
                private static final String KEY_ATTRIBUTE_ID = "i";
                private static final String KEY_ATTRIBUTE_VALUE = "v";

                @Override
                public void write(JsonWriter out, Layout value) throws IOException {
                    out.beginObject();

                    out.name(KEY_TYPE);
                    out.value(value.type);

                    if (null != value.data) {
                        out.name(KEY_DATA);
                        out.beginObject();
                        for (Map.Entry<String, Value> entry : value.data.entrySet()) {
                            out.name(entry.getKey());
                            COMPILED_VALUE_TYPE_ADAPTER.write(out, entry.getValue());
                        }
                        out.endObject();
                    }


                    if (null != value.attributes) {
                        out.name(KEY_ATTRIBUTES);
                        out.beginArray();
                        for (Layout.Attribute attribute : value.attributes) {
                            out.beginObject();

                            out.name(KEY_ATTRIBUTE_ID);
                            out.value(attribute.id);

                            out.name(KEY_ATTRIBUTE_VALUE);
                            COMPILED_VALUE_TYPE_ADAPTER.write(out, attribute.value);

                            out.endObject();
                        }
                        out.endArray();
                    }

                    if (null != value.extras) {
                        out.name(KEY_EXTRAS);
                        COMPILED_VALUE_TYPE_ADAPTER.write(out, value.extras);
                    }

                    out.endObject();
                }

                @Override
                public Layout read(JsonReader in) throws IOException {
                    in.beginObject();

                    String name;
                    String type = null;
                    Map<String, Value> data = null;
                    List<Layout.Attribute> attributes = null;
                    ObjectValue extras = null;

                    while (in.hasNext()) {
                        name = in.nextName();
                        switch (name) {
                            case KEY_TYPE:
                                type = in.nextString();
                                break;
                            case KEY_DATA:
                                data = readData(in);
                                break;
                            case KEY_ATTRIBUTES:
                                attributes = readAttributes(in);
                                break;
                            case KEY_EXTRAS:
                                extras = readExtras(in);
                                break;
                            default:
                                throw new IllegalStateException("Bad attribute '" + name + "'");
                        }
                    }

                    in.endObject();

                    if (null == type) {
                        throw new IllegalStateException("Layout must have type attribute!");
                    }

                    //noinspection ConstantConditions
                    return new Layout(type, attributes, data, extras);
                }

                @Nullable
                private Map<String, Value> readData(JsonReader in) throws IOException {
                    Map<String, Value> data = new HashMap<>();
                    in.beginObject();
                    String name;
                    Value value;
                    while (in.hasNext()) {
                        name = in.nextName();
                        value = COMPILED_VALUE_TYPE_ADAPTER.read(in);
                        data.put(name, value);
                    }
                    in.endObject();
                    return data;
                }

                @Nullable
                private ObjectValue readExtras(JsonReader in) throws IOException {
                    return COMPILED_VALUE_TYPE_ADAPTER.read(in).getAsObject();
                }

                @Nullable
                private List<Layout.Attribute> readAttributes(JsonReader in) throws IOException {
                    List<Layout.Attribute> attributes = new ArrayList<>();

                    in.beginArray();
                    int id;
                    Value value;
                    while (in.hasNext()) {
                        in.beginObject();
                        in.nextName();
                        id = Integer.parseInt(in.nextString());
                        in.nextName();
                        value = COMPILED_VALUE_TYPE_ADAPTER.read(in);
                        attributes.add(new Layout.Attribute(id, value));
                        in.endObject();
                    }
                    in.endArray();

                    return attributes;
                }
            };
        }
    };

    /**
     *
     */
    public final CustomValueTypeAdapterCreator<NestedBinding> NESTED_BINDING = new CustomValueTypeAdapterCreator<NestedBinding>() {
        @Override
        public CustomValueTypeAdapter<NestedBinding> create(int type) {
            return new CustomValueTypeAdapter<NestedBinding>(type) {
                @Override
                public void write(JsonWriter out, NestedBinding value) throws IOException {
                    COMPILED_VALUE_TYPE_ADAPTER.write(out, value.getValue());
                }

                @Override
                public NestedBinding read(JsonReader in) throws IOException {
                    return NestedBinding.valueOf(COMPILED_VALUE_TYPE_ADAPTER.read(in));
                }
            };
        }
    };

    /**
     *
     */
    public final CustomValueTypeAdapterCreator<Resource> RESOURCE = new CustomValueTypeAdapterCreator<Resource>() {
        @Override
        public CustomValueTypeAdapter<Resource> create(int type) {
            return new CustomValueTypeAdapter<Resource>(type) {
                @Override
                public void write(JsonWriter out, Resource value) throws IOException {
                    out.value(value.resId);
                }

                @Override
                public Resource read(JsonReader in) throws IOException {
                    return Resource.valueOf(Integer.parseInt(in.nextString()));
                }
            };
        }
    };

    /**
     *
     */
    public final CustomValueTypeAdapterCreator<StyleResource> STYLE_RESOURCE = new CustomValueTypeAdapterCreator<StyleResource>() {
        @Override
        public CustomValueTypeAdapter<StyleResource> create(int type) {
            return new CustomValueTypeAdapter<StyleResource>(type) {

                private static final String KEY_ATTRIBUTE_ID = "a";
                private static final String KEY_STYLE_ID = "s";

                @Override
                public void write(JsonWriter out, StyleResource value) throws IOException {
                    out.beginObject();

                    out.name(KEY_ATTRIBUTE_ID);
                    out.value(value.attributeId);

                    out.name(KEY_STYLE_ID);
                    out.value(value.styleId);

                    out.endObject();
                }

                @Override
                public StyleResource read(JsonReader in) throws IOException {
                    in.beginObject();
                    in.nextName();
                    String attributeId = in.nextString();
                    in.nextName();
                    String styleId = in.nextString();
                    in.endObject();
                    return StyleResource.valueOf(Integer.parseInt(styleId), Integer.parseInt(attributeId));
                }
            };
        }
    };

    /**
     *
     */
    private CustomValueTypeAdapterMap map = new CustomValueTypeAdapterMap();

    private static final String ARRAYS_DELIMITER = "|";
    private static final String ARRAY_DELIMITER = ",";

    public static String writeArrayOfInts(int[] array) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < array.length; index++) {
            builder.append(array[index]);
            if (index < array.length - 1) {
                builder.append(ARRAY_DELIMITER);
            }
        }
        return builder.toString();
    }

    public static String writeArrayOfIntArrays(int[][] arrays) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < arrays.length; index++) {
            builder.append(writeArrayOfInts(arrays[index]));
            if (index < arrays.length - 1) {
                builder.append(ARRAYS_DELIMITER);
            }
        }
        return builder.toString();
    }

    public static int[] readArrayOfInts(String string) {
        int[] array = new int[0];
        StringTokenizer tokenizer = new StringTokenizer(string, ARRAY_DELIMITER);
        while (tokenizer.hasMoreTokens()) {
            array = Arrays.copyOf(array, array.length + 1);
            array[array.length - 1] = Integer.parseInt(tokenizer.nextToken());
        }
        return array;
    }

    public static int[][] readArrayOfIntArrays(String string) {
        int[][] arrays = new int[0][];
        StringTokenizer tokenizer = new StringTokenizer(string, ARRAYS_DELIMITER);
        while (tokenizer.hasMoreTokens()) {
            arrays = Arrays.copyOf(arrays, arrays.length + 1);
            arrays[arrays.length - 1] = readArrayOfInts(tokenizer.nextToken());
        }
        return arrays;
    }

    /**
     * @param context
     */
    public ProteusTypeAdapterFactory(Context context) {

        this.context = context;

        register(AttributeResource.class, ATTRIBUTE_RESOURCE);
        register(Binding.class, BINDING);
        register(Color.Int.class, COLOR_INT);
        register(Color.StateList.class, COLOR_STATE_LIST);
        register(Dimension.class, DIMENSION);

        register(DrawableValue.ColorValue.class, DRAWABLE_COLOR);
        register(DrawableValue.LayerListValue.class, DRAWABLE_LAYER_LIST);
        register(DrawableValue.LevelListValue.class, DRAWABLE_LEVEL_LIST);
        register(DrawableValue.RippleValue.class, DRAWABLE_RIPPLE);
        register(DrawableValue.ShapeValue.class, DRAWABLE_VALUE);
        register(DrawableValue.StateListValue.class, DRAWABLE_VALUE);
        register(DrawableValue.UrlValue.class, DRAWABLE_URL);

        register(Layout.class, LAYOUT);
        register(NestedBinding.class, NESTED_BINDING);
        register(Resource.class, RESOURCE);
        register(StyleResource.class, STYLE_RESOURCE);
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class clazz = type.getRawType();

        if (clazz == Primitive.class) {
            //noinspection unchecked
            return (TypeAdapter<T>) PRIMITIVE_TYPE_ADAPTER;
        } else if (clazz == ObjectValue.class) {
            //noinspection unchecked
            return (TypeAdapter<T>) OBJECT_TYPE_ADAPTER;
        } else if (clazz == Array.class) {
            //noinspection unchecked
            return (TypeAdapter<T>) ARRAY_TYPE_ADAPTER;
        } else if (clazz == Null.class) {
            //noinspection unchecked
            return (TypeAdapter<T>) NULL_TYPE_ADAPTER;
        } else if (clazz == Layout.class) {
            //noinspection unchecked
            return (TypeAdapter<T>) LAYOUT_TYPE_ADAPTER;
        } else if (clazz == Value.class) {
            //noinspection unchecked
            return (TypeAdapter<T>) VALUE_TYPE_ADAPTER;
        }

        return null;
    }

    public void register(Class<? extends Value> clazz, CustomValueTypeAdapterCreator<? extends Value> creator) {
        map.register(clazz, creator);
    }

    public CustomValueTypeAdapter<? extends Value> getCustomValueTypeAdapter(Class<? extends Value> clazz) {
        return map.get(clazz);
    }

    public CustomValueTypeAdapter<? extends Value> getCustomValueTypeAdapter(int type) {
        return map.get(type);
    }

    private Context getContext() {
        return context;
    }

    private static Value compileString(String string) {
        if (Binding.isBindingValue(string)) {
            return Binding.valueOf(string.substring(1), PROTEUS_INSTANCE_HOLDER.getProteus().formatterManager);
        } else {
            return new Primitive(string);
        }
    }

    public static class ProteusInstanceHolder {

        private Proteus proteus;

        private ProteusInstanceHolder() {
        }

        public Proteus getProteus() {
            return proteus;
        }

        public void setProteus(Proteus proteus) {
            this.proteus = proteus;
        }

        public boolean isLayout(String type) {
            return null != proteus && proteus.has(type);
        }
    }

    public static abstract class CustomValueTypeAdapter<V extends Value> extends TypeAdapter<V> {

        public final int type;

        protected CustomValueTypeAdapter(int type) {
            this.type = type;
        }

    }

    public static abstract class CustomValueTypeAdapterCreator<V extends Value> {

        public abstract CustomValueTypeAdapter<V> create(int type);

    }

    private class LayoutTypeAdapter extends TypeAdapter<Layout> {

        @Override
        public void write(JsonWriter out, Layout value) throws IOException {
            VALUE_TYPE_ADAPTER.write(out, value);
        }

        @Override
        public Layout read(JsonReader in) throws IOException {
            Value value = VALUE_TYPE_ADAPTER.read(in);
            return value != null && value.isLayout() ? value.getAsLayout() : null;
        }

        public Layout read(String type, Proteus proteus, JsonReader in) throws IOException {
            List<Layout.Attribute> attributes = new ArrayList<>();
            Map<String, Value> data = null;
            ObjectValue extras = new ObjectValue();
            String name;
            while (in.hasNext()) {
                name = in.nextName();
                if (ProteusConstants.DATA.equals(name)) {
                    data = readData(in);
                } else {
                    ViewTypeParser.AttributeSet.Attribute attribute = proteus.getAttributeId(name, type);
                    if (null != attribute) {
                        FormatterManager manager = PROTEUS_INSTANCE_HOLDER.getProteus().formatterManager;
                        Value value = attribute.processor.precompile(VALUE_TYPE_ADAPTER.read(in), getContext(), manager);
                        attributes.add(new Layout.Attribute(attribute.id, value));
                    } else {
                        extras.add(name, VALUE_TYPE_ADAPTER.read(in));
                    }
                }
            }
            return new Layout(type, attributes.size() > 0 ? attributes : null, data, extras.entrySet().size() > 0 ? extras : null);
        }

        public Map<String, Value> readData(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                in.nextNull();
                return new HashMap<>();
            }

            if (peek != JsonToken.BEGIN_OBJECT) {
                throw new JsonSyntaxException("data must be a Map<String, String>.");
            }

            Map<String, Value> data = new HashMap<>();

            in.beginObject();
            while (in.hasNext()) {
                JsonReaderInternalAccess.INSTANCE.promoteNameToValue(in);
                String key = in.nextString();
                Value value = VALUE_TYPE_ADAPTER.read(in);
                Value replaced = data.put(key, value);
                if (replaced != null) {
                    throw new JsonSyntaxException("duplicate key: " + key);
                }
            }
            in.endObject();

            return data;
        }
    }

    private class CustomValueTypeAdapterMap {

        private final Map<Class<? extends Value>, CustomValueTypeAdapter<? extends Value>> types = new HashMap<>();

        private CustomValueTypeAdapter<? extends Value>[] adapters = new CustomValueTypeAdapter[0];

        public CustomValueTypeAdapter<? extends Value> register(Class<? extends Value> clazz, CustomValueTypeAdapterCreator creator) {
            CustomValueTypeAdapter<? extends Value> adapter = types.get(clazz);
            if (null != adapter) {
                return adapter;
            }
            //noinspection unchecked
            adapter = creator.create(adapters.length);
            adapters = Arrays.copyOf(adapters, adapters.length + 1);
            adapters[adapters.length - 1] = adapter;
            return types.put(clazz, adapter);
        }

        public CustomValueTypeAdapter<? extends Value> get(Class<? extends Value> clazz) {
            CustomValueTypeAdapter i = types.get(clazz);
            if (null == i) {
                throw new IllegalArgumentException(clazz.getName() + " is not a known value type! Remember to register the class first");
            }
            return types.get(clazz);
        }

        public CustomValueTypeAdapter<? extends Value> get(int i) {
            if (i < adapters.length) {
                return adapters[i];
            }
            throw new IllegalArgumentException(i + " is not a known value type! Did you conjure up this int?");
        }
    }
}
