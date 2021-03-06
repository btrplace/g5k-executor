#!/bin/bash

NODE="$1"
BMC_USER="$2"
BMC_MDP="$3"
NOTIF_DIR="$4"

# Translate name to real
NODE=`cat translate | grep -P "$NODE\t" | awk '{print $2;}'`

function wait_for_boot {
	local NODE="$1"

	while ! ping -c 1 $NODE > /dev/null; do
		sleep 2
	done

	# If NOTIF_DIR is specified, wait until the booted node create a file (with his hostname) in this folder
	if [ -n "$NOTIF_DIR" ]; then
		while [ ! -f "$NOTIF_DIR/$NODE" ]; do
			sleep 2
		done
		rm -rf $NOTIF_DIR/$NODE
	else
		# Wait 30 more seconds (start services)
		sleep 30
	fi
}


if [ -n "$NODE" ]; then
	if [ -f "$NODE" ]; then
		echo -e "Start:\tPowering on $(cat $NODE | wc -l) nodes"

		if [ -n "$BMC_USER" -a -n "$BMC_MDP" ]; then
			# Using IPMI
			for N in `cat $NODE`; do
				echo "ERROR" > /tmp/error-${NODE}
				while [ $(cat /tmp/error-$NODE | wc -l) -gt 0 ]; do
					ipmitool -H $(host $(echo "$N" | cut -d'.' -f1)-bmc.$(echo "$N" | cut -d'.' -f2,3,4) | awk '{print $4;}') -I lan -U $BMC_USER -P $BMC_MDP chassis power on > /dev/null 2>/tmp/error-$NODE
				done
			done
		else
			# Using kapower
			kapower3 --on -f $NODE >/dev/null
		fi

		# Wait for boot of all nodes
		START=$(date +%s)
		for N in `cat $NODE`; do
			wait_for_boot $N &
		done
		wait
		END=$(date +%s)

		echo -e "End:\tPowering on $(cat $NODE | wc -l) nodes\t(time=$(($END - $START)))s"
		
	else
		echo -e "Start:\tPowering on node '$NODE'"

		if [ -n "$BMC_USER" -a -n "$BMC_MDP" ]; then
			# Using IPMI
			echo "ERROR" > /tmp/error-$NODE
			while [ $(cat /tmp/error-$NODE | wc -l) -gt 0 ]; do
				ipmitool -H $(host $(echo "$NODE" | cut -d'.' -f1)-bmc.$(echo "$NODE" | cut -d'.' -f2,3,4) | awk '{print $4;}') -I lan -U $BMC_USER -P $BMC_MDP chassis power on > /dev/null 2>/tmp/error-$NODE
			done
		else
			# Using kapower
			kapower3 --on -m $NODE >/dev/null
		fi

		# Wait for boot
		START=$(date +%s)
		wait_for_boot $NODE
		END=$(date +%s)

		echo -e "End:\tPowering on node '$NODE'\t(time=$(($END - $START)))s"
	fi
fi
