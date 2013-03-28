package org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineGroupManagerLocation;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.globals.Globals;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl.Static;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;

import junit.framework.TestCase;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class TestStatic extends TestCase
{


    /** The resource demand estimator (mock).*/
    private ResourceDemandEstimator estimator_;
    
    /** The static dispatch policy (under test).*/
    PlacementPolicy staticPlacement_;
    
    /** List of virtual machines.*/
    private List<VirtualMachineMetaData> virtualMachines_;
    
    /** List of group managers.*/
    List<LocalControllerDescription> localControllers_;
    
    /** 
     * Setup method.
     * Initializes the estimator.
     * 
     * 
     */
    public void setUp() throws Exception 
    {
      estimator_ = EasyMock.createMock(ResourceDemandEstimator.class);
      staticPlacement_ = new Static(estimator_);
      virtualMachines_ = new ArrayList<VirtualMachineMetaData>();
      localControllers_ = new ArrayList<LocalControllerDescription>();

    }
    
    /**
     * vm1 is bound to lc1 and lc1 exist in the repository and has enough capacity 
     *
     */
    public void testPlaceOneLcBoundHasNotEnoughCapacity()
    {
        VirtualMachineMetaData vm1  = new VirtualMachineMetaData();
        LocalControllerDescription lc1 = new LocalControllerDescription();
        lc1.setId("1");
        VirtualMachineLocation location = new VirtualMachineLocation();
        location.setLocalControllerId("1");
        vm1.setVirtualMachineLocation(location);
        
        virtualMachines_.add(vm1);
        localControllers_.add(lc1);
        
        expect(estimator_.hasEnoughLocalControllerCapacity(vm1, lc1)).andReturn(true);
        replay(estimator_);
        
        PlacementPlan placementPlan = staticPlacement_.place(virtualMachines_, localControllers_);
        
        List<LocalControllerDescription> targetLocalControllers = placementPlan.getLocalControllers();
        List<VirtualMachineMetaData> unassignedVirtualMachines = placementPlan.gettUnassignedVirtualMachines();
        
        assertEquals(targetLocalControllers.size(), 1);
        assertEquals(targetLocalControllers.get(0), lc1);
        assertEquals(unassignedVirtualMachines.size(), 0);
    }
    
    /**
     * vm1 is bound to lc1 and lc1 exist in the repository and has not enough capacity 
     *
     */
    public void testPlaceOneLcBoundNotEnoughCapacity()
    {
        VirtualMachineMetaData vm1  = new VirtualMachineMetaData();
        LocalControllerDescription lc1 = new LocalControllerDescription();
        lc1.setId("1");
        VirtualMachineLocation location = new VirtualMachineLocation();
        location.setLocalControllerId("1");
        vm1.setVirtualMachineLocation(location);
        
        virtualMachines_.add(vm1);
        localControllers_.add(lc1);
        
        expect(estimator_.hasEnoughLocalControllerCapacity(vm1, lc1)).andReturn(false);
        replay(estimator_);
        
        PlacementPlan placementPlan = staticPlacement_.place(virtualMachines_, localControllers_);
        
        List<LocalControllerDescription> targetLocalControllers = placementPlan.getLocalControllers();
        List<VirtualMachineMetaData> unassignedVirtualMachines = placementPlan.gettUnassignedVirtualMachines();
        
        assertEquals(0, targetLocalControllers.size());
        assertEquals(1, unassignedVirtualMachines.size());
        assertEquals(vm1, unassignedVirtualMachines.get(0));
    }
    
    
    /**
     * vm1 should be dispatch on lc1 but no lc in the repository 
     */
    public void testDispatchNoLc()
    {
        VirtualMachineMetaData vm1_  = new VirtualMachineMetaData();
        VirtualMachineLocation location = new VirtualMachineLocation();
        location.setLocalControllerId("1");
        
        vm1_.setVirtualMachineLocation(location);
        
        virtualMachines_.add(vm1_);

        PlacementPlan placementPlan = staticPlacement_.place(virtualMachines_, localControllers_);
        
        List<LocalControllerDescription> targetLocalControllers = placementPlan.getLocalControllers();
        List<VirtualMachineMetaData> unassignedVirtualMachines = placementPlan.gettUnassignedVirtualMachines();
        
        assertEquals(0, targetLocalControllers.size());
        assertEquals(1, unassignedVirtualMachines.size());
    }
    
    /**
     * vm1 is bound to lc1 but lc1 doesn't exist in the repository
     */
    public void testDispatchNoLcBound()
    {
        VirtualMachineMetaData vm1  = new VirtualMachineMetaData();
        LocalControllerDescription lc1 = new LocalControllerDescription();
        lc1.setId("1");
        VirtualMachineLocation location = new VirtualMachineLocation();
        location.setLocalControllerId("2");
        vm1.setVirtualMachineLocation(location);
        
        virtualMachines_.add(vm1);
        localControllers_.add(lc1);  
        
        expect(estimator_.hasEnoughLocalControllerCapacity(vm1, lc1)).andReturn(true);
        replay(estimator_);
        
        PlacementPlan placementPlan = staticPlacement_.place(virtualMachines_, localControllers_);
        
        List<LocalControllerDescription> targetLocalControllers = placementPlan.getLocalControllers();
        List<VirtualMachineMetaData> unassignedVirtualMachines = placementPlan.gettUnassignedVirtualMachines();
        
        assertEquals(targetLocalControllers.size(), 0);
        assertEquals(unassignedVirtualMachines.size(), 1);
        assertEquals(unassignedVirtualMachines.get(0), vm1);
    }
    
    
    /**
     * vm1 is not bound to any lc
     */
    public void testDispatchUnknownBound()
    {
        VirtualMachineMetaData vm1  = new VirtualMachineMetaData();
        LocalControllerDescription lc1 = new LocalControllerDescription();
        lc1.setId(Globals.DEFAULT_INITIALIZATION);
        VirtualMachineLocation location = new VirtualMachineLocation();
        location.setLocalControllerId("1");
        vm1.setVirtualMachineLocation(location);
        
        virtualMachines_.add(vm1);
        localControllers_.add(lc1);  
        
        //no expectation here
        
        PlacementPlan placementPlan = staticPlacement_.place(virtualMachines_, localControllers_);
        
        List<LocalControllerDescription> targetLocalControllers = placementPlan.getLocalControllers();
        List<VirtualMachineMetaData> unassignedVirtualMachines = placementPlan.gettUnassignedVirtualMachines();
        
        assertEquals(targetLocalControllers.size(), 0);
        assertEquals(unassignedVirtualMachines.size(), 1);
        assertEquals(unassignedVirtualMachines.get(0), vm1);
    }
    

}
