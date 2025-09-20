# Assignment 3 – SPL Course (Client-Server with STOMP Protocol)

This repository contains my **third assignment** for the  
**Systems Programming Lab (SPL)** course.  

The project implements a **client-server architecture** using the STOMP protocol:  
- **Client:** C++ implementation that connects to the server, sends and receives events, and processes data.  
- **Server:** Java (Maven-based) STOMP server with a multithreaded Reactor design.  

---

## Project Overview
- **Client:** C++ (makefile build)  
- **Server:** Java (Maven, Reactor pattern)  
- **Protocol:** STOMP 1.2 (text-based messaging protocol)  
- **Data:** Input/output JSON event files  

---

## Key Concepts & Principles Learned
1. **Client-Server Architecture**  
   - Designed communication between distributed components.  

2. **Networking Protocols**  
   - Implemented STOMP protocol encoder/decoder.  
   - Handled message framing, parsing, and dispatch.  

3. **Concurrency & Multithreading**  
   - Server built on Reactor design with thread pool.  
   - Client handles multiple events concurrently.  

4. **Cross-Language Integration**  
   - Client in C++ communicates seamlessly with a Java server.  

5. **Software Engineering Practices**  
   - Use of Maven for server build.  
   - Modular C++ project structure with headers and sources.  
   - Testing with JSON input/output scenarios.  

---

## Build & Run

### Build the Client (C++)
```bash
cd client
make
./bin/StompEMIClient <host> <port>
