# Define variables for classpath and source directories
CLASSPATH := .:libs/commons-cli-1.6.0.jar:libs/commons-pool2-2.12.0.jar:libs/jedis-5.1.2.jar:libs/slf4j-api-2.0.12.jar:libs/slf4j-simple-2.0.12.jar:libs/gson-2.10.1.jar
PROJECT_DIR :=p4
SERVER_DIR := server
CLIENT_DIR := client
RMI_REGISTRY_PORT_SERVER_SIDE := 5182
RMI_REGISTRY_PORT_CLIENT_SIDE := 5181


#killing existing registry
kill_registry_server_side:
	@echo "Killing RMI registry on port $(RMI_REGISTRY_PORT_SERVER_SIDE)"
	-pkill -f "rmiregistry.*$(RMI_REGISTRY_PORT_SERVER_SIDE)"

#killing existing registry
kill_registry_client_side:
	@echo "Killing RMI registry on port $(RMI_REGISTRY_PORT_CLIENT_SIDE)"
	-pkill -f "java.*$(RMI_REGISTRY_PORT_CLIENT_SIDE)"

# Define targets
all: compile

# Rule to compile Java files
compile:
	javac -cp "$(CLASSPATH)" $(SERVER_DIR)/*.java $(CLIENT_DIR)/*.java

# Clean rule
clean:
	rm -f $(SERVER_DIR)/*.class $(CLIENT_DIR)/*.class
clean_redis:
	redis-cli flushall
start_redis:
	redis-server &
kill_java:
	killall -9 java
