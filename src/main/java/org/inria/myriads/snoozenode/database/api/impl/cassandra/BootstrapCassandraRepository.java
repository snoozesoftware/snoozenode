package org.inria.myriads.snoozenode.database.api.impl.cassandra;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerList;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.database.api.BootstrapRepository;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.CassandraUtils;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.JsonSerializer;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.RowQueryIterator;
import org.inria.myriads.snoozenode.database.api.impl.memory.GroupManagerMemoryRepository;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Bootstrap Cassandra repository.
 * 
 * @author msimonin
 *
 */
public class BootstrapCassandraRepository extends CassandraRepository implements BootstrapRepository
{
    
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(BootstrapCassandraRepository.class);
    
    /**
     * 
     * Constructor.
     * 
     * @param hosts         List of cassandra hosts to connect to.
     */
    public BootstrapCassandraRepository(String hosts)
    {
        super(hosts);
        log_.debug("Bootstrap cassandra repository initialized");
    }


    public VirtualMachineMetaData getVirtualMachineMetaData(String virtualMachineId, int numberOfMonitoringEntries)
    {
        VirtualMachineMetaData virtualMachine = getVirtualMachineMetaDataCassandra(virtualMachineId, numberOfMonitoringEntries);
        return virtualMachine;
    }

    public  List<GroupManagerDescription> getGroupManagerDescriptions(String firstGroupManagerId, int limit)
    {
        return getGroupManagerDescriptionsOnly(firstGroupManagerId, limit, false, 0);
    }


    @Override
    public LocalControllerList getLocalControllerList()
    {   
        HashMap<String, LocalControllerDescription> localControllers = getLocalControllerDescriptionsOnly(null,null,-1, 0, true, false);
        LocalControllerList localControllerList = new LocalControllerList(localControllers);
        return localControllerList;
    }
   

}
