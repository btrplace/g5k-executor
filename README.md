# Grid'5000 executor

This repository contains the source code of the BtrPlace g5k-executor. It allows to execute [BtrPlace JSON converted plans](https://github.com/btrplace/scheduler/wiki/JSON-Messages#json-format-of-a-reconfigurationplan) into [Grid'5000 infrastructure](https://www.grid5000.fr/mediawiki/index.php/Grid5000:Home).

For a full example, have a look at [this repository](https://github.com/btrplace/migrations-UCC-15) that contains all the informations to reproduce the experiments from the paper "Scheduling Live-Migrations for Fast, Adaptable and Energy Efficient Relocation Operation" published at [UCC'15](http://cyprusconferences.org/ucc2015). 

Requirements:
* JDK 8+
* maven 3+

## Get the latest version

First, get the laster version (`master` branch) and compile it using `maven`:

``` shell
# Get it and compile it
git clone https://github.com/btrplace/g5k-executor.git
cd g5k-executor
mvn -Dmaven.test.skip=true package
```

A distribution tarball is generated into the `target` folder, you can now extract and start to use the executor. The main script is `g5kExecutor` For example, execute it as is to show the cmdline options:

``` shell
# Start to use it
cd target
tar xzf g5k-1.0-SNAPSHOT-distribution.tar.gz
cd g5k-1.0-SNAPSHOT/
./g5kExecutor
```
 
## Usage
  
You first need to edit the migration script `g5k-1.0-SNAPSHOT/scripts/migrate.sh` and modify at least the variable `VM_BASE_IMG` to match your VM image location.

Then, the `g5k-1.0-SNAPSHOT/scripts/translate` file must be modified to allow to translate VMs and g5k nodes names into the BtrPlace internal VMs and nodes names, the file should look like this:

``` txt
vm-1 vm#0
vm-2 vm#1
...
griffon-60 node#0
griffon-61 node#1
...
```
 
### Cmdline options
 
``` txt
g5kExecutor [-d scripts_dir] (-mvm|-buddies -p <x>) -i <json_file> -o <output_file>
 -buddies (--memory-buddies-scheduler) : Select the scheduler of Memory buddies
 -d (--scripts-dir) VAL                : Scripts location relative directory
 -i (--input-json) VAL                 : The json reconfiguration plan to read
                                         (can be a .gz)
 -mvm (--mvm-scheduler)                : Select the scheduler of mVM (default
                                         choice)
 -o (--output-csv) VAL                 : Print actions durations to this file
 ```
 
### Usage examples

Execute a BtrPlace recnofiguration plan using the `mVM` scheduler:
 
 ``` shell
 ./g5kExecutor --mvm-scheduler --input-json <JSON_FILE> --output-csv <OUTPUT_CSV>
```

Execute it using the [Memory Buddies](http://dl.acm.org/citation.cfm?id=1508299) scheduler, with a parallelism set to 2:

``` shell
./g5kExecutor --memory-buddies-scheduler --parallelism 2 -i <JSON_FILE> -o <OUTPUT_CSV>
```

The `<OUTPUT_CSV>` file contains 3 fields: `ACTION;START;END` where `ACTION` represents the BtrPlace String representation of the action, `START` and `END` correspond respectively to the start and end time of the action in the form of timestamps.
