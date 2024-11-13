# Programming Project 4
## Distributed Identity Server (Phase III)

* Team members: Sheikh Md Mushfiqur Rahman & Shaznin Sultana
* Course: CS555 [Distributed Systems] ; Spring 2024 

## Overview
In this project, we are deploying multiple server instances, on different machines, to enhance reliability. Clients can connect to any server for improved performance. Among these servers, one will be elected as the leader, responsible for servicing client requests and checkpointing the state to disk and other servers. In case the leader becomes unreachable, a new leader is elected without any loss of state within our defined inconsistency window. For consistency, Local-Write protocol is implemented. It is chosen because it implements sequential consistency. For server synchronization we are implementing Lamport timestamps.

This is also a RMI-based Distributed Identity Server. Server generates UUIDs for unique clients upon request and provides functionalities like removing accounts, looking up accounts by UUID or name, and updating account information. All communication between server and client is encrypted with SSL encryption protocol. The server uses Redis as its data storage, a popular key-value NoSQL database. 

Bully algorithm functionality in short: 

After binding to the registry, a server initiates an election. When a new server joins, if its power exceeds that of the current server, the lower-powered server challenges it. The higher-powered server acknowledges and triggers a new election. If it remains the most powerful, it declares itself as the coordinator. Otherwise, it acknowledges the new coordinator declared by the higher-powered server. This process repeats until a coordinator is established.
Every 3 seconds, servers send "are you alive" messages to each other when no election is in progress, ensuring registry lookups remain successful. If a server fails to respond, triggering an exception, it checks if the crashed server was the coordinator or a general server. If the coordinator, it initiates a new election; otherwise, it reports the crashed server. Upon the crashed server's return, the election process restarts.

## Building the code

Clone the repository and checkout to the remote p4_branch. Then follow the below commands:

1. `make -C p4 clean` to remove class files.
2. `make -C p4 kill_registry_server_side` to kill existing registry (on port 5182. this is for server.).
3. `make -C p4 kill_registry_client_side` to kill existing registry (on port 5181. this is for client).
4. `make -C p4 compile` to compile java files.
6. You can do all of the above work by executing `prepare_environment.sh` script as well.
7. Run `redis-cli ping` and check the appropriate `PONG` response, otherwise, start the redis server again `redis-server`.
8. Clean the redis server with `redis-cli flushall`.
9. `java -cp ".:p4/libs/commons-cli-1.6.0.jar:p4/libs/commons-pool2-2.12.0.jar:p4/libs/jedis-5.1.2.jar:p4/libs/slf4j-api-2.0.12.jar:p4/libs/slf4j-simple-2.0.12.jar:p4/libs/gson-2.10.1.jar" p4.server.IdServerMain onyxnode66.boisestate.edu onyxnode70.boisestate.edu onyxnode55.boisestate.edu` to run the server and bind. (You have to include the server name of the server in which you are executing the command as well)

10. `java -cp ".:p4/libs/commons-cli-1.6.0.jar:p4/libs/gson-2.10.1.jar" p4.client.IdClient -s onyxnode70.boisestate.edu -n 5181 -c shaznin12 shazninsultana -p shaznin1234` to run client and execute operation on `onyxnode70.boisestate.edu`.  It could be any other server on which your are running on you registry.

11. You don't need to go inside the `p4` folder to do any of it.
12. If there are any change, you should execute the previous steps.
13. For client side, first run `java -cp ".:p4/libs/commons-cli-1.6.0.jar" p4.client.IdClient`. It shows all the functionalities possible by the server and respective client commands.
14. **NB:** Occasionaly we faced an issue while client call that says something like this **"Remote Exception:no such object in table"**. In that case, In the terminal of the server for which this problem is occuring, type `killall -9 java`. This command actually helped us a lot.

## Manifest

* IdServerForClient.java: The server implementation for client-side operations in the IdServer application. This class implements the IdServerInterfaceForClient interface and provides methods for creating, modifying, and deleting users on the client side. It also manages the Lamport clock for synchronization purposes. It uses SSL encrypted communication.
Necessary methods created for implementing the cli options from the client. These options are basically providing functionalities such as removing accounts, looking up accounts by UUID or name, and updating account information, getting all user information  upon client request.

* IdServerForServer.java: The server implementation for server-side operations in the IdServer application. This class implements the IdServerInterfaceForServer interface. It implements the bully algorithm , lamport clock, and all necessary communication among servers. This doesn't use SSL encryption.

* IdServerMain:  The main class for the IdServer application. This class initializes and starts the ID server, binding it to the RMI registry for client and server-side interactions. It also sets up SSL properties, defines the server address map, and implements a shutdown hook to handle server shutdown processes.

* IdClient.java: This file contains necessary properties setup for establishing encrypted SSL connection with server. Used 'apache commons cli' to parse command line arguments/options. The options are created and the methods are called for the respective options.

* User.java: a java class that represetns a user object.

* UserDbOperation.java: a java class which contains the abstract database operations such as create, update, delete.

* libs: This folder contains all the necessary dependencies(.jar files) such as for redis, apache commons cli etc.

* resources: All necessary resources for SSL encryption are in this folder. Under this folder, Server.cer is the self signed certificate, Client_Trustore is used by the client to verify the server's identity,  Server_Keystore contains the server's certificate and private key used to authenticate and encrypt communication with the client.


## Testing

For testing, we simply conducted some test cases in different scenarios to verify the Identity Server is working properly:

* To check if the Server is correctly bound in RMI registry, after running the server, it shows `IdServerForClient: IdServerForClient bound in registry on port 5181` and
  `IdServerForServer: IdServerForServer bound in registry on port 5182 on this host`, otherwise it throws an exception.
* To check if the SSL connection and network transmissions is properly set up, `java -Djavax.net.debug=all IdClient - <args>`
* After starting the Redis Server, and three servers start running on onyx node, client can begin requesting services to any available servers.

Funtions testing:

* For functionality testing, three servers will be running on three different onyx nodes and two clients will be running in another two windows.

* Bully algorithm testing:

    * Upon running, each server gets a "server_id" which denotes its power for participating in bully election. After getting power, each server joins for election. 
    * One server only: When there is only one server alive, it starts election and wins by itself. Declares itself Coordinator.
    * Joins other servers: When other server joins, it checks those servers' power id and if it has lower power, it sends election challenge to other by sending message "*Sending election challenge to <HigherServerAddress>*". 
    On the other hand, higher powered server sends "*Received election request*" and starts election.
    * Winner node: Winner node declares itself Coordinator by sending message "*Letting other servers know that I (hostname) won the election*".
    * Repeater: All the servers checks other servers' availability by looking up on registry and says "*I am <hostname> and I am now checking if other server <otherServerHostname> is alive.*"
    * Non-conordinator server crash: For this test case, when a general server crashes, all the other servers identify its termination since there is a repeater that checks the availability of the servers in every 5 seconds. So, other servers prints "*onyxnodeXX has crashed*".
    * Coordinator server crash: For this case, when coordinator terminates, other servers identifies this incident "*coordinator onyxnodeXX has crashed. initiating election*" and starts election again. This time, the server with second highest power id wins and becomes coordinator.
    
        Now, if the previous coordinator comes back, it starts the election since it is now the highest power server. So, it wins the election again and takes over the coordinator position again.

* Lamport Clock testing:
To check whetehr the lamport clock is working, we shut down a random server, and continued create and modify operations on other servers. When The dead servers comes back, we check the full user list on that server to check whetehr it has updated it's database properly based on the lamport clock.

* Local-write protocol testing: 
To test this feature, we requested different servers from different client for both create and modify operations. And then request the list of all users from different servers. The lists were same from all of the server.
   
* Clients connect to two different servers and can modify the accounts and both see the results:

For example, two clients modifies account from two different servers and both will be able to see modified changes in both the servers.

* Server crashed and starts up again and synchronized:

If one server crashes and in the mean time, there is a modification in another server, the changes will be synchronized in other servers too. This means that the terminated server is synchronized with that modification even if during that change it was not alive. So, client can see the same results in any servers available.

* `java -cp ".:p4/libs/commons-cli-1.6.0.jar" p4.client.IdClient -s <hostname> -n 5181 -c <login name> [<realname>] -p <password>`

This command is to create new user id. `-c` is the option and 'login name', 'password' are required arguments for this function. 'real name' is optional. So without giving 'real name' in the argument, Server takes the name of the current user logged into the system as real name. Server responds to this service with an 'UUID'. Also, the login name must be unique to create an user.

* `java -cp ".:p4/libs/commons-cli-1.6.0.jar" p4.client.IdClient -s <hostname> -n 5181 -l <login name>`

This command is to lookup user account in the server with login name. This shows the UUID, login name, real name associated with this account.

* `java -cp ".:p4/libs/commons-cli-1.6.0.jar" p4.client.IdClient -s <hostname> -n 5181 -r <UUID>`

This command is a reverse lookup user account in the server with UUID.

* `java -cp ".:p4/libs/commons-cli-1.6.0.jar" p4.client.IdClient -s <hostname> -n 5181 -m <old login name> <new login name> -p <password>`

This command is to modify and request for a new login name. It doesn't change the UUID. Also, if the new login name is already taken, it shows error.

* `java -cp ".:p4/libs/commons-cli-1.6.0.jar" p4.client.IdClient -s <hostname> -n 5181 -g  <users | UUIDs | all>`

This command returns all the user names or UUIDs or every details of the user accounts including name, uuid, ip address, created time, modified time. Note that no commands in the shows the passwords. The passwords are encrypted.

* `java -cp ".:p4/libs/commons-cli-1.6.0.jar" p4.client.IdClient -s <hostname> -n 5181 -d <login name> -p <password>`

This command deletes user account with the requested login name and correct password.


## Video Link: 

https://boisestate.zoom.us/rec/share/y_vKFKgbTJo1qXi3v6YtaZmmIRS6nbEdVncCgpHyN-UQAQm1BC02bGibKCIPa2bL.Nf2gHnFDVMA9c5nW?startTime=1714961468000


## Reflection

* Roles of each Team Members:

Sheikh Md Mushfiqur Rahman: I basically implemented 

- The crud operations of both server side and client side server. 
- The local write protocol where user can request any server, but all of the updates will be transferred to every alive server.
- The lamport clock for servers, depending on which a dead server when comes back can update it's database 
- how other servers receive the update call.
- deployment of the project on onyx.
- Running tests.
- Overall I mapped out the basic architecture of the project.

One of the most annoying issue that I faced in this project was deploying it remote. As you
already know, we were stuck in ec2 deployment issue almost for 2 days. At the end, I was able to
deploy the project on onyx but that too with some issues that we already notified the instructor.
I think overall the implementation was easy however the requirements for the poject is vague -
which made it more complex that it should. We were not provbided enough materials regarding
the ec2. If we had, we would not need 2 days extra and loose 10%, we would have been able to
submit the project within due date as most of our works were already done. I learned a lot in
this project, however, and the 10% deduction makes me sad.


Shaznin Sultana: I implemented the 

- SSL+RMI connection, 
- Bully algorithm, 
- apache commons cli options, 
- contributed in server-client functions implementations,
- overall testing usecases, 
- written readme, javadocs, 
- some bugs and error solving.

* In my opinion, the concept was confusing because of complex scenario implementation but not
difficult. The main challenge was to make it work in remote. I tried aws ec2 instances, onyx
nodes and still it was really difficult and frustrating to make it work and getting looped
into almost same problems of connection timeout, unmarshal error, and some common frequent
erros. These errors also made it hard to work on the main functionalities of the project. For
example, we implemented all our functionalities in local very early, but still we were dealing
with these connection and other frustrating errors.

* Implementing bully algorithm seemed fun to me honestly. I followed some documentations,
class materials, internet sources to get a clear idea. Also, for any of my portion of the work,
may it be errors or implementations, I tried to follow the class materials and took help from
internet sources.

* In my case, like last time, I was having issues using 'Windows' for the projects. I had to
work with WSL and still there was basic linux-windows issues to deal with. These issues were
not difficult, I would say a bit irritating. However, this time I also took my friend's linux
machine to overcome those problems.

* Overall, I think, there were not enough materials for using aws ec2 with RMI-SSL. I felt like
there were mostly some common issues of ec2 instances that could be easily solved if we were
provided more materials or guidelines for this. Also, I think this last project pdf was less
organized compared to the other two.

* Regardless, I have learnt so many things from this project implementation that I cannot even
list out here. Just not happy with the 10% deductions for being late.
