# P2P Encrypted File Sharing System

## Table of Contents
- [Introduction](#introduction)
- [Getting Started](#getting-started)
- [Technical Implementation](#technical-implementation)
- [Application Overflow](#Application-overflow)
- [Usage](#usage)
- [References](#references)

## Introduction
This project is about Peer-to-Peer (P2P) encrypted file sharing system. P2P file-sharing technology enables direct file sharing among users, eliminating the need for a central server. This approach distributes network resources across all nodes, enhancing efficiency and reducing reliance on a single failure point. So that users can create, delete, modify and restore the files in this  distributed file system. Built with Java, it utilizes a P2P architecture, allowing users to directly share files with one another without relying on a central server.

## Getting Started
### Prerequisites
* Java Development Kit (JDK) 11+
* Apache Maven

### Installing and Running
1. Clone the repository to your local machine: `git clone https://github.com/smaddin1/secure_file_sharing_system .
2. Navigate to the project directory: `cd secure_file_sharing_system`
3. Compile the project using Maven: `mvn clean install`
4. Navigate to the target directory: `cd target`
5. Run the application: `java -jar file-sharing-1.0-SNAPSHOT.jar`

## Technical Implementation
This implementation has the design, decisions, technologies, tools, libraries, and algorithms.

## Programming Languages and libraries
We have chosen java as the programming language for this project. Java is an object-oriented programming language which is having good compatibility. It is selected for its capabilities in multi-threading and also the security. Java provides good libraries such as java.net and java.nio classifies client-server communication, DataStream transfers and protocol implementation

### Network communication
This includes Sockets, connection management, messaging and protocols, Encryption and security, Error Handling and recovery.
Sockets are endpoints for network data transmission. It uses both client and server sockets for the data transmission.so that it can manage for incoming connections and also it can initiate connections. 
Connection management manages the connections accordingly in peer functions as both server and client.


### Encryption and security
It contains Secure communication in java libraries such as JCA and JSSE. To secure this communication it uses different encryption techniques such as AES file encryption and RSA keys this key is used to exchange with the AES key.

### Key Management
It is centralized through a service called key Distributed system. It is responsible for creating, distributing and managing all the cryptographic keys. The KDS generate key pairs for each peer in the network. And also the KDS issues a digital certificate. When a peer needs to establish a communication with other peer it requests public key from the KDS. This KDS maintains a secure database while peers store their private keys in a secure manner. The KDS provides encryption and decryption process upon the request of the peers

### File System Operations
The File system operations plays a crucial role in enabling the.users to manage and access their own files of each peers. This file system operations include File File Creation, File Deletion, File reading, File writing with operation Transformation. Files can be shared between the peers.

### Malicious Activity Detection Service
In peer-to-peer (P2P) file-sharing system, ensuring the integrity of files is paramount. One effective method to achieve this is through the use of cryptographic hashing. This section delves into the technical details of implementing file integrity checks using cryptographic hashing in your P2P file-sharing project. Cryptographic hashing involves generating a unique, fixed-size hash value (or hash code) from file data. This hash acts as a digital fingerprint of the file's contents. Any alteration in the file, however minor, results in a significantly different hash, making it an effective tool for detecting changes or corruptions in files. It contains Hash function selection, generating Hashes, Hash verification, Handling Hash mis matches and Hash updates.


### Application Work flow
In a standard P2P file-sharing system, clients interact with either a central server or a network of distributed servers for various functions, including peer discovery, key exchange, and communication coordination.


## Usage
Launch the application, it generates public key and private key for each peer in the system. It provides the security for the user to have better communication within the network.

The secure file sharing system supports the following commands:
- `keygen --keyLength`: Generate a new key with a specified key length.
- `rm --fileName`: Remove the file with the given name.
- `ls`: List the contents of the current directory.

- `mkdir --directoryName --accessList`: Create a new directory with the given name and access list.
- `restore --fileName`: Restore the file with the given name.
- `touch --fileName --accessList`: Create a new file with the given name and access list.
- `chmod --[directoryName|fileName] --updatedAccessList`: Change the access permissions of a directory or file to the updated access list.
- `cd --directoryName`: Change the current directory to the given directory name.

Users can create, read, update, delete(crud) and restore files on their system, it is available in the peer to peer network. The system ensures  through the use of strong encryption algorithms and mechanisms to detect unauthorized usage.

## References

* Schollmeier, R. (2001). A Definition of Peer-to-Peer Networking for the Classification of Peer-to-Peer Architectures and Applications. Proceedings of the First International Conference on Peer-to-Peer Computing, 101-102. This paper provides a foundational definition of P2P networking and its applications.
* Lua, E. K., Crowcroft, J., Pias, M., Sharma, R., & Lim, S. (2005). A survey and comparison of peer-to-peer overlay network schemes. IEEE Communications Surveys & Tutorials, 7(2), 72-93. This survey offers a comprehensive comparison of various P2P overlay network schemes.
* Ripeanu, M., Foster, I., & Iamnitchi, A. (2002). Mapping the Gnutella Network: Properties of Large-Scale Peer-to-Peer Systems and Implications for System Design. IEEE Internet Computing Journal, 6(1), 50-57. This study maps the Gnutella network, providing insights into large-scale P2P system design.

This README file explains only the basic usage of this project. To have better understanding of this project refer to the project report.


