/**
 * Copyright (C) 2010-2012 Eugen Feller, INRIA <eugen.feller@inria.fr>
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
package org.inria.myriads.snoozenode.groupmanager.virtualclusterparser.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.exception.VirtualMachineMonitoringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Virtual cluster parser utilities.
 * 
 * @author Eugen Feller
 */
public final class VirtualClusterParserUtils 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(VirtualClusterParserUtils.class);
    
    /** Hide constructor. */
    private VirtualClusterParserUtils()
    {
        throw new UnsupportedOperationException();
    }
   
    /**
     * Replaces the MAC address inside libvirt template.
     * 
     * @param template          The template
     * @param newMacAddress     The new mac address
     * @return                  Modified template string
     */
    public static String replaceMacAddressInLibVirtTemplate(String template, String newMacAddress)
    {
        Guard.check(template, newMacAddress);        
        log_.debug("Replacing MAC address in the libvirt template with: " + newMacAddress);
       
        Document doc = stringToDom(template);
        
        NodeList nodes = doc.getElementsByTagName("mac");
        if (nodes.getLength() > 0 && nodes.item(0).getNodeType() == Node.ELEMENT_NODE)
        {
            Element element = (Element) nodes.item(0);
            element.setAttribute("address", newMacAddress);
            
        }
        
        String newTemplate = domToString(doc);
        
        return newTemplate;
    }
    

    /**
     * Gets the MAC of the libvirt template.
     * 
     * Note: return the first one found in the template
     * 
     * @param template      The template
     * @return              The mac address
     */
    public static String getMacAddressFromLibVirtTemplate(String template) 
    {
        Guard.check(template);
        String finalMacAddress = null;
        
        Document doc = stringToDom(template);
        
        NodeList nodes = doc.getElementsByTagName("mac");
        if (nodes.getLength() > 0 && nodes.item(0).getNodeType() == Node.ELEMENT_NODE)
        {
            Element element = (Element) nodes.item(0);
            finalMacAddress = element.getAttribute("address");
            
        }
         
        return finalMacAddress;
    }
    
    
    
    /**
     * Returns a list of networks attached to a domain.
     * 
     *
     * @param xmlDescription    The xml description
     * @return                  The list of network interfaces
     * @throws VirtualMachineMonitoringException 
     */
    public static List<String> getNetworkInterfacesFromXml(String xmlDescription) 
            throws VirtualMachineMonitoringException 
    {
        Guard.check(xmlDescription);   
        List<String> networkInterfaces = new ArrayList<String>();
        try 
        {            
            Document doc = stringToDom(xmlDescription);
            NodeList nodes = doc.getElementsByTagName("interface");
            for (int i = 0; i < nodes.getLength(); i++) 
            {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element element = (Element) node;
                    NodeList snodes = element.getElementsByTagName("target");
                    
                    for (int j = 0; j < snodes.getLength(); j++)
                    {
                        Node snode = snodes.item(j);                   
                        if (snode.getNodeType() == Node.ELEMENT_NODE)
                        {
                            networkInterfaces.add(((Element) snode).getAttribute("dev"));                    
                        }
                    }
                }
            }
            
            return networkInterfaces;
        }
        catch (Exception exception)
        {
            throw new VirtualMachineMonitoringException(String.format("Unable to get network interface for XML : %s",
                    exception.getMessage()));
        }
    }
    
    /**
     * Convert a string document to a Document DOM.
     * 
     * @param xml       the string to convert
     * @return          the Document DOM  representative of the String
     */
    public static Document stringToDom(String xml)
    {
        Guard.check(xml);        
        Document doc = null;
        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();
            return doc;
        }
        catch (Exception exception)
        {
            log_.error("Unable to parse xml template");
        }
        return doc;
    }
    
    /**
     * Convert a DOM document to a String.
     * 
     * @param doc           Document DOM
     * @return xml          string representative of the Document DOM
     */
    public static String domToString(Document doc)
    {
        Guard.check(doc);        
        String xml = null;
        try
        {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            StringWriter writer = new StringWriter();
            Result result = new StreamResult(writer);
            Source source = new DOMSource(doc);
            transformer.transform(source, result);
            writer.close();
            xml = writer.toString();
        }
        catch (Exception exception)
        {
            log_.error("Unable to write for the xml template");
        }
        
        return xml;
    }
    
}
