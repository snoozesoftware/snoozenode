package org.inria.myriads.snoozenode.localcontroller.monitoring.host.api.impl;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.inria.myriads.snoozecommon.metric.Metric;
import org.inria.myriads.snoozenode.localcontroller.monitoring.host.api.MetricProducer;


/**
 * @author vv
 *
 */
public class CpuTempProducer implements MetricProducer
{

    private String name_ = "cpuTemp";
    
    @Override
    public Metric getMetric()
    {
        Runtime r = Runtime.getRuntime();
        String f, temp;
        f = "sensors -u";
        double temperature = 0.0;
        
        int nbTemp = 0;
        double total = 0;
        
        Process p;
        try
        {
        p = r.exec(f);
        
        BufferedReader pin = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
        while((temp = pin.readLine()) != null)
        {
            Pattern pat = Pattern.compile("temp[0-9]+_input:.*");
        
            Matcher m = pat.matcher(temp);
            if (m.find())
            {
                Pattern pat2 = Pattern.compile("[0-9]+(.[0-9]+)+");
                    Matcher m2 = pat2.matcher(m.group(0));
        
                    if (m2.find())
                    {
                        total += Double.parseDouble(m2.group(0));
                        nbTemp++;
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        temperature = total / nbTemp;

        return new Metric(name_, temperature);
    }

    @Override
    public String getName()
    {
        return name_;
    }

}
