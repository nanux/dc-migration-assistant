package com.atlassian.migration.datacenter.fs.processor.mappers

import com.google.gson.*
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.springframework.beans.factory.FactoryBean
import org.springframework.stereotype.Component
import java.lang.reflect.Type

@Component
class ObjectMapperFactory : FactoryBean<Gson> {
    override fun getObject(): Gson? {
        return GsonBuilder().registerTypeAdapter(DateTime::class.java, DateTimeTypeAdapter()).create()
    }

    override fun getObjectType(): Class<*>? {
        return Gson::class.java
    }

    internal class DateTimeTypeAdapter : JsonSerializer<DateTime?>, JsonDeserializer<DateTime?> {
        @Throws(JsonParseException::class)
        override fun deserialize(json: JsonElement, typeOfT: Type?,
                                 context: JsonDeserializationContext?): DateTime {
            return DateTime.parse(json.asString)
        }

        override fun serialize(src: DateTime?, typeOfSrc: Type?,
                               context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(ISODateTimeFormat
                    .dateTimeNoMillis()
                    .print(src))
        }
    }
}