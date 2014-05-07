# Changelog

## Version 2.1.4

  - implement searchVirtualMachine in GroupManagerCassandraRepository.
  - implement getLocalControllerList in GroupManagerResource.

## Version 2.1.3

Bug fixes

  - #83 fix serial console template issue.
  - #81 handle destroy image with backingImageManager and src=dest path.

Features : 
  - #82 Get the vm lists even if in-memory database is used.

## Version 2.1.2 - 14-01-21

Bug fixes : 

  - local images are destroyed after destroying the virtual machines

## Version 2.1.1 - 14-01-09

Bug fixes :

  - Fix compatibility with euca2ools
  - Fix migration issue when using backing mode.


