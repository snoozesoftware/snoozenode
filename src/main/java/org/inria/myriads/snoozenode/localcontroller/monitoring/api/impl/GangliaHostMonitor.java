package org.inria.myriads.snoozenode.localcontroller.monitoring.api.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
public class GangliaHostMonitor extends HostMonitor
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GangliaHostMonitor.class);
    
    /** List of monitored resources.*/
    List<String> resourceNames_;
        
    private NetworkAddress address_;
    
    /** socket Address. */
    private InetSocketAddress socketAddress_;

    private String hostname_;

    public GangliaHostMonitor()
    {
        log_.debug("Building a new Ganglia host monitor");
        resourceNames_ = new ArrayList<String>();
    }
    
    @Override
    public void initialize() throws HostMonitoringException
    {
        for (Resource resource :settings_.getResources())
        {
            log_.debug("Adding " + resource.getName() + "to the resources list");
            resourceNames_.add(resource.getName());
        }
        
        String address = settings_.getOptions().get("hostname");
        if (address == null)
        {
            throw new HostMonitoringException("address options is missing");
        }
        
        String port = settings_.getOptions().get("port");    
        if (port == null)
        {
            throw new HostMonitoringException("address options is missing");
        }
        address_ = new NetworkAddress();
        address_.setAddress(address);
        address_.setPort(Integer.valueOf(port));
        hostname_= address_.getAddress();
        
        
    }
    
    public HostMonitoringData getResourceData() 
    {
        HostMonitoringData resource = new HostMonitoringData();

        try{
            log_.debug("Connecting to ganglia daemon");
            socketAddress_ = new InetSocketAddress(address_.getAddress(), address_.getPort());
            Socket clientSocket = new Socket();
            clientSocket.connect(socketAddress_);
            
            InputStream input = clientSocket.getInputStream();
            String gangliaString = IOUtils.toString(input, "UTF-8");
            
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            XPath xPath = XPathFactory.newInstance().newXPath();
            Document xmlDocument = builder.parse(new ByteArrayInputStream(gangliaString.getBytes()));
            //gets all the metrics.
            HashMap<String, Double> monitoring = new HashMap<String, Double>();
            for (String resourceName : resourceNames_)
            {
                log_.debug("Getting resource for " + resourceName);
                
                String expression = "//HOST[@NAME='" + hostname_ + "']/METRIC[@NAME='" +  resourceName + "']";
                log_.debug("XPATH : " + expression);
                Element element = (Element) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODE);
                if (element != null)
                {
                    Double value = Double.valueOf(element.getAttribute("VAL"));
                    log_.debug("Adding " + value + " to the local resource monitoring set");
                    monitoring.put(resourceName, value);
                }
                else
                {
                    log_.debug("Unable to fetch resource " + resourceName);
                }
            }
            resource.setUsedCapacity(monitoring);
            log_.debug("Closing socket");
            clientSocket.close();
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
