package org.inria.myriads.snoozenode.database.api.impl.cassandra;

import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.codehaus.jackson.map.ObjectMapper;
import me.prettyprint.cassandra.serializers.AbstractSerializer;

public class JsonSerializer extends AbstractSerializer<Object> 
{
    private static final String UTF_8 = "UTF-8";
    private static final Charset charset = Charset.forName(UTF_8);
    private Class<?> class_;
    
    
    
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
        
        try {
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
