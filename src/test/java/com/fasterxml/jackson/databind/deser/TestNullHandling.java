package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class TestNullHandling extends BaseMapTest
{
    static class FunnyNullDeserializer extends JsonDeserializer<String>
    {
        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            return "text";
        }

        @Override
        public String getNullValue(DeserializationContext ctxt) { return "funny"; }
    }

    static class AnySetter{

        private Map<String,String> any = new HashMap<String,String>();

        @JsonAnySetter
        public void setAny(String name, String value){
            this.any.put(name,value);
        }

        public Map<String,String> getAny(){
            return this.any;
        }
    }

    static class TestObject {
      public String string1 = "foo"; // default value
      public String string2;

      public EmptyObject object1 = new EmptyObject(); // default value
      public EmptyObject object2;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestObject that = (TestObject) o;
            return Objects.equals(string1, that.string1) &&
                Objects.equals(string2, that.string2) &&
                Objects.equals(object1, that.object1) &&
                Objects.equals(object2, that.object2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(string1, string2, object1, object2);
        }
    }

    static class EmptyObject {
        public String foo;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EmptyObject that = (EmptyObject) o;
            return Objects.equals(foo, that.foo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(foo);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testAnySetterNulls() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        String fieldName = "fieldName";
        String nullValue = "{\""+fieldName+"\":null}";

        // should get non-default null directly:
        AnySetter result = mapper.readValue(nullValue, AnySetter.class);

        assertEquals(1, result.getAny().size());
        assertNotNull(result.getAny().get(fieldName));
        assertEquals("funny", result.getAny().get(fieldName));

        // as well as via ObjectReader
        ObjectReader reader = mapper.readerFor(AnySetter.class);
        result = reader.readValue(nullValue);

        assertEquals(1, result.getAny().size());
        assertNotNull(result.getAny().get(fieldName));
        assertEquals("funny", result.getAny().get(fieldName));
    }

    // Test for [JACKSON-643]
    public void testCustomRootNulls() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        // should get non-default null directly:
        String str = mapper.readValue("null", String.class);
        assertNotNull(str);
        assertEquals("funny", str);
        
        // as well as via ObjectReader
        ObjectReader reader = mapper.readerFor(String.class);
        str = reader.readValue("null");
        assertNotNull(str);
        assertEquals("funny", str);
    }

    // Test for [#1269]
    public void test_NEVER_SET_AS_NULL() throws Exception {

        ObjectMapper mapper = new ObjectMapper().enable(MapperFeature.NEVER_SET_AS_NULL);

        // should get non-default null directly:
        String str = mapper.readValue("null", String.class);
        assertNull(str);

        // as well as via ObjectReader
        ObjectReader reader = mapper.readerFor(String.class);
        str = reader.readValue("null");
        assertNull(str);

        JavaType type = mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class);
        Map<?,?> actual = mapper.readValue("{\"key\":null}", type);
        assertNotNull(actual);
        assertEquals(0, actual.size());

        actual = mapper.readValue("{\"foo\":\"bar\", \"fizz\": \"buzz\", \"bing\":null}", type);
        assertNotNull(actual);
        assertEquals(2, actual.size());
        Map<String, String> expected = new HashMap<>();
        expected.put("foo", "bar");
        expected.put("fizz", "buzz");
        assertEquals(expected, actual);

        // POJO
        TestObject expectedObject = new TestObject();
        TestObject actualObject = mapper.readValue("{\"string1\":null,\"string2\":null,\"object1\":null,\"object2\":null}", TestObject.class);
        assertNotNull(actualObject);
        assertEquals(expectedObject, actualObject);
        assertNotNull(actualObject.string1);
        assertNotNull(actualObject.string1.equals("foo"));
        assertNull(actualObject.string2);
        assertNotNull(actualObject.object1);
        assertNotNull(actualObject.object1);
        assertNull(actualObject.object2);
    }

    // Test for [#407]
    public void testListOfNulls() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        List<String> list = Arrays.asList("funny");
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, String.class);

        // should get non-default null directly:
        List<?> deser = mapper.readValue("[null]", type);
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals(list.get(0), deser.get(0));

        // as well as via ObjectReader
        ObjectReader reader = mapper.readerFor(type);
        deser = reader.readValue("[null]");
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals(list.get(0), deser.get(0));
    }

    // Test for [#407]
    public void testMapOfNulls() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        JavaType type = mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class);
        // should get non-default null directly:
        Map<?,?> deser = mapper.readValue("{\"key\":null}", type);
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals("funny", deser.get("key"));

        // as well as via ObjectReader
        ObjectReader reader = mapper.readerFor(type);
        deser = reader.readValue("{\"key\":null}");
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals("funny", deser.get("key"));
    }
}
