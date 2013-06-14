package org.inria.myriads.snoozenode.eventmessage;

public enum EventType
{
    /**GL join*/
    GL_JOIN,
    /**GL FAILED*/
    GL_FAILED,
    /** GM join. */
    GM_JOIN, 
    /** GM failed. */
    GM_FAILED,
    /** LC join. */
    LC_JOIN,
    /** LC failed. */
    LC_FAILED,
    /** VM start.*/
    VM_START,
    /** VM started.*/
    VM_STARTED,
    /** VM suspend.*/
    VM_SUSPEND_PENDING,
    /** VM suspend.*/
    VM_SUSPENDED,
    /** vm resume pending*/ 
    VM_RESUME_PENDING,
    /** VM suspend.*/
    VM_RESUMED,
    /** VM reboot.*/
    VM_REBOOT_PENDING,
    /** VM rebooted.*/
    VM_REBOOTED,
    /** VM shutdown pending.*/
    VM_SHUTDOWN_PENDING,
    /** VM shutdown.*/
    VM_SHUTDOWN,
    /** VM destroy pending.*/
    VM_DESTROY_PENDING,
    /** VM destroyed.*/
    VM_DESTROYED,
    /** VM Migrate.*/
    VM_MIGRATE,
    /** VM Migrated.*/
    VM_MIGRATED, 
    /** GL Summary*/
    GL_SUMMARY,
}
