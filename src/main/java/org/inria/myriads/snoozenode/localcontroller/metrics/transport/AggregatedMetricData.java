package org.inria.myriads.snoozenode.localcontroller.metrics.transport;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.inria.myriads.snoozecommon.datastructure.LRUCache;
import org.inria.myriads.snoozecommon.metric.Metric;
import org.inria.myriads.snoozenode.localcontroller.metrics.consumer.LocalControllerMetricConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregatedMetricData implements Cloneable, Serializable
{
    
    
    /** Default serial id.*/
    private static final long serialVersionUID = 1L;


    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LocalControllerMetricConsumer.class);
  
    
    Map<String, LRUCache<Long, Metric>> metricData_ ;

    
    public AggregatedMetricData()
    {
        metricData_ = new HashMap<String, LRUCache<Long, Metric>>();
    }
    /**
     * @param metricData
     */
    public AggregatedMetricData(Map<String, LRUCache<Long, Metric>> metricData)
    {
        metricData_ = metricData;
    }

    
    public void add(Metric metric)
    {
        String metricName = metric.getName();
        if (! metricData_.containsKey(metricName))
        {
            log_.debug(String.format("Metric %s found",metricName));
            metricData_.put(metricName, new LRUCache<Long,Metric>());

        }
        metricData_.get(metricName).put(metric.getTimestamp(), metric);

    }
    
    /**
     * @return the metricData
     */
    public Map<String, LRUCache<Long, Metric>> getMetricData()
    {
        return metricData_;
    }

    /**
     * @param metricData the metricData to set
     */
    public void setMetricData(Map<String, LRUCache<Long, Metric>> metricData)
    {
        metricData_ = metricData;
    }
    
    public void clear()
    {
        metricData_.clear();
        
    }
    
    @SuppressWarnings("unchecked")
    public Object clone()
    {
        Map<String, LRUCache<Long, Metric>> clone = new HashMap<String, LRUCache<Long, Metric>>(); 
        for ( Entry<String, LRUCache<Long, Metric>> entry  : this.getMetricData().entrySet())
        {
            clone.put(entry.getKey(), (LRUCache<Long, Metric>) entry.getValue().clone());
        }
        return new AggregatedMetricData(clone);
    }
    
    public void merge(AggregatedMetricData currentData)
    {
        String currentKey = "";
        for ( Entry<String, LRUCache<Long, Metric>> entry  : currentData.getMetricData().entrySet())
        {
            currentKey = entry.getKey();
            if (metricData_.containsKey(currentKey))
            {
                metricData_.get(currentKey).putAll(entry.getValue());
            }
            else
            {
                metricData_.put(currentKey, entry.getValue());
            }
        }
        
    }
    
}
