package org.inria.myriads.snoozenode.groupmanager.virtualclustermanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.globals.Globals;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupLeaderSchedulerSettings;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.enums.Dispatching;

import junit.framework.TestCase;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class TestVirtualClusterManager extends TestCase
{

    
    VirtualClusterManager virtualClusterManager_; 
    
    public void setUp() throws Exception 
    {
        GroupLeaderRepository repository = EasyMock.createMock(GroupLeaderRepository.class);
        NodeConfiguration nodeConfiguration = EasyMock.createMock(NodeConfiguration.class);
        ResourceDemandEstimator estimator = EasyMock.createMock(ResourceDemandEstimator.class);
        GroupLeaderSchedulerSettings groupLeaderScheduler = new GroupLeaderSchedulerSettings();
        groupLeaderScheduler.setDispatchingPolicy(Dispatching.FirstFit);
        
       
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
        GroupLeaderSchedulerSettings gms = nodeConfiguration.getGroupLeaderScheduler();

        virtualClusterManager_ = new VirtualClusterManager(nodeConfiguration, repository, estimator);

    }
    
    /**
     * Test with a gm set
     * should position the gm only
     */
    public void testSetVirtualMachineLocationGMset()
    {
        VirtualMachineMetaData vm = new VirtualMachineMetaData();
        virtualClusterManager_.setVirtualMachineLocation(vm, "gm1");
        
        assertEquals("gm1", vm.getGroupManagerLocation().getGroupManagerId());
        assertEquals(Globals.DEFAULT_INITIALIZATION, vm.getVirtualMachineLocation().getLocalControllerId());
    }

    
    
    /**
     * Test with a wrong gm set
     * should position the gm only
     */
    public void testSetVirtualMachineLocationWrongGMset()
    {
        VirtualMachineMetaData vm = new VirtualMachineMetaData();
        virtualClusterManager_.setVirtualMachineLocation(vm, "gm42");
        
        assertEquals(Globals.DEFAULT_INITIALIZATION, vm.getGroupManagerLocation().getGroupManagerId());
        assertEquals(Globals.DEFAULT_INITIALIZATION, vm.getVirtualMachineLocation().getLocalControllerId());
    }
    
    /**
     * Test with a lc set
     * should position the gm & the lc
     */
    public void testSetVirtualMachineLocationLCset()
    {
        VirtualMachineMetaData vm = new VirtualMachineMetaData();
        virtualClusterManager_.setVirtualMachineLocation(vm, "lc1");
        
        assertEquals("gm1", vm.getGroupManagerLocation().getGroupManagerId());
        assertEquals("lc1", vm.getVirtualMachineLocation().getLocalControllerId());
    }
    
    /**
     * Test with a wrong lc set
     * should position the gm & the lc
     */
    public void testSetVirtualMachineLocationWrongLCset()
    {
        VirtualMachineMetaData vm = new VirtualMachineMetaData();
        virtualClusterManager_.setVirtualMachineLocation(vm, "lc42");
        
        assertEquals(Globals.DEFAULT_INITIALIZATION, vm.getGroupManagerLocation().getGroupManagerId());
        assertEquals(Globals.DEFAULT_INITIALIZATION, vm.getVirtualMachineLocation().getLocalControllerId());
    }
    
}
