#pragma once

#include "../include/ConnectionHandler.h"

// TODO: implement the STOMP protocol
class StompProtocol
{
private:
    std::unique_ptr<ConnectionHandler> connectionHandler = nullptr; // Manages the connection handler
    bool terminate = false; // Tracks whether the client should terminate
    // bool userConnected = false;

    int receipt_Id = 0; // Increasing receipt id for unique receipts
    int subscription_Id = 0; // Increasing sub iD for unqiue subs

    int logout_Id = -1; // Indicator if LOGOUT FRAME sent(will be equal to the receipt-id sent)
    // int sub_receipt_sent = -1;


public:
    // Processes keyboard input from the user
    void processKeyboardInput(const std::string& input);

    // Processes frames received from the server
    void process(const std::string& frame);

    std::string epochToDate(int epoch);

    // Checks if the client should terminate
    bool shouldTerminate() const;

    // Checks if the client is connected to the server
    bool isConnected() const;

    bool isUserConnected() const;

    // Retrieves the connection handler
    ConnectionHandler* getConnectionHandler() const;

    // Extracts the receiptID from the RECEIPT
    std::string extractReceiptId(const std::string& frame);

    std::string trim(const std::string &str);

    void processMessageFrame(const std::string &frame);
    void processSummaryCommand(const std::string &channel_name, const std::string &user, const std::string &file);
};
