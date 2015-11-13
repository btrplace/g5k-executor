#!/bin/bash

VM_NAME="$1"
NODE_SRC="$2"
NODE_DEST="$3"
BANDWIDTH="$4"
VIRSH_OPTS="$5"

# Translate name to real
VM_NAME=`cat translate | grep -P "$VM_NAME\t" | awk '{print $2;}'`
NODE_SRC=`cat translate | grep -P "$NODE_SRC\t" | awk '{print $2;}'`
NODE_DEST=`cat translate | grep -P "$NODE_DEST\t" | awk '{print $2;}'`

SSH_USER="root"
SSH_OPTS=' -o StrictHostKeyChecking=no -o BatchMode=yes -o UserKnownHostsFile=/dev/null -o LogLevel=quiet '


# Convert Mb/s to MiB/s and round
BANDWIDTH_OCTET=`bc <<< "$BANDWIDTH/8.388608"`

START=$(date +%s)
echo -e "Start:\tMigrate $VM_NAME from $NODE_SRC to $NODE_DEST at $BANDWIDTH Mbps ($BANDWIDTH_OCTET MiB/s)"


# Set bandwidth
virsh --connect qemu+tcp://$NODE_SRC/system migrate-setspeed $VM_NAME --bandwidth $BANDWIDTH

# Do the migration
virsh --connect qemu+tcp://$NODE_SRC/system migrate $VIRSH_OPTS $VM_NAME qemu+tcp://$NODE_DEST/system

# Ensure libvirt is not crashed
ssh $SSH_USER@$NODE_DEST $SSH_OPTS "sudo /etc/init.d/libvirt-bin start"
ssh $SSH_USER@$NODE_SRC $SSH_OPTS "sudo /etc/init.d/libvirt-bin start"


END=$(date +%s)
echo -e "End:\tMigrate $VM_NAME from $NODE_SRC to $NODE_DEST at $BANDWIDTH Mbps ($BANDWIDTH_OCTET MiB/s)\t(time=$(($END - $START)))s"
