#include "StompProtocol.h"
#include <iostream>
#include <sstream>
#include <string>
#include <memory>
#include "ConnectionHandler.h"
#include <unordered_map>
#include <event.h>
#include <fstream>
#include <iomanip>
#include <ctime>


std::string channelName;
std::unordered_map<std::string, int> channels_map; // Key: Channel joined, Value: channel id
std::string loggedUser;
// Outer key: user, Inner key: channel, Value: list of events for that user and channel
std::unordered_map<std::string, std::unordered_map<std::string, std::vector<Event>>> events_map; 

void StompProtocol::processKeyboardInput(const std::string& input) {
    std::istringstream iss(input);
    std::string command;
    std::string frameString;
    iss >> command;

     if (command == "login")
    {
        std::string hostPort, username, password;
        iss >> hostPort >> username >> password;
        if (username.empty() || password.empty() || hostPort.empty() )
        {
            std::cerr<< "Invalid syntax, please use login {host:port} {username} {password}\n";
            return;
        }

        if (connectionHandler)
        {
            std::cerr<< "The client is already logged in, log out before trying again. \n";
            return;
        }

        size_t colonPos = hostPort.find(':');
        if (colonPos == std::string::npos) {
            std::cerr << "Invalid host:port format.\n";
            return;
        }

        std::string host = hostPort.substr(0, colonPos);
        short port = std::stoi(hostPort.substr(colonPos + 1));

        // Create and connect the ConnectionHandler
        connectionHandler = std::unique_ptr<ConnectionHandler>(new ConnectionHandler(host, port));
        if (!connectionHandler->connect()) {
            std::cerr << "Could not connect to server.\n";
            connectionHandler.reset();
            return;
        }

        // Send the CONNECT frame
        std::ostringstream frame;
        frame << "CONNECT\n"
              << "accept-version:1.2\n"
              << "host:stomp.cs.bgu.ac.il\n"
              << "login:" << username << "\n"
              << "passcode:" << password << "\n" 
              << "\n";
        
        frameString = frame.str();

        if (!connectionHandler->sendLine(frameString)) {
            std::cerr << "Failed to send CONNECT frame.\n";
        } 
        else 
        {
            loggedUser = username;
            std::cout << "Login request sent. Waiting for server acknowledgment...\n";
        }
    } 
    else if (command == "join") 
    {
        if (!connectionHandler) {
            std::cerr << "Not logged in.\n";
            return;
        }
        iss >> channelName;

        if (channelName.empty()) {
            std::cerr << "Invalid join command format.\nPlease use join {channel_name} \n";
            return;
        }

        std::ostringstream frame;
        int channel_id = -1; // channel_id == -1 means didn't tried to join
        try
        {
            channel_id = channels_map.at(channelName); // Throws std::out_of_range if not mapped already
            std::cout<<"Multiple 'join' , locating the channel_id " << channel_id << "\n";   
        } 
        catch (const std::out_of_range& e)
        {
            channel_id = subscription_Id++;
            // std::cout<< "Creating new channelID " << channel_id << "\n";   
        }
        
        
        frame << "SUBSCRIBE\n"
                << "destination:/" << channelName << "\n" 
                << "id:" << channel_id << "\n"
                << "receipt:" << receipt_Id++ << "\n" 
                << "\n";

        frameString = frame.str();

        if (!connectionHandler->sendLine(frameString)) {
            std::cerr << "Failed to send SUBSCRIBE frame.\n";
        }
        else
        {
            std::cout << "Joined channel " << channelName << std::endl; // Not waiting for receipt ack
            channels_map.insert({channelName,channel_id});
        }
    } 
    else if (command == "exit") {
        if (!connectionHandler) {
            std::cerr << "Not logged in.\n";
            return;
        }

        iss >> channelName;

        if (channelName.empty()) {
            std::cerr << "Invalid exit command format, use: exit {channelName} \n";
            return;
        }

        std::ostringstream frame;
        try
        {
            int channel_id = channels_map.at(channelName); // Throws std::out_of_range
            frame << "UNSUBSCRIBE\n"
                << "id:" << channel_id << "\n"
                << "receipt:" << receipt_Id++ << "\n" 
                << "\n";

            frameString = frame.str();
            if (!connectionHandler->sendLine(frameString)) {
                std::cerr << "Failed to send UNSUBSCRIBE frame.\n";
            } else { 
                std::cout << "Exited channel " << channelName << ".\n";
                channels_map.erase(channelName);
            }
            
        } 
        catch (const std::out_of_range& e)
        {
            std::cout << "Can't unsubscribe from " << channelName << ", you are not registerd to it" << std::endl;
        }
    
    } 
    else if (command == "logout") {
        if (!connectionHandler) {
            std::cerr << "Not logged in.\n";
            return;
        }

        std::ostringstream frame;
        logout_Id = receipt_Id; // To identify the RECEIPT that ack the logout
        frame << "DISCONNECT\n"
                << "receipt:" << receipt_Id++ << "\n" 
                << "\n";
        frameString = frame.str();

        if (!connectionHandler->sendLine(frameString)) {
            std::cerr << "Failed to send DISCONNECT frame.\n";
        } else {
            std::cout << "Logout frame sent. Waiting for server receipt... \n";
        }
    }
    else if (command == "report")
    {
        if (!connectionHandler) {
            std::cerr << "Not logged in.\n";
            return;
        }

        std::string fileName;
        iss >> fileName;

        if (fileName.empty()) {
            std::cerr << "Invalid report command format: report {filePath} \n";
            return;
        }

        // Check if the file exists and can be opened
        std::ifstream file(fileName);
        if (!file.is_open()) {
            std::cerr << "Error: Unable to open file '" << fileName << "'. Please ensure the file exists and is accessible.\n";
            return;
        }
        file.close(); // Close the file after checking

        // Parse events from the file
        try {
            names_and_events parsedData = parseEventsFile(fileName);
            channelName = parsedData.channel_name;
            try
            {
                channels_map.at(channelName); // Throws std::out_of_range if not mapped already
            } 
            catch (const std::out_of_range& e)
            {
                std::cout<<"Can't report about channel that you are not subscribed to \n";   
                return;  
            }
            std::vector<Event> events = parsedData.events;

            if (events.empty()) {
                std::cerr << "No events found in the file '" << fileName << "'. Nothing to report.\n";
                return;
            }

            // Sort events by date_time
            std::sort(events.begin(), events.end(), [](const Event& a, const Event& b) {
                return a.get_date_time() < b.get_date_time();
            });

            // Send each event
            for (const Event& event : events) {
                std::ostringstream frame;
                frame << "SEND\n"
                    << "destination:/" << channelName << "\n"
                    << "\n"
                    << "user:" << loggedUser << "\n"
                    << "city:" << event.get_city() << "\n"
                    << "event name:" << event.get_name() << "\n"
                    << "date time:" << event.get_date_time() << "\n"
                    << "general information:\n";

                for (const auto& pair : event.get_general_information()) {
                    frame << "\t" << pair.first << ": " << pair.second << "\n";
                }

                frame << "description:\n" << event.get_description() << "\n";

                // Send the frame
                std::string frameString = frame.str();


                if (!connectionHandler->sendLine(frameString)) {
                    std::cerr << "Failed to send event: " << event.get_name() << "\n";
                } else {
                    std::cout << "Event '" << event.get_name() << "' sent successfully.\n";
                    // Will add the events to the hashmap when receiving the MESSAGE
                }
            }

        } catch (const std::exception& ex) {
            std::cerr << "Failed to process report command: " << ex.what() << "\n";
        }
    }

    else if (command == "summary") // summary {channel_name} {user} {file}
    {
        if (!connectionHandler) {
            std::cerr << "Not logged in.\n";
            return;
        }

        std::string summary_channel_name;
        std::string summary_user;
        std::string summary_file_path;
        iss >> summary_channel_name >> summary_user >> summary_file_path;

        if (summary_file_path.empty() || summary_user.empty() || summary_channel_name.empty() )
         {
            std::cerr << "Invalid summary command format.\nsummary {channel_name} {user} {file}\n";
            return;
        }
        this->processSummaryCommand(summary_channel_name, summary_user, summary_file_path);

    }
    else
    {
        std::cerr << "Unknown command: " << command << "\n";
    }
}

void StompProtocol::process(const std::string& frame) {
    if (frame.find("ERROR") != std::string::npos) // Handle ERROR receipts
    { 
        std::cerr << "ERROR FROM THE SERVER: \n " << frame << "\nLogging out and closing the socket\n";
        logout_Id = -1;
        connectionHandler.reset();
        channels_map.clear(); // Deletes every subscribed channels
        events_map.clear(); // Deletes every information for summary
        loggedUser = "";
    } 
    else if (frame.find("CONNECTED") != std::string::npos)
    { 
        std::cout << "Login successful \n";
    }
    else if (frame.find("MESSAGE") != std::string::npos) 
    {
        this->processMessageFrame(frame);
    }
     else if (frame.find("RECEIPT") != std::string::npos) {
        // Extract and compare the receipt-id
        // They said only to take care about LOGOUT RECEIPT
        if (logout_Id != -1 ) // lougout_id != -1, means that logout request been sent
        {
            std::string receiptIdFromFrame = extractReceiptId(frame);
            if (!receiptIdFromFrame.empty() && std::to_string(logout_Id) == receiptIdFromFrame) {
                logout_Id = -1;
                connectionHandler.reset();
                channels_map.clear(); // Deletes every subscribed channels
                events_map.clear(); // Deletes every information for summary
                loggedUser = "";
                std::cout << "Logged out, received Receipt ACK \n";

            }
            //  else {
            //     std::cerr << "Keep waiting for logout ack, Receipt ID does not match. Expected: " << logout_Id << ", Got: " << receiptIdFromFrame << "\n";
            // }
        }
    }
    else
    {
        std::cout << "Unknown frame received: " << frame << "\n";
    }
}

void StompProtocol::processMessageFrame(const std::string& frame)
{
    std::istringstream iss(frame);
    std::string line;
    std::string user, channel, city, name, description;
    int date_time = 0;
    std::map<std::string, std::string> general_information;

    bool in_general_info = false, in_description = false;
    while (std::getline(iss, line)) {
        // Parse key-value pairs
        if (line.find(':') != std::string::npos) {
            auto colon_pos = line.find(':');
            std::string key = line.substr(0, colon_pos);
            std::string value = line.substr(colon_pos + 1);

            if (key == "destination") {
                if (value.length() > 1 && value.at(0) == '/')
                {
                    channel = value.substr(1); // Removes leading '/'
                }
                else {
                    channel = value;
                }
                
            } else if (key == "user") {
                user = value;
            } else if (key == "city") {
                city = value;
            } else if (key == "event name") {
                name = value;
            } else if (key == "date time") {
                date_time = std::stoi(value);
            } else if (key == "general information") {
                in_general_info = true;
            } else if (key == "description") {
                in_description = true;
            } else if (in_general_info) {
                general_information[key] = value;
            }
        } else if (in_description) {
            description += line + "\n";
        }
    }

    Event event(channel, city, name, date_time, description, general_information);
    event.setEventOwnerUser(user);

    // Update the summary map
    if (events_map[user].find(channel) == events_map[user].end()) {
        events_map[user][channel] = std::vector<Event>();
    }
    events_map[user][channel].push_back(event);

    // std::cout << "Processed event from user: " << user << " in channel: " << channel << "\n";
}

void StompProtocol::processSummaryCommand(const std::string& channel_name, const std::string& user, const std::string& file) {
    // Check if user and channel exist in the map
    if (events_map.find(user) == events_map.end() || events_map[user].find(channel_name) == events_map[user].end()) {
        std::cerr << "No events found for user '" << user << "' in channel '" << channel_name << "'.\n";
        return;
    }

    const auto& events = events_map[user][channel_name];

    // Sort events by date_time, then by event_name lexicographically
    std::vector<Event> sortedEvents = events;
    std::sort(sortedEvents.begin(), sortedEvents.end(), [](const Event& a, const Event& b) {
        if (a.get_date_time() != b.get_date_time()) {
            return a.get_date_time() < b.get_date_time();
        }
        return a.get_name() < b.get_name();
    });

    // Statistics for summary
    int totalReports = sortedEvents.size();
    int activeCount = 0;
    int forcesArrivalCount = 0;
    for (const auto& event : sortedEvents)
    {
        const auto& general_info = event.get_general_information();
        for (const auto& pair : general_info) {
            std::string key = trim(pair.first);
            std::string value = trim(pair.second);

            if (key == "active" && value == "true") {
                activeCount++;
            }
            if (key == "forces_arrival_at_scene" && value == "true") {
                forcesArrivalCount++;
            }
        }
    }

    // Open the file for writing
    std::ofstream outFile(file);
    if (!outFile.is_open()) {
        std::cerr << "Error: Unable to open file '" << file << "' for writing.\n";
        return;
    }

    // Write summary to file
    outFile << "Channel " << channel_name << "\n";
    outFile << "Stats:\n";
    outFile << "Total: " << totalReports << "\n";
    outFile << "active: " << activeCount << "\n";
    outFile << "forces arrival at scene: " << forcesArrivalCount << "\n";
    outFile << "\nEvent Reports:\n";

    for (size_t i = 0; i < sortedEvents.size(); ++i) {
        const auto& event = sortedEvents[i];
        std::string description = event.get_description();
        if (description.size() > 27) {
            description = description.substr(0, 27) + "...";
        }

        outFile << "\nReport_" << (i + 1) << ":\n";
        outFile << "\tcity: " << event.get_city() << "\n";
        outFile << "\tdate time: " << epochToDate(event.get_date_time()) << "\n";
        outFile << "\tevent name: " << event.get_name() << "\n";
        outFile << "\tsummary: " << description << "\n";
    }

    outFile.close();
    std::cout << "Summary written to " << file << " successfully.\n";
}

std::string StompProtocol::epochToDate(int epoch) 
{
    std::time_t time = static_cast<std::time_t>(epoch);
    std::tm* tm = std::localtime(&time);

    char buffer[20]; // Enough space for "dd/mm/yy hh:mm\0"
    std::strftime(buffer, sizeof(buffer), "%d/%m/%y %H:%M", tm);
    return std::string(buffer);
}


bool StompProtocol::shouldTerminate() const {
    return terminate;
}

bool StompProtocol::isConnected() const {
    return connectionHandler != nullptr;
}

ConnectionHandler* StompProtocol::getConnectionHandler() const {
    return connectionHandler.get();
}

std::string StompProtocol::extractReceiptId(const std::string& frame) {
    // Find the position of "receipt-id:"
    std::size_t pos = frame.find("receipt-id:");
    if (pos != std::string::npos) {
        // Find the end of the line after "receipt-id:"
        std::size_t start = pos + std::string("receipt-id:").length();
        std::size_t end = frame.find('\n', start); // Look for the end of the line or delimiter
        if (end == std::string::npos) {
            end = frame.length(); // If no newline, take until the end of the string
        }
        return frame.substr(start, end - start);
    }
    return ""; // Return empty if receipt-id is not found
}

std::string StompProtocol::trim(const std::string& str) {
    size_t first = str.find_first_not_of(" \t");
    size_t last = str.find_last_not_of(" \t");
    return (first == std::string::npos || last == std::string::npos) ? "" : str.substr(first, last - first + 1);
}



