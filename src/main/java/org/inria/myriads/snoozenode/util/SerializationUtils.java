/**
 * Copyright (C) 2010-2013 Eugen Feller, INRIA <eugen.feller@inria.fr>
 *
 * This file is part of Snooze, a scalable, autonomic, and
 * energy-aware virtual machine (VM) management framework.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package org.inria.myriads.snoozenode.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.codehaus.jackson.map.ObjectMapper;
import org.inria.myriads.snoozecommon.guard.Guard;

/**
 * Network utility.
 * 
 * @author Eugen Feller
 */
public final class SerializationUtils 
{
    /**
     * Hide the consturctor.
     */
    private SerializationUtils() 
    {
        throw new UnsupportedOperationException();
    }

    /** 
     * Serialize object.
     *  
     * @param obj                   The object to serialize
     * @return                      The byte array
     * @throws IOException          Exception          
     */
    public static byte[] serializeObject(Object obj) 
        throws IOException 
    {
        Guard.check(obj);
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput); 
        objectOutput.writeObject(obj);
        
        byte[] byteArray = byteOutput.toByteArray();
        byteOutput.close();
        objectOutput.close();
        
        return byteArray;
    }
    
    /** 
     * Deserialize object.
     *  
     * @param byteArray                 The byte array
     * @return                          The return object
     * @throws IOException              Exception                 
     * @throws ClassNotFoundException   Exception   
     */
    public static Object deserializeObject(byte[] byteArray) 
        throws IOException, ClassNotFoundException 
    {
        Guard.check(byteArray);
        
        ByteArrayInputStream byteInput = new ByteArrayInputStream(byteArray);
        ObjectInputStream objectInput = new ObjectInputStream(byteInput);
        Object object = objectInput.readObject();
        return object;
    }

    /**
     * 
     * Serializes object to json.
     * 
     * @param data  The data
     * @return  String (serialiazed)
     * @throws IOException              Exception
     */
    public static String serializeObjectToJSON(Object data) 
            throws  IOException
    {
        
        ObjectMapper mapper = new ObjectMapper();
        Writer strWriter = new StringWriter();
        mapper.writeValue(strWriter, data);
        String json = strWriter.toString();
        return json;
    }
}
