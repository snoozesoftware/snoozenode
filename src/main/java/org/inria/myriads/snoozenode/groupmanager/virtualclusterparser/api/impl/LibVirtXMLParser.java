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
package org.inria.myriads.snoozenode.groupmanager.virtualclusterparser.api.impl;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.NetworkDemand;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualClusterSubmissionRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineTemplate;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.exception.VirtualClusterParserException;
import org.inria.myriads.snoozenode.exception.VirtualMachineTemplateException;
import org.inria.myriads.snoozenode.groupmanager.virtualclusterparser.api.VirtualClusterParser;
import org.inria.myriads.snoozenode.util.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * LibVirt XML Parser.
 * 
 * @author Eugen Feller
 */
public final class LibVirtXMLParser
    implements VirtualClusterParser
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LibVirtXMLParser.class);
            
    /**
     * Constructor.
     */
    public LibVirtXMLParser() 
    {
        log_.debug("Starting libvirt XML parser");       
    }
    
    /**
     * Creates virtual machine meta data.
     * 
     * @param cluster                        The virual cluster description
     * @return                               The virtual machine meta data map
     * @throws VirtualClusterParserException 
     */
    public ArrayList<VirtualMachineMetaData> createVirtualMachineMetaData(VirtualClusterSubmissionRequest cluster) 
        throws VirtualClusterParserException 
    {
        Guard.check(cluster);
        log_.debug("Creating virtual machine meta data");
        
        ArrayList<VirtualMachineMetaData> metaData = new ArrayList<VirtualMachineMetaData>();
        List<VirtualMachineTemplate> virtualMachineDescriptions = cluster.getVirtualMachineTemplates();
        for (VirtualMachineTemplate description : virtualMachineDescriptions)
        {
            VirtualMachineMetaData virtualMachine;
            try 
            {
                virtualMachine = parseDescription(description);
            } 
            catch (Exception exception) 
            {
                throw new VirtualClusterParserException(String.format("Failed parsing libvirt template: %s", 
                                                                      exception.getMessage()));
            }        
            metaData.add(virtualMachine);
        }
        
        return metaData; 
    }
    
    /**
     * Start processing the file.
     * 
     * @param virtualMachineDescription     The virtual machine description
     * @return                              The virtual machine meta data
     * @throws Exception 
     * @throws Exception
     */
    private VirtualMachineMetaData parseDescription(VirtualMachineTemplate virtualMachineDescription) 
        throws Exception 
    {
        Guard.check(virtualMachineDescription);
        log_.debug(String.format("Starting to parse virtual machine description: %s", virtualMachineDescription));
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        Document document = 
            documentBuilder.parse(new InputSource(new StringReader(virtualMachineDescription.getLibVirtTemplate())));

        VirtualMachineMetaData virtualMachineMetaData = 
            parseDocument(document, virtualMachineDescription.getNetworkCapacityDemand());    
        virtualMachineMetaData.setXmlRepresentation(virtualMachineDescription.getLibVirtTemplate());
        return virtualMachineMetaData;
    }
 
    /** 
     * Parse the dom representation.
     * 
     * @param document                  The document file
     * @param networkCapacityDemand     The network capacity demand
     * @return                          The virtual machine meta data
     * @throws Exception 
     */
    private VirtualMachineMetaData parseDocument(Document document, NetworkDemand networkCapacityDemand) 
        throws Exception
    {
        Guard.check(document);
        log_.debug("Parsing the DOM file now");
                                        
        Element root = document.getDocumentElement();
        String virtualMachineId = getInformation(root, "name");
        
        VirtualMachineMetaData virtualMachineMetaData = new VirtualMachineMetaData();
        virtualMachineMetaData.getVirtualMachineLocation().setVirtualMachineId(virtualMachineId);
        
        ArrayList<Double> requestedCapacity = generateRequestedCapacity(root, networkCapacityDemand);
        virtualMachineMetaData.setRequestedCapacity(requestedCapacity);
        return virtualMachineMetaData;
    }
        
    /**
     * Generates the requested capacity.
     * 
     * @param root                  The root element
     * @param networkCapacity       The network capacity
     * @return                      The requested capacity
     * @throws Exception 
     */
    private ArrayList<Double> generateRequestedCapacity(Element root,  NetworkDemand networkCapacity) 
        throws Exception 
    {          
        Guard.check(root);
        int memorySize = Integer.valueOf(getInformation(root, "memory"));
        int numberOfVCPUs = Integer.valueOf(getInformation(root, "vcpu"));
        if (memorySize == 0 || numberOfVCPUs == 0)
        {
            throw new VirtualMachineTemplateException("Memory information is not available");
        }
                
        ArrayList<Double> resourceRequirements = MathUtils.createCustomVector(numberOfVCPUs, 
                                                                              memorySize, 
                                                                              networkCapacity);       
        return resourceRequirements;
    }
    
    /**
     * Returns information from tag.
     * 
     * @param root          The root element
     * @param tag           The tag
     * @return              The information
     */
    private String getInformation(Element root, String tag) 
    {
        Guard.check(root, tag);
        NodeList nodes = root.getElementsByTagName(tag);
        
        Element element = (Element) nodes.item(0);
        String information = getDataFromElement(element);
        
        return information;
    }
     
    /**
     * Retrieves data from element.
     * 
     * @param element       The element
     * @return              The data
     */
    private String getDataFromElement(Element element) 
    {
        Guard.check(element);
        Node child = element.getFirstChild();

        if (child instanceof CharacterData) 
        {
            CharacterData characterData = (CharacterData) child;
            return characterData.getData();
        }
        
        return null;
    }
}
