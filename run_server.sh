#!/bin/bash
# Execute script to kill server
./p4/kill_server.sh
# Start RMI registry
rmiregistry 5181 &

# Run the server
#java -cp ".:p4/libs/commons-cli-1.6.0.jar:p4/libs/commons-pool2-2.12.0.jar:p4/libs/jedis-5.1.2.jar:p4/libs/slf4j-api-2.0.12.jar:p4/libs/slf4j-simple-2.0.12.jar:p4/libs/gson-2.10.1.jar" p4.server.IdServer 5181 IdServer_1
#java -cp ".:p4/libs/commons-cli-1.6.0.jar:p4/libs/commons-pool2-2.12.0.jar:p4/libs/jedis-5.1.2.jar:p4/libs/slf4j-api-2.0.12.jar:p4/libs/slf4j-simple-2.0.12.jar:p4/libs/gson-2.10.1.jar" p4.server.IdServer 5181 IdServer_2
#java -cp ".:p4/libs/commons-cli-1.6.0.jar:p4/libs/commons-pool2-2.12.0.jar:p4/libs/jedis-5.1.2.jar:p4/libs/slf4j-api-2.0.12.jar:p4/libs/slf4j-simple-2.0.12.jar:p4/libs/gson-2.10.1.jar" p4.server.IdServer 5181 IdServer_3