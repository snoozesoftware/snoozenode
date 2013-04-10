package org.inria.myriads.snoozenode.database.api.impl;


import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.AssignedGroupManager;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;

import junit.framework.TestCase;

public class TestGroupLeaderMemoryRepository extends TestCase
{

    GroupLeaderMemoryRepository repository_;
    /**
     * 
     * Setup method.
     * 
     */
    @Override
    protected void setUp() throws Exception
    {
        String[] virtualMachineSubnets = {"192.168.122.0/30"};
        repository_ = new GroupLeaderMemoryRepository(virtualMachineSubnets,0);
    }
    
    /**
     * Empty repository.
     * -> null
     * 
     */
    public void testGetAssignedGroupManagerEmptyRepository()
    {
        NetworkAddress address = new NetworkAddress();
        address.setAddress("10.0.0.1");
        address.setPort(5000);
        AssignedGroupManager assignedGroupManager = repository_.getAssignedGroupManager(address);
        assertNull(assignedGroupManager);
    }
    
    
    /**
     * No assigment found.
     *  
     */
    public void testGetAssignedGroupManagerNoAssignement()
    {
        GroupManagerDescription gm1 = new GroupManagerDescription();
        LocalControllerDescription lc1 = new LocalControllerDescription();
        lc1.setId("lc1");
        NetworkAddress address = new NetworkAddress();
        address.setAddress("10.0.0.1");
        address.setPort(5000);
        lc1.setControlDataAddress(address);
        gm1.getLocalControllers().put("lc1", lc1);
        repository_.addGroupManagerDescription(gm1);
        
        NetworkAddress address2 = new NetworkAddress();
        address2.setAddress("10.0.0.2");
        address2.setPort(5000);
        AssignedGroupManager assignedGroupManager = repository_.getAssignedGroupManager(address2);
        assertNull(assignedGroupManager);
    }
    
    /**
     * One assigment found.
     *  
     */
    public void testGetAssignedGroupManagerOneAssignement()
    {
        GroupManagerDescription gm1 = new GroupManagerDescription();
        LocalControllerDescription lc1 = new LocalControllerDescription();
        lc1.setId("lc1");
        NetworkAddress address = new NetworkAddress();
        address.setAddress("10.0.0.1");
        address.setPort(5000);
        lc1.setControlDataAddress(address);
        gm1.getLocalControllers().put("lc1", lc1);
        repository_.addGroupManagerDescription(gm1);
        
        NetworkAddress address2 = new NetworkAddress();
        address2.setAddress("10.0.0.1");
        address2.setPort(5000);
        AssignedGroupManager assignedGroupManager = repository_.getAssignedGroupManager(address2);
        
        assertEquals("lc1", assignedGroupManager.getLocalControllerId());
        assertEquals(gm1, assignedGroupManager.getGroupManager());
    }

    public void testGenerateAddressPoolOneSubnet()
    {
        String[] virtualMachineSubnets = {"192.168.122.0/30"};
        GroupLeaderMemoryRepository repository = new GroupLeaderMemoryRepository(virtualMachineSubnets,0);
        repository.generateAddressPool(virtualMachineSubnets);
        assertEquals(2,repository.getNumberOfFreeIpAddresses());
    }
    
    public void testGenerateAddressPoolTwoSubnets()
    {
        String[] virtualMachineSubnets = {"192.168.122.0/22", "10.0.0.1/22"};
        GroupLeaderMemoryRepository repository = new GroupLeaderMemoryRepository(virtualMachineSubnets,0);
        repository.generateAddressPool(virtualMachineSubnets);
        assertEquals(2044,repository.getNumberOfFreeIpAddresses());

    }
}
