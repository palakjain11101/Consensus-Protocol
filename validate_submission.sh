#!/bin/bash

files=(Coordinator.java Participant.java UDPLoggerServer.java UDPLoggerClient.java Vote.java CoordinatorLogger.java ParticipantLogger.java  Coordinator.class Participant.class UDPLoggerServer.class UDPLoggerClient.class CoordinatorLogger.class ParticipantLogger.class Vote.class)
lport=12344
cport=12345
n=3
pport1=12346
pport2=12347
pport3=12348
timeout=500
options="A B"

check_status() {
	if [ ! $? -eq 0 ]
	then
		echo "Error, script terminated"
		exit 1
	fi
}

silently_kill() {
	{ kill -9 $1 && wait $1; } 2>/dev/null
}

echo
echo "This script will validate your COMP2207 1920 coursework submission by checking whether:"
echo -e "\t - the zip file can be unzipped (this requires unzip command to be installed)"
echo -e "\t - all required files are there"
echo -e "\t - all the included Java source files can be compiled"
echo -e "\t - UDPLoggerServer, Coordinator and Participant processes can be started"
echo -e "\t - required log files are created"
echo
echo "Please make sure that your zip file is successfully validated with this script before submitting it."
echo
echo "This script will unzip files in ./tmp/ directory, and compile and execute your software in ./run/ directory. Both folders will be created in the current working directory. If those directories already exist, they will first be deleted, together with all their content."
read -p "Are you sure you want to continue (y/n)" -n 1 -r
echo
echo

if [[ $REPLY =~ ^[Yy]$ ]]
then
	echo "Validation started"
else
	echo "Script not executed"
	exit 0
fi

if [ -d tmp/ ]; then
	echo -n "tmp directory already exists, deleting it..."
	rm -fdr tmp/
	check_status
	echo "ok"
fi

echo -n "Creating tmp directory..."
mkdir tmp
check_status
echo "ok"

echo -n "Unzipping submission file..."
unzip $1 -d tmp/ > /dev/null
check_status
echo "ok"

echo -n "Checking all required files exist..."
cd tmp/
for f in ${files[@]}; do
	if [ ! -f "$f" ]; then
		echo "$f does not exist"
		exit 1
	fi
done
echo "ok"

cd ..

if [ -d run/ ]; then
	echo -n "run directory already exists, deleting it..."
	rm -fdr run/
	check_status
	echo "ok"
fi

echo -n "Creating run directory..."
mkdir run
check_status
echo "ok"

cd run/

echo -n "Compiling Java sources..."
javac ../tmp/*.java -d .
check_status
echo "ok"

echo -n "Starting UDPLoggerServer on port ${lport}..."
java UDPLoggerServer $lport > /dev/null &
loggerId=$!
check_status
sleep 1s
echo "ok"

echo -n "Starting Coordinator on port ${cport} with ${n} participants, timout ${timeout}ms and options ${options} ..."
java Coordinator $cport $lport 3 $timeout $options > /dev/null &
coordinatorId=$!
check_status
sleep 1s
echo "ok"

echo -n "Starting ${n} Participant processes on ports ${pport1}, ${pport2} and ${pport3} with timout ${timeout}ms..."
java Participant $cport $lport $pport1 $timeout > /dev/null &
p1Id=$!
check_status
java Participant $cport $lport $pport2 $timeout > /dev/null &
p2Id=$!
check_status
java Participant $cport $lport $pport3 $timeout > /dev/null &
p3Id=$!
check_status
echo "ok"

echo -n "Wait 10s..."
sleep 10s
check_status
echo "ok"

echo -n "Kill all Java processes still running..."
silently_kill $p1Id 
silently_kill $p2Id
silently_kill $p3Id
silently_kill $coordinatorId
silently_kill $loggerId
echo "ok"

echo -n "Checking all required log files exist..."
if [ ! -n "$(find . -name 'logger_server_*.log' | head -1)" ]; then
	echo "log file of logger server not found"
fi

if [ ! -n "$(find . -name 'coordinator_*.log' | head -1)" ]; then
        echo "log file of coordinator not found"
fi

p1log="participant_${pport1}_*.log"
if [ ! -n "$(find . -name ${p1log} | head -1)" ]; then
        echo "log file of participant ${pport1} not found"
fi

p2log="participant_${pport2}_*.log"
if [ ! -n "$(find . -name ${p2log} | head -1)" ]; then
        echo "log file of participant ${pport2} not found"
fi

p3log="participant_${pport3}_*.log"
if [ ! -n "$(find . -name ${p3log} | head -1)" ]; then
        echo "log file of participant ${pport3} not found"
fi
echo "ok"

echo "Validation successfully completed."
