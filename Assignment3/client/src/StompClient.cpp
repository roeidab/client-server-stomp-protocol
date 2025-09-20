#include "StompClient.h"
#include <iostream>
#include <string>
#include <thread>
#include <mutex>

std::mutex mtx;

// Listening to the user inputs and processing them
void StompClient::keyboardReader(StompProtocol& protocol) {
    // Start the server listener thread
    std::thread serverThread(&StompClient::serverReader, this, std::ref(protocol));
    while (true) {
        try {
            std::string inputLine;
            std::getline(std::cin, inputLine);

            if (inputLine.empty()) continue;

            // Process the input using StompProtocol
            protocol.processKeyboardInput(inputLine);
        } catch (const std::exception& ex) {
            std::cerr << "Keyboard reader error: " << ex.what() << std::endl;
        }
    }
    std::cout<< "Terminating keyboardReader \n";
}

// Listening to the server, func for the serverThread
void StompClient::serverReader(StompProtocol& protocol) {
    while (true) {
        try {
            mtx.lock();
            std::string serverResponse;
            // Listen for server responses
            if (protocol.getConnectionHandler() && protocol.getConnectionHandler()->getLine(serverResponse)) {
                // std::cout<< "Received server response calling process \n";
                protocol.process(serverResponse);
            }
            mtx.unlock();
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        } 
        catch (const std::exception& ex) {
            std::cerr << "Error in server reader: " << ex.what() << std::endl;
        }
    }
    std::cout<< "Terminating serverReader \n";
}

int main() {
    StompClient client;
    StompProtocol protocol;
    client.keyboardReader(protocol);
    std::cout << "Client has terminated." << std::endl;
    return 0;
}
