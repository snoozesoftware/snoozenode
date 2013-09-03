package org.inria.myriads.snoozenode.monitoring;

/**
 * 
 * Transport protocol.
 * 
 * @author msimonin
 *
 */
public enum TransportProtocol 
{
    /** Rabbitmq.*/
    RABBITMQ,
    /** TCP (not used).*/
    TCP,
    /** UDP (not used).*/
    UDP, 
    /** test transport.*/
    TEST,
}
