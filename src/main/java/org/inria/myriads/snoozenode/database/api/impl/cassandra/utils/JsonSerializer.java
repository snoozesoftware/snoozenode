package org.inria.myriads.snoozenode.database.api.impl.cassandra.utils;

import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import me.prettyprint.cassandra.serializers.AbstractSerializer;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * 
 * Json serializer.
 * 
 * @author msimonin
 *
 */
public class JsonSerializer extends AbstractSerializer<Object> 
{
    /** UTF8.*/
    private static final String UTF_8 = "UTF-8";
    
    /** Charset.*/
    private static final Charset charset = Charset.forName(UTF_8);
    
    /** class. */
    private Class<?> class_;
    
    /**
     * 
     * Constructeur.
     * 
     * @param clazz     the class.
     */
    public JsonSerializer(Class<?> clazz)
    {
        class_ = clazz;
    }

    @Override
    public ByteBuffer toByteBuffer(Object obj) 
    {
        if (obj == null)
        {
            return null;
        }
        
        ObjectMapper mapper = new ObjectMapper();
        Writer strWriter = new StringWriter();
        
        try 
        {
            mapper.writeValue(strWriter, obj);
            String json = strWriter.toString();
            return ByteBuffer.wrap(json.getBytes(charset));
        } 
        catch (Exception e) 
        {
            return null;
        }
        
    }

    @Override
    public Object fromByteBuffer(ByteBuffer byteBuffer) 
    {
        if (byteBuffer == null)
        {
            return null;
        }
        
        String json = charset.decode(byteBuffer).toString();
        ObjectMapper mapper = new ObjectMapper();  
        try 
        {
            Object o = mapper.readValue(json, class_);
            return o;
        } 
        catch (Exception exception)
        {
            return null;
        }
    }

    /**
     * 
     * To String.
     * 
     * @param obj   object to dump.
     * @return String
     */
    public String toString(Object obj)
    {
        if (obj == null)
        {
            return null;
        }
        
        ObjectMapper mapper = new ObjectMapper();
        Writer strWriter = new StringWriter();
        
        try 
        {
            mapper.writeValue(strWriter, obj);
            String json = strWriter.toString();
            return json;
        }
        catch (Exception exception)
        {
            return null;
        }
    }

    /**
     * 
     * from String.
     * 
     * @param string  String to deserialize.  
     * @return  object
     */
    public Object fromString(String string)
    {
        if (string == null)
        {
            return null;
        }
        
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            Object o = mapper.readValue(string, class_);
            return o;
        }
        catch (Exception exception)
        {
            return null;
        }
    }



}
