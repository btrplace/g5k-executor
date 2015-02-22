#!/bin/bash

NODE="$1"
BMC_USER="$2"
BMC_MDP="$3"
NOTIF_DIR="$4"

echo "$TEST"

echo "Booting node $NODE (`date +%S`)"
sleep 4