package org.inria.myriads.snoozenode.database.api.impl;

import junit.framework.TestCase;

public class TestGroupLeaderMemoryRepository extends TestCase
{

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
