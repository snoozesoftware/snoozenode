#!/bin/bash
#
# Copyright (C) 2010-2013 Eugen Feller, INRIA <eugen.feller@inria.fr>
#
# This file is part of Snooze, a scalable, autonomic, and
# energy-aware virtual machine (VM) management framework.
#
# This program is free software: you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation, either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, see <http://www.gnu.org/licenses>.
#

SNOOZE_PACKAGE_NAME="snoozenode"
SNOOZE_JAR_NAME="uber-snoozenode-1.1.1-SNAPSHOT.jar"

# Update config files
cp -R ../../configs/framework/quartz.properties $SNOOZE_PACKAGE_NAME/debian/input/configs/
cp -R ../../configs/framework/snooze_node.cfg $SNOOZE_PACKAGE_NAME/debian/input/configs/
cp -R ../../configs/framework/log4j.xml $SNOOZE_PACKAGE_NAME/debian/input/configs/
cp -R ../../configs/init_script/snoozenode $SNOOZE_PACKAGE_NAME/debian/input/init.d/
cp -R ../../configs/powermanagement/pm-utils-hook/00snoozenode \
                                    $SNOOZE_PACKAGE_NAME/debian/input/pm-utils/

# Update snooze jar
cp -R ../../target/$SNOOZE_JAR_NAME $SNOOZE_PACKAGE_NAME/debian/input/snoozenode.jar

# Generate package (with fakeroot)
export LD_LIBRARY_PATH=/usr/lib/libfakeroot:/usr/lib/x86_64-linux-gnu/libfakeroot/
export LD_PRELOAD=libfakeroot-sysv.so

cd $SNOOZE_PACKAGE_NAME
./rules
