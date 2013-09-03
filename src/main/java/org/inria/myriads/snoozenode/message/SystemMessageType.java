package org.inria.myriads.snoozenode.message;

/**
 * 
 * System message type.
 * 
 * @author msimonin
 *
 */
public enum SystemMessageType
{
    /**GL join.*/
    GL_JOIN,
    /**GL FAILED.*/
    GL_FAILED,
    /** GM join. */
    GM_JOIN, 
    /** GM failed. */
    GM_FAILED,
    /** LC join. */
    LC_JOIN,
    /** LC failed. */
    LC_FAILED,
    /** GL Summary.*/
    GL_SUMMARY, 
    /** LC anomaly (overload, underload).*/
    LC_ANOMALY,
    /** Reconfiguration. */
    RECONFIGURATION, 
    /** GM state change to busy.*/
    GM_BUSY, 
    /** GM state change to idle.*/
    GM_IDLE,
}

