#pragma once

#include <thread>
#include <mutex>
#include <atomic>
#include <string>
#include "ConnectionHandler.h"
#include "StompProtocol.h"

// Global mutex for synchronization
// extern mutex mtx;
class StompClient {
// Function declarations
public:
    /**
    * Keyboard thread function - handles user input and starts the server thread after login.
    * @param protocol Reference to the StompProtocol instance.
    * @param shouldTerminate Atomic boolean for signaling termination.
    */
    void keyboardReader(StompProtocol& protocol);

    /**
    * Server thread function - listens to server responses and processes them.
    * @param connectionHandler Reference to the ConnectionHandler instance.
    * @param shouldTerminate Atomic boolean for signaling termination.
    */
    void serverReader(StompProtocol& protocol);

};
