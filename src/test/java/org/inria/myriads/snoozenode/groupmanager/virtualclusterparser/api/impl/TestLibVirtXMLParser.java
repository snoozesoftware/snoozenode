package org.inria.myriads.snoozenode.groupmanager.virtualclusterparser.api.impl;


import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.TestCase;

import org.inria.myriads.snoozecommon.communication.virtualmachine.ResizeRequest;
import org.inria.myriads.snoozenode.groupmanager.virtualclusterparser.api.VirtualClusterParser;
import org.inria.myriads.snoozenode.groupmanager.virtualclusterparser.util.VirtualClusterParserUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * Tests the libvirt parser.
 * 
 * @author msimonin
 *
 */
public class TestLibVirtXMLParser extends TestCase 
{

    /**
     * 
     * The the handle resize request method.
     * 
     */
    public void testHandleResizeRequest() 
    {
        String xmlDesc = 
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                   "<domain type=\"kvm\"> " +
                       "<name>debian2</name>"  +
                       "<uuid>0f476e56-67ea-11e1-858e-00216a972a37</uuid>" +
                       "<memory>597152</memory>" +
                       "<vcpu>2</vcpu>" +
                 "</domain>";
        ResizeRequest resizeRequest = new ResizeRequest();
        ArrayList<Double> requestedCapacity = new ArrayList(Arrays.asList(1,256000,6400,6400));
        resizeRequest.setResizedCapacity(requestedCapacity);
        VirtualClusterParser libvirtParser = new LibVirtXMLParser();
        
        String newXmlDesc = libvirtParser.handleResizeRequest(xmlDesc, resizeRequest);
        
        String objXmlDesc = xmlDesc.replace("<vcpu>2</vcpu>", "<vcpu>1</vcpu>");
        objXmlDesc = objXmlDesc.replace("<memory>597152</memory>", "<memory>256000</memory>");
        
        assertEquals(newXmlDesc, objXmlDesc);
    }

}
