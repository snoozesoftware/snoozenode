package org.inria.myriads.snoozenode.groupmanager.virtualclustermanager;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineErrorCode;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.globals.Globals;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupLeaderSchedulerSettings;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;

/**
 * 
 * Test virtual cluster manager.
 * 
 * @author msimonin
 *
 */
public class TestVirtualClusterManager extends TestCase
{

    
     
    /** Virtual cluster manager (under test). */
    private VirtualClusterManager virtualClusterManager_; 
    
    /**
     * Setup method.
     */
    public void setUp() 
    {
        GroupLeaderRepository repository = EasyMock.createMock(GroupLeaderRepository.class);
        NodeConfiguration nodeConfiguration = EasyMock.createMock(NodeConfiguration.class);
        StaticDynamicResourceDemandEstimator estimator = EasyMock.createMock(StaticDynamicResourceDemandEstimator.class);
        GroupLeaderSchedulerSettings groupLeaderScheduler = new GroupLeaderSchedulerSettings();
        groupLeaderScheduler.setDispatchingPolicy("firstfit");
        
       
        //gl -> gm1, gm2, gm3
        //Map<String, GroupManagerDescription> groupManagers = new HashMap<String, GroupManagerDescription>();
        ArrayList<GroupManagerDescription> groupManagers = new ArrayList<GroupManagerDescription>();
        GroupManagerDescription gm1 = new GroupManagerDescription();
        GroupManagerDescription gm2 = new GroupManagerDescription();
        GroupManagerDescription gm3 = new GroupManagerDescription();
        gm1.setId("gm1");
        gm2.setId("gm2");
        gm3.setId("gm3");
        groupManagers.add(gm1);
        groupManagers.add(gm2);
        groupManagers.add(gm3);
        
        // gm1 -> lc1 , lc2
        HashMap<String, LocalControllerDescription> lcgm1 = new HashMap<String, LocalControllerDescription>();
        LocalControllerDescription lc1 = new LocalControllerDescription();
        lc1.setId("lc1");
        LocalControllerDescription lc2 = new LocalControllerDescription();
        lc2.setId("lc2");
        lcgm1.put("lc1", lc1);
        lcgm1.put("lc2", lc2);
        gm1.setLocalControllers(lcgm1);
        
        // gm2 -> lc3        
        HashMap<String, LocalControllerDescription> lcgm2 = new HashMap<String, LocalControllerDescription>();
        LocalControllerDescription lc3 = new LocalControllerDescription();
        lc3.setId("lc3");
        lcgm2.put("lc3", lc3);
        gm2.setLocalControllers(lcgm2);
        // gm3 -> no lcs
        // --
        
        expect(repository.getGroupManagerDescriptions(0)).andReturn(groupManagers);
        expect(nodeConfiguration.getGroupLeaderScheduler()).andReturn(groupLeaderScheduler).anyTimes();
        replay(repository);
        replay(nodeConfiguration);
        replay(estimator);
        
        virtualClusterManager_ = new VirtualClusterManager(nodeConfiguration, repository, estimator);

    }
    
    /**
     * Test with a right gm set.
     * should position the gm only
     */
    public void testSetVirtualMachineLocationGMset()
    {
        VirtualMachineMetaData vm = new VirtualMachineMetaData();
        virtualClusterManager_.setVirtualMachineLocation(vm, "gm1");
        
        assertEquals("gm1", vm.getVirtualMachineLocation().getGroupManagerId());
        assertEquals(Globals.DEFAULT_INITIALIZATION, vm.getVirtualMachineLocation().getLocalControllerId());
    }

    
    
    /**
     * Test with a wrong gm set.
     * should position the gm to default and set error code.
     */
    public void testSetVirtualMachineLocationWrongGMset()
    {
        VirtualMachineMetaData vm = new VirtualMachineMetaData();
        virtualClusterManager_.setVirtualMachineLocation(vm, "gm42");
        
        assertEquals(Globals.DEFAULT_INITIALIZATION, vm.getVirtualMachineLocation().getGroupManagerId());
        assertEquals(Globals.DEFAULT_INITIALIZATION, vm.getVirtualMachineLocation().getLocalControllerId());
        assertEquals(VirtualMachineStatus.ERROR, vm.getStatus());
        assertEquals(VirtualMachineErrorCode.INVALID_HOST_ID, vm.getErrorCode());
    }
    
    /**
     * Test with a lc set.
     * should position the gm & the lc
     */
    public void testSetVirtualMachineLocationLCset()
    {
        VirtualMachineMetaData vm = new VirtualMachineMetaData();
        virtualClusterManager_.setVirtualMachineLocation(vm, "lc1");
        
        assertEquals("gm1", vm.getVirtualMachineLocation().getGroupManagerId());
        assertEquals("lc1", vm.getVirtualMachineLocation().getLocalControllerId());
    }
    
    /**
     * Test with a wrong lc set.
     * should position the gm & the lc to default and error code to invalid_host_id
     */
    public void testSetVirtualMachineLocationWrongLCset()
    {
        VirtualMachineMetaData vm = new VirtualMachineMetaData();
        virtualClusterManager_.setVirtualMachineLocation(vm, "lc42");
        
        assertEquals(Globals.DEFAULT_INITIALIZATION, vm.getVirtualMachineLocation().getGroupManagerId());
        assertEquals(Globals.DEFAULT_INITIALIZATION, vm.getVirtualMachineLocation().getLocalControllerId());
        assertEquals(VirtualMachineStatus.ERROR, vm.getStatus());
        assertEquals(VirtualMachineErrorCode.INVALID_HOST_ID, vm.getErrorCode());
    }
    
}
