/*
 ** client.c -- a stream socket client demo
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <netdb.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <time.h>

#include <arpa/inet.h>

#define PORT "10010" // the port client will be connecting to

#define MAXDATASIZE 100 // max number of bytes we can get at once

// get sockaddr, IPv4 or IPv6:
void *get_in_addr(struct sockaddr *sa)
{
    if (sa->sa_family == AF_INET) {
        return &(((struct sockaddr_in*)sa)->sin_addr);
    }
    
    return &(((struct sockaddr_in6*)sa)->sin6_addr);
}

int readWord(char * buffer, int * position)
{
    int x1 = buffer[*position]<< 24 & 255;
    * position = *position + 1;
    int x2 = buffer[*position]<<16 & 255;
    *position = *position + 1;
    int x3 = buffer[*position]<<8 & 255;
    *position = *position + 1;
    int x4 = buffer[*position] & 255;
    *position = *position + 1;
    int x = x1 + x2 + x3 + x4;
    return x;
}

int readShort(char * buffer, int * position)
{
    int x1 = buffer[*position]<< 8 & 255;
    * position = *position + 1;
    int x2 = buffer[*position] & 255;
    *position = *position + 1;
    int x = x1 + x2;
    return x;
}

int readByte(char * buffer, int * position)
{
    int x = buffer[*position] & 255;
    * position = *position + 1;
    return x;
}

void *putWord(char * buffer, int value, int * position)
{
    buffer[*position] = (char) value >> 24;
    printf("%s", &buffer[*position]);
    * position = *position + 1;
    buffer[*position] = (char) value >> 16;
    printf("%s", &buffer[*position]);
    * position = *position + 1;
    buffer[*position] = (char) value >> 8;
    printf("%s", &buffer[*position]);
    * position = *position + 1;
    buffer[*position] = (char) value;
    printf("%s", &buffer[*position]);
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
    buffer[*position] = (char) value >> 8;
    * position = *position + 1;
    return 0;
}


int main(int argc, char *argv[])
{
    int sockfd, numbytes;
    char buf[MAXDATASIZE];
    struct addrinfo hints, *servinfo, *p;
    int rv;
    char s[INET6_ADDRSTRLEN];
    
    if (argc != 3) {
        fprintf(stderr,"usage: client hostname\n");
        exit(1);
    }
    
    memset(&hints, 0, sizeof hints);
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    
    if ((rv = getaddrinfo(argv[2], PORT, &hints, &servinfo)) != 0) {
        fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
        return 1;
    }
    
    
    // Read input
    int opcode;
    int op1;
    int op2;
    int numberOfOps = 1;
    printf("Enter opcode: ");
    opcode = getchar();
    printf("Enter first operand: ");
    op1 = getchar();
    
    if (opcode == 0 || opcode == 1) {
        printf("Enter second operand: ");
        op2 = getchar();
        numberOfOps = 2;
    }
    
    
    // loop through all the results and connect to the first we can
    for(p = servinfo; p != NULL; p = p->ai_next) {
        if ((sockfd = socket(p->ai_family, p->ai_socktype,
                             p->ai_protocol)) == -1) {
            perror("client: socket");
            continue;
        }
        
        if (connect(sockfd, p->ai_addr, p->ai_addrlen) == -1) {
            close(sockfd);
            perror("client: connect");
            continue;
        }
        
        break;
    }
    
    if (p == NULL) {
        fprintf(stderr, "client: failed to connect\n");
        return 2;
    }
    
    inet_ntop(p->ai_family, get_in_addr((struct sockaddr *)p->ai_addr),
              s, sizeof s);
    printf("client: connecting to %s\n", s);
    
    freeaddrinfo(servinfo); // all done with this structure
    

    char buffer[MAXDATASIZE];
    int *position = 0;
    putByte(buffer, 0, &position); // TML
    putByte(buffer, 0, &position); // requestID
    putByte(buffer, opcode, &position); // opCode
    putByte(buffer, numberOfOps, &position); // number of operands
    putShort(buffer, 0, &position); // op1
    if (numberOfOps > 1) {
        putShort(buffer, 0, &position); // op2
    }

    clock_t before = clock();

    if ((numbytes = sendto(sockfd, buffer, strlen(argv[3]), 0,
                           p->ai_addr, p->ai_addrlen)) == -1) {
        perror("talker: sendto");
        exit(1);
    }
    
    if ((numbytes = recv(sockfd, buf, MAXDATASIZE-1, 0)) == -1) {
        perror("recv");
        exit(1);
    }
    
    clock_t difference = clock() - before;
    int msec = difference / CLOCKS_PER_SEC;
    printf("Response time: %dms",difference);
    
    
    buf[numbytes] = '\0';
    
    printf("client: received '%s'\n",buf);
    
    close(sockfd);
    
    return 0;
}

