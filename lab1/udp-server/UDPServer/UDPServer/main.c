/*
 ** listener.c -- a datagram sockets "server" demo
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>

#define MYPORT "10012"    // the port users will be connecting to

#define MAXBUFLEN 100

// get sockaddr, IPv4 or IPv6:
void *get_in_addr(struct sockaddr *socket_address)
{
    
    if (socket_address->sa_family == AF_INET) {
        return &(((struct sockaddr_in*)socket_address)->sin_addr);
    }
    
    return &(((struct sockaddr_in6*)socket_address)->sin6_addr);
}

int readWord(char * buffer, int * position)
{
    int x1 = (buffer[*position] & 255) << 24;
    * position = *position + 1;
    int x2 = (buffer[*position] & 255) << 16;
    *position = *position + 1;
    int x3 = (buffer[*position] & 255) << 8;
    *position = *position + 1;
    int x4 = (buffer[*position] & 255);
    *position = *position + 1;
    int x = x1 + x2 + x3 + x4;
    return x;
}

int readShort(char * buffer, int * position)
{
    int x1 = (buffer[*position] & 255) << 8;
    *position = *position + 1;
    int x2 = (buffer[*position] & 255);
    *position = *position + 1;
    int x = x1 + x2;
    return x;
}

int readByte(char * buffer, int * position)
{
    int x = (buffer[*position] & 255);
    * position = *position + 1;
    return x;
}

void *putWord(char * buffer, int value, int * position)
{
    buffer[*position] = (char) value >> 24;
    * position = *position + 1;
    buffer[*position] = (char) value >> 16;
    * position = *position + 1;
    buffer[*position] = (char) value >> 8;
    * position = *position + 1;
    buffer[*position] = (char) value;
    * position = *position + 1;
    return 0;
}
void *putShort(char * buffer, int value, int * position)
{
    buffer[*position] = (char) value >> 8;
    * position = *position + 1;
    buffer[*position] = (char) value;
    * position = *position + 1;
    return 0;
}
void *putByte(char * buffer, int value, int * position)
{
    buffer[*position] = (char) value;
    * position = *position + 1;
    return 0;
}


int main(void)
{
    int sockfd;
    struct addrinfo hints, *servinfo, *p;
    int rv;
    int number_of_bytes;
    struct sockaddr_storage their_addr;
    char buffer[MAXBUFLEN];
    socklen_t address_length;
    char s[INET6_ADDRSTRLEN];
    
    memset(&hints, 0, sizeof hints);
    hints.ai_family = AF_UNSPEC; // set to AF_INET to force IPv4
    hints.ai_socktype = SOCK_DGRAM;
    hints.ai_flags = AI_PASSIVE; // use my IP
    
    if ((rv = getaddrinfo(NULL, MYPORT, &hints, &servinfo)) != 0) {
        fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
        return 1;
    }
    
    // loop through all the results and bind to the first we can
    for(p = servinfo; p != NULL; p = p->ai_next) {
        if ((sockfd = socket(p->ai_family, p->ai_socktype,
                             p->ai_protocol)) == -1) {
            perror("listener: socket");
            continue;
        }
        
        if (bind(sockfd, p->ai_addr, p->ai_addrlen) == -1) {
            close(sockfd);
            perror("listener: bind");
            continue;
        }
        
        break;
    }
    
    if (p == NULL) {
        fprintf(stderr, "listener: failed to bind socket\n");
        return 2;
    }
    
    freeaddrinfo(servinfo);
    
    printf("listener: waiting to recvfrom...\n");
    
    while (1) {
        
        
        address_length = sizeof their_addr;
        if ((number_of_bytes = recvfrom(sockfd, buffer, MAXBUFLEN-1 , 0,
                                 (struct sockaddr *)&their_addr, &address_length)) == -1) {
            perror("recvfrom");
            exit(1);
        }
        printf("listener: got packet from %s\n",
               inet_ntop(their_addr.ss_family,
                         get_in_addr((struct sockaddr *)&their_addr),
                         s, sizeof s));
        printf("listener: packet is %d bytes long\n", number_of_bytes);
        buffer[number_of_bytes] = '\0';
        printf("listener: packet contains \"%s\"\n", buffer);
        
        

        int *position = 0;
        int total_message_length = readByte(buffer, &position);
        int request_id = readByte(buffer, &position);
        int op = readByte(buffer, &position);
        int number_of_ops = readByte(buffer, &position);
        int op1 = readShort(buffer, &position);
        int op2 = 0;
        if (number_of_ops > 1) {
            op2 = readShort(buffer, &position);
        }

        printf("TML: %d \nRequestId: %d \n opcode: %d \n number of ops: %d \n op1: %d \n op2: %d",
               total_message_length,
               request_id,
               op,
               number_of_ops,
               op1,
               op2);
    
        int result;
        switch(op) {
            case 0: // Add +
                result = op1 + op2;
                break;
            case 1: // Subtract -
                result = op1 - op2;
                break;
            case 2: // Or |
                result = op1 | op2;
                break;
            case 3: // And &
                result = op1 & op2;
                break;
            case 4: // Left shift <<
                result = op1 << op2;
                break;
            case 5: // Right shift >>
                result = op1 >> op2;
                break;
            case 6: // Not ~
                result = ~op1;
                break;
            default:
                break;
        }
        printf("\nResult: %d", result);
        char *result_buffer[MAXBUFLEN];

        int *result_position = 0;
        putByte(result_buffer, total_message_length, &result_position); // put Total Message Length
        putByte(result_buffer, request_id, &result_position); // put request id
        putByte(result_buffer, 0, &result_position); // put error code
        putWord(result_buffer, result, &result_position); // put result
        

        // send buffer
        long send_to_error;
        if ((send_to_error = sendto(sockfd, result_buffer, MAXBUFLEN - 1, 0, (struct sockaddr *)&their_addr, address_length)) == -1) {
            perror("sendto");
            exit(1);
        }
    }
    
    //close(sockfd);
    
    return 0;
}
