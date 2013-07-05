package org.inria.myriads.snoozenode.localcontroller.monitoring.host.api;

import org.inria.myriads.snoozecommon.metric.Metric;
import org.inria.myriads.snoozenode.localcontroller.metrics.transport.AggregatedMetricData;

import junit.framework.TestCase;

public class TestAggregatedMetricData extends TestCase
{
    
    public void testCreate()
    {
        AggregatedMetricData aggregatedMetric = new AggregatedMetricData();
        assertNotNull(aggregatedMetric.getMetricData());
        assertEquals(aggregatedMetric.getMetricData().size(),0);
    }
    
    public void testAdd()
    {
        AggregatedMetricData aggregatedMetric = new AggregatedMetricData(); 
        Metric m = new Metric("test", 1d);
        aggregatedMetric.add(m);
        assertEquals(aggregatedMetric.getMetricData().size(),1);
        assertNotNull(aggregatedMetric.getMetricData().get("test"));
        assertEquals(aggregatedMetric.getMetricData().get("test").size(),1);
        assertTrue(aggregatedMetric.getMetricData().get("test").containsValue(m));
    }
    
    public void testAdd2()
    {
        AggregatedMetricData aggregatedMetric = new AggregatedMetricData(); 
        Metric m1 = new Metric("test", 1d);
        Metric m2 = new Metric("test", 2d);
        m2.setTimestamp(m1.getTimestamp()+1);
        aggregatedMetric.add(m1);
        aggregatedMetric.add(m2);
        assertEquals(aggregatedMetric.getMetricData().size(),1);
        assertNotNull(aggregatedMetric.getMetricData().get("test"));
        assertEquals(aggregatedMetric.getMetricData().get("test").size(),2);
        assertTrue(aggregatedMetric.getMetricData().get("test").containsValue(m1));
        assertTrue(aggregatedMetric.getMetricData().get("test").containsValue(m2));
    }
    
    public void testClone()
    {
        AggregatedMetricData aggregatedMetric = new AggregatedMetricData(); 
        Metric m = new Metric("test", 1d);
        aggregatedMetric.add(m);
        AggregatedMetricData aggregatedMetricClone = (AggregatedMetricData) aggregatedMetric.clone();
        assertEquals(aggregatedMetricClone.getMetricData().size(),1);
        assertNotNull(aggregatedMetricClone.getMetricData().get("test"));
        assertEquals(aggregatedMetricClone.getMetricData().get("test").size(),1);
        assertTrue(aggregatedMetricClone.getMetricData().get("test").containsValue(m));
    }
    
    public void testMergeOneEmpty()
    {
        AggregatedMetricData aggregatedMetric1 = new AggregatedMetricData();
        AggregatedMetricData aggregatedMetric2 = new AggregatedMetricData();
        Metric m = new Metric("test", 1d);
        aggregatedMetric2.add(m);
        aggregatedMetric1.merge(aggregatedMetric2);
        assertEquals(aggregatedMetric1.getMetricData().size(),1);
        assertNotNull(aggregatedMetric1.getMetricData().get("test"));
        assertEquals(aggregatedMetric1.getMetricData().get("test").size(),1);
        assertTrue(aggregatedMetric1.getMetricData().get("test").containsValue(m));
    }
    
    public void testMergeSameKey()
    {
        AggregatedMetricData aggregatedMetric1 = new AggregatedMetricData();
        AggregatedMetricData aggregatedMetric2 = new AggregatedMetricData();
        Metric m1 = new Metric("test", 1d);
        aggregatedMetric1.add(m1);
        Metric m2 = new Metric("test", 1d);
        m2.setTimestamp(1234L);
        aggregatedMetric2.add(m2);
        aggregatedMetric1.merge(aggregatedMetric2);
        assertEquals(aggregatedMetric1.getMetricData().size(),1);
        assertNotNull(aggregatedMetric1.getMetricData().get("test"));
        assertEquals(aggregatedMetric1.getMetricData().get("test").size(),2);
        assertTrue(aggregatedMetric1.getMetricData().get("test").containsValue(m1));
        assertTrue(aggregatedMetric1.getMetricData().get("test").containsValue(m1));
    }
    
    public void testMergeDifferentKey()
    {
        AggregatedMetricData aggregatedMetric1 = new AggregatedMetricData();
        AggregatedMetricData aggregatedMetric2 = new AggregatedMetricData();
        Metric m1 = new Metric("test1", 1d);
        aggregatedMetric1.add(m1);
        Metric m2 = new Metric("test2", 1d);
        m2.setTimestamp(1234L);
        aggregatedMetric2.add(m2);
        aggregatedMetric1.merge(aggregatedMetric2);
        assertEquals(aggregatedMetric1.getMetricData().size(),2);
        assertNotNull(aggregatedMetric1.getMetricData().get("test1"));
        assertNotNull(aggregatedMetric1.getMetricData().get("test2"));
        assertEquals(aggregatedMetric1.getMetricData().get("test1").size(),1);
        assertTrue(aggregatedMetric1.getMetricData().get("test1").containsValue(m1));
        assertTrue(aggregatedMetric1.getMetricData().get("test2").containsValue(m2));
    }
}
