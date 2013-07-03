package org.inria.myriads.snoozenode.localcontroller.monitoring.host.api.impl;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.IOUtils;
import org.inria.myriads.snoozecommon.metric.Metric;
import org.inria.myriads.snoozenode.localcontroller.monitoring.host.api.MetricProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
 * @author vv
 *
 */
public class GmondProducer implements MetricProducer
{

    /** Define the logger. */
    private static final Logger log_= LoggerFactory.getLogger(GmondProducer.class);
    
    /** Metrics to fetch. */
    private List<String> metrics_ ;

    /** Probing interval. */
    private int interval_;
    
    /** Hostname.*/
    private String hostname_;
    
    /** Port. */
    private int port_;
    
    /** Socket to gmond. */
    private Socket clientSocket_;

    private InetSocketAddress socketAddress_; 
    
    
    public GmondProducer(String hostname,
                         int port,
                         String[] metrics,
                         int interval
                         )
    {
        log_.debug("Initialized Gmond Metrics producer with parameters : ");
        log_.debug(String.format("hostname = %s , port = %s , interval = %s , metrics[0] = %s" ,
                hostname, port, interval, metrics[0]));
        hostname_ = hostname;
        port_ = port;
        interval_ = interval;
        metrics_ = new ArrayList<String>();
        
        for (String metric : metrics)
        {
            metrics_.add(metric);
        }
        log_.debug("Gmond metric producer initialized");
    }
    
    @Override
    public List<Metric> getMetric() throws Exception
    {
        List<Metric> metrics = new ArrayList<Metric>();
        try{
            socketAddress_ = new InetSocketAddress("localhost", 8649);
            clientSocket_ = new Socket();
            clientSocket_.connect(socketAddress_);
            
            InputStream input = clientSocket_.getInputStream();
            String myString = IOUtils.toString(input, "UTF-8");

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new ByteArrayInputStream(myString.getBytes("utf-8"))));
            NodeList nList = doc.getElementsByTagName("HOST");
            
            for (int temp = 0; temp < nList.getLength(); temp++) 
            {
                Node nNode = nList.item(temp);     
           
                if (nNode.getNodeType() == Node.ELEMENT_NODE) 
                {
                    Element eElement = (Element) nNode;
                    NodeList xmlMetrics = eElement.getElementsByTagName("METRIC");
                    for (int tempMetric= 0; tempMetric < xmlMetrics.getLength(); tempMetric++) 
                    {
                        Node nMetric = xmlMetrics.item(tempMetric);     
                        
                        if (nMetric.getNodeType() == Node.ELEMENT_NODE) 
                        {
                            Element eMetric = (Element) nMetric;
                            String metricName = eMetric.getAttribute("NAME") ;
                            if (metrics_.contains(metricName))
                            {
                                //add metric here
                                double metricValue = Double.valueOf(eMetric.getAttribute("VAL")); 
                                metrics.add(new Metric(metricName, metricValue));
                            }
                        }
                    }
                }
            }
        }
        catch(Exception exception)
        {
            log_.warn("Unable to parse the gmond xml");
        }
                
        clientSocket_.close();

        return metrics;
    }

    @Override
    public String getType()
    {
        return this.getClass().getSimpleName();
    }

}
