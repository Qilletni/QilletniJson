package dev.qilletni.lib.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.qilletni.api.lang.types.EntityType;
import dev.qilletni.api.lang.types.JavaType;
import dev.qilletni.api.lang.types.ListType;
import dev.qilletni.api.lang.types.QilletniType;
import dev.qilletni.api.lang.types.StaticEntityType;
import dev.qilletni.api.lang.types.conversion.TypeConverter;
import dev.qilletni.api.lang.types.entity.EntityInitializer;
import dev.qilletni.api.lang.types.list.ListInitializer;
import dev.qilletni.api.lib.annotations.NativeOn;
import dev.qilletni.lib.json.adapters.BooleanTypeAdapterFactory;
import dev.qilletni.lib.json.adapters.DoubleTypeAdapterFactory;
import dev.qilletni.lib.json.adapters.DynamicTypeAdapterFactory;
import dev.qilletni.lib.json.adapters.EntityTypeAdapterFactory;
import dev.qilletni.lib.json.adapters.IntegerTypeAdapterFactory;
import dev.qilletni.lib.json.adapters.ListTypeAdapterFactory;
import dev.qilletni.lib.json.adapters.StringTypeAdapterFactory;
import dev.qilletni.lib.json.exceptions.UnserializableTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class Json {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Json.class);
    
    private final EntityInitializer entityInitializer;
    private final TypeConverter typeConverter;
    private final ListInitializer listInitializer;

    public Json(EntityInitializer entityInitializer, TypeConverter typeConverter, ListInitializer listInitializer) {
        this.entityInitializer = entityInitializer;
        this.typeConverter = typeConverter;
        this.listInitializer = listInitializer;
    }
    
    private void registerTypeAdapters(GsonBuilder gson) {
        gson.registerTypeAdapterFactory(new EntityTypeAdapterFactory(typeConverter, entityInitializer))
                .registerTypeAdapterFactory(new StringTypeAdapterFactory(typeConverter))
                .registerTypeAdapterFactory(new ListTypeAdapterFactory(listInitializer, entityInitializer))
                .registerTypeAdapterFactory(new BooleanTypeAdapterFactory(typeConverter))
                .registerTypeAdapterFactory(new IntegerTypeAdapterFactory(typeConverter))
                .registerTypeAdapterFactory(new DoubleTypeAdapterFactory(typeConverter))
                .registerTypeAdapterFactory(new DynamicTypeAdapterFactory(typeConverter));
    }

    @NativeOn("JsonConverter")
    public QilletniType createJsonConverter(StaticEntityType staticJsonConverter, boolean prettyPrint) {
        var gson = new GsonBuilder();
        
        registerTypeAdapters(gson);
        
        if (prettyPrint) {
            gson.setPrettyPrinting();
        }

        return entityInitializer.initializeEntity("JsonConverter", gson.create(), prettyPrint);
    }

    @NativeOn("JsonConverter")
    public String toJson(EntityType jsonConverter, QilletniType obj) {
        var gson = jsonConverter.getEntityScope().<JavaType>lookup("_gson").getValue().getReference(Gson.class);
        
        if (obj instanceof EntityType entityType && entityType.getEntityDefinition().getTypeName().equals("Map")) {
            LOGGER.debug("Serializing map: {}", obj);
            return gson.toJson(obj);
        }  else if (obj instanceof ListType listType) {
            LOGGER.debug("Serializing list: {}", obj);
            return gson.toJson(listType.getItems());
        }
        
        throw new UnserializableTypeException("Cannot serialize type %s".formatted(obj.typeName()));
    }

    @NativeOn("JsonConverter")
    public QilletniType fromJson(EntityType jsonConverter, String json) {
        var gson = jsonConverter.getEntityScope().<JavaType>lookup("_gson").getValue().getReference(Gson.class);

        JsonElement element = JsonParser.parseString(json);

        if (element.isJsonArray()) {
            LOGGER.debug("DESERIALIZING LIST!");
            return gson.fromJson(json, ListType.class);
        } else if (element.isJsonObject()) {
            LOGGER.debug("DESERIALIZING MAP!");
            var mapContents = gson.fromJson(json, HashMap.class);

            LOGGER.debug("Deserialized map: {}", mapContents);

            var mapEntity = entityInitializer.initializeEntity("Map");
            mapEntity.getEntityScope().<JavaType>lookup("_map").getValue().setReference(mapContents);

            return mapEntity;
        } else {
            throw new IllegalArgumentException("Unsupported JSON type");
        }
    }
}
