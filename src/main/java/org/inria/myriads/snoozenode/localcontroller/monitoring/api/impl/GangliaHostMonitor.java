package org.inria.myriads.snoozenode.localcontroller.monitoring.api.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.HostMonitoringData;
import org.inria.myriads.snoozenode.exception.HostMonitoringException;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.HostMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author msimonin
 *
 */
public class GangliaHostMonitor implements HostMonitor
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GangliaHostMonitor.class);
    
    /** List of monitored resources.*/
    List<String> resourceNames_;
        
    /** Hostname.*/
    private String hostname_;
    
    private NetworkAddress address_;
    
    /** Socket to gmond. */
    private Socket clientSocket_;
    
    /** socket Address. */
    private InetSocketAddress socketAddress_; 

    /**
     * @param resourceName
     * @param interval
     * @param hostname
     * @param address
     * @param clientSocket
     */
    public GangliaHostMonitor(List<String> resourceNames,  String hostname, NetworkAddress address)
    {
        log_.debug("Initialized Gmond host monitor with parameters : ");
        log_.debug(String.format("hostname = %s , port = %s , metrics = %s",
                hostname, address.getPort(),  resourceNames));
        
        resourceNames_ = resourceNames;
        hostname_ = hostname;
        address_ = address;
        
        log_.debug("Gmond host monitor initialized");
    }
    
    public HostMonitoringData getResourceData() 
    {
        HostMonitoringData resource = new HostMonitoringData();

        try{
            log_.debug("Connecting to ganglia daemon");
            socketAddress_ = new InetSocketAddress(address_.getAddress(), address_.getPort());
            clientSocket_ = new Socket();
            clientSocket_.connect(socketAddress_);
            
            InputStream input = clientSocket_.getInputStream();
            String gangliaString = IOUtils.toString(input, "UTF-8");
            
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            XPath xPath = XPathFactory.newInstance().newXPath();
            Document xmlDocument = builder.parse(new ByteArrayInputStream(gangliaString.getBytes()));
            //gets all the metrics.
            HashMap<String, Double> monitoring = new HashMap<String, Double>();
            for (String resourceName : resourceNames_)
            {
                String expression = "//HOST[@NAME='" + hostname_ + "']/METRIC[@NAME='" +  resourceName + "']";
                Element element = (Element) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODE);
                if (element != null)
                {
                    Double value = Double.valueOf(element.getAttribute("VAL"));
                    log_.debug("Adding " + value + " to the local resource monitoring set");
                    monitoring.put(resourceName, value);
                }
            }
            resource.setUsedCapacity(monitoring);
            log_.debug("Closing socket");
            clientSocket_.close();
            log_.debug("Returning resource");
        }
        catch(Exception exception)
        {
            log_.warn("Unable to parse the gmond xml" + exception.getMessage());
            return resource;
        }
        return resource;
    }

    @Override
    public ArrayList<Double> getTotalCapacity() throws HostMonitoringException
    {
        // TODO Auto-generated method stub
        return null;
    }


}
