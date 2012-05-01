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
package org.inria.myriads.snoozenode.util;

import java.util.ArrayList;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.NetworkDemand;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Math utility.
 * 
 * @author Eugen Feller
 */
public final class MathUtils 
{    
    /** Size of the resource vectors. */
    public static final int RESOURCE_VECTOR_SIZE = 4;
     
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(MathUtils.class);
    
    /**
     * Hide the consturctor.
     */
    private MathUtils() 
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Compares two vectors.
     * 
     * @param firstVector     The first vector
     * @param secondVector    The second vector
     * @return                true if first is less, false otherwise
     */
    public static boolean vectorCompareIsLess(List<Double> firstVector,
                                              List<Double> secondVector) 
    {
        Guard.check(firstVector, secondVector);
        log_.debug(String.format("Comparing %s with %s", firstVector, secondVector));
        
        if (!isCorrectDimension(firstVector, secondVector))
        {
            return false;
        }
        
        for (int i = 0; i < RESOURCE_VECTOR_SIZE; i++)
        {
            if (firstVector.get(i) > secondVector.get(i))
            {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Rounds a double value to the first two digits.
     * 
     * @param value     The original value
     * @return          The rounded value
     */
    public static double roundDoubleValue(double value)
    {
        double roundedValue = Math.round(value * 100) / 100.0; 
        return roundedValue; 
    }
    
    /**
     * Compares two vectors.
     * 
     * @param firstVector     The first vector
     * @param secondVector    The second vector
     * @return                true if first is greater, false otherwise
     */
    public static boolean vectorCompareIsGreater(List<Double> firstVector,
                                                 List<Double> secondVector) 
    {
        Guard.check(firstVector, secondVector);
        log_.debug(String.format("Comparing %s with %s", firstVector, secondVector));
        
        if (!isCorrectDimension(firstVector, secondVector))
        {
            return false;
        }
        
        for (int i = 0; i < RESOURCE_VECTOR_SIZE; i++)
        {
            if (firstVector.get(i) < secondVector.get(i))
            {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Verifies the dimensions.
     * 
     * @param firstVector       The first vector
     * @param secondVector      The second vector
     * @return                  true if dimensions are ok, false otherwise
     */
    public static boolean isCorrectDimension(List<Double> firstVector, List<Double> secondVector)
    {
        Guard.check(firstVector, secondVector);
        if (firstVector.size() == RESOURCE_VECTOR_SIZE && 
                secondVector.size() == RESOURCE_VECTOR_SIZE)
        {
            return true;
        }
        
        log_.error("The vector dimensions are bad!!");
        return false;       
    }
    
    /**
     * Adds two vectors.
     * 
     * @param firstVector       The first vector
     * @param secondVector      The second vector
     * @return                  The new vector
     */
    public static ArrayList<Double> addVectors(List<Double> firstVector, List<Double> secondVector)   
    {
        Guard.check(firstVector, secondVector);
        log_.debug(String.format("Adding vector %s with %s", firstVector, secondVector));
        
        ArrayList<Double> newVector = new ArrayList<Double>();
        if (!isCorrectDimension(firstVector, secondVector))
        {
            return newVector;
        }
        
        for (int i = 0; i < RESOURCE_VECTOR_SIZE; i++)
        {
            double value = firstVector.get(i) + secondVector.get(i);
            newVector.add(value);
        }
        
        return newVector;
    }
    
    /**
     * Divides all values of a vector by a number.
     * 
     * @param vector       The vector
     * @param divisor      The divisor
     * @return             The new vector
     */
    public static ArrayList<Double> divideVector(List<Double> vector, int divisor)   
    {
        Guard.check(vector, divisor);
        ArrayList<Double> newVector = new ArrayList<Double>();
        
        if (vector.size() != RESOURCE_VECTOR_SIZE)
        {
            return newVector;
        }
        
        for (int i = 0; i < RESOURCE_VECTOR_SIZE;  i++)
        {
            double value = vector.get(i) / divisor;
            newVector.add(value);
        }
        
        return newVector;
    }
    
    /**
     * Divides all values of a vector by a number.
     * 
     * @param firstVector       The first vector
     * @param secondVector      The second vector
     * @return                  The new vector
     */
    public static ArrayList<Double> substractVector(List<Double> firstVector, 
                                                    List<Double> secondVector)   
    {
        Guard.check(firstVector, secondVector);
        ArrayList<Double> newVector = new ArrayList<Double>();   
        
        if (!isCorrectDimension(firstVector, secondVector))
        {
            return newVector;
        }
          
        for (int i = 0; i < RESOURCE_VECTOR_SIZE; i++)
        {
            double value = Math.abs(firstVector.get(i) - secondVector.get(i));
            newVector.add(value);
        }
        
        return newVector;
    }
    
    /**
     * Computes the L1 norm.
     * 
     * @param values    The data
     * @return          The L1 norm value
     */
    public static double computeL1Norm(List<Double> values)
    {
       Guard.check(values);
       
       double result = 0;
       for (Double value : values)
       {
           result += Math.abs(value);
       }
       
       return result;
    }
    
    /**
     * Computes the Max norm.
     * 
     * @param values  The data
     * @return        The max norm value
     */
    public static double computeMaxNorm(List<Double> values)
    {
       Guard.check(values);
       
       double result = 0;
       for (int i = 0; i < values.size(); i++)
       {  
           if (values.get(i) > result)
           {  
               result = values.get(i);
           }  
       }  
       
       return result;  
    }
    
    /**
     * Creates virtual machine utilization vector.
     * 
     * @param cpu           The cpu utilization
     * @param memory        The memory usage
     * @param networkDemand The network demand
     * @return              The vector of values
     */
    public static ArrayList<Double> createCustomVector(double cpu, double memory, NetworkDemand networkDemand)
    {
        Guard.check(cpu, memory, networkDemand);
        ArrayList<Double> utilizationVector = new ArrayList<Double>();
        utilizationVector.add(cpu);
        utilizationVector.add(memory);
        utilizationVector.add(networkDemand.getRxBytes());
        utilizationVector.add(networkDemand.getTxBytes());
        return utilizationVector; 
    }
    
    /**
     * Creates and empty vector.
     * s
     * @return                  The vector of values
     */
    public static ArrayList<Double> createEmptyVector()
    {
        NetworkDemand demand = new NetworkDemand();
        demand.setRxBytes(0);
        demand.setTxBytes(0);
        ArrayList<Double> emptyVector = createCustomVector(0, 0, demand);
        return emptyVector; 
    }
    
    /**
     * Computes the Euclid norm.
     * 
     * @param values  The data
     * @return        The euclid norm value
     */
    public static double computeEuclidNorm(List<Double> values)
    {
       Guard.check(values);
       
       double result = 0;
       for (Double value : values)
       {
           result += Math.pow(value, 2);
       }
       
       return Math.sqrt(result);
    }
}
