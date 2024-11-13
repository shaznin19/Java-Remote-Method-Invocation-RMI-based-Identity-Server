#!/bin/bash

# Function to kill rmiregistry
kill_rmiregistry() {
    # Find PID of rmiregistry process
    pid=$(ps aux | grep rmiregistry | grep -v grep | awk '{print $2}')
    if [ ! -z "$pid" ]; then
        echo "Killing rmiregistry process with PID: $pid"
        kill $pid
    else
        echo "No rmiregistry process found."
    fi
}

# Call the function to kill rmiregistry
kill_rmiregistry
