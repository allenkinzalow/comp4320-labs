import socket
import sys
import struct
import thread


class Buffer:
    def __init__(self, data=[0] * 100):
        self.position = 0
        self.buffer = bytearray(data)

    def readStr(self):
       # Put code here
        subBuffer = bytearray()
        for i in range(0, 63):
            subBuffer.append(self.buffer[self.position + i])
            self.movePos()
        return str(subBuffer)

    def readWord(self):
        x1 = (self.buffer[self.position] & 255) << 24
        self.movePos()
        x2 = (self.buffer[self.position] & 255) << 16
        self.position = self.position + 1
        x3 = (self.buffer[self.position] & 255) << 8
        self.movePos()
        x4 = (self.buffer[self.position] & 255)
        self.movePos()
        return x1 + x2 + x3 + x4

    def readByte(self):
        x = self.buffer[self.position] & 255
        self.movePos()
        return x

    def putWord(self, data):
        x1 = data >> 24
        self.buffer.append(x1)
        self.movePos()
        x2 = (data >> 16) & 0x000000ff
        self.buffer.append(x2)
        self.movePos()
        x3 = (data >> 8) & 0x000000ff
        self.buffer.append(x3)
        self.movePos()
        x4 = data & 0x000000ff
        self.buffer.append(x4)
        self.movePos()

    def putByte(self, data):
        x = data & 0x000000ff
        self.buffer.append(x)
        self.movePos()

    def putStr(self, data):
        messageBuffer = bytearray(data)
        actualBuffer = bytearray(64)
        messageLength = len(messageBuffer) if len(
            messageBuffer) <= 64 else len(actualBuffer)
        for i in range(0, messageLength - 1):
            actualBuffer[i] = messageBuffer[i]
        for b in actualBuffer:
            self.buffer[self.position] = b
            self.movePos()

    def movePos(self, amount=1):
        self.position = self.position + amount


class Response:
    def __init__(self, response):
        self.response = response
        self.buffer = Buffer(self.response)
        self.readResponse()

    def readResponse(self):
        self.gid = self.buffer.readByte()
        self.magic = self.buffer.readWord()
        self.rid = self.buffer.readByte()
        self.nextSlaveIP = self.buffer.readWord()
        self.dottedIP = socket.inet_ntoa(struct.pack('>L', self.nextSlaveIP))

    def printResponse(self):
        print "Group ID of master: ", self.gid
        print "My ring ID: ", self.rid
        print "IP Address:  ", self.dottedIP


class MessageResponse:
    def __init__(self, response):
        self.response = response
        self.buffer = Buffer(self.response)
        self.readResponse()

    def readResponse(self):
        self.gid = self.buffer.readByte()
        self.magic = self.buffer.readWord()
        self.ttl = self.buffer.readByte()
        self.ridDest = self.buffer.readByte()
        self.ridSrc = self.buffer.readByte()
        self.message = self.buffer.readStr()
        self.checkSum = self.buffer.readByte()


if (len(sys.argv) != 3):
    print "Error: Incorrect arguments. Use format: Slave MasterHostname MasterPort#"
    sys.exit()

hostname = sys.argv[1]
port = int(sys.argv[2])
groupID = 2
magicNumber = '0x4a6f7921'

buffer = Buffer()

client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

client.connect((hostname, port))

buffer.putByte(groupID)
buffer.putWord(int(magicNumber, 16))

# send request to server
client.send(buffer.buffer)

# Receive response from server
response = client.recv(4096)
resp = Response(response)
resp.printResponse()
udpPort = 10010 + resp.gid * 5 + resp.rid
print udpPort


def listenForMessages(threadName, delay, host, port, myRid, nextSlaveIP):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server_address = (host, port)
    sock.bind(server_address)
    while True:
        response = sock.recvfrom(4096)
        messageResponse = MessageResponse(response)
        if (messageResponse.ridDest == myRid):
            print messageResponse.message
        elif (messageResponse.ttl > 1):
            next_address = (nextSlaveIP, port)
            sock.sendto(response, port)


try:
    thread.start_new_thread(
        listenForMessages,
        ("Thread-2", 4, hostname, udpPort, resp.rid, resp.nextSlaveIP))
except:
    print "Error: unable to start thread"

while (True):
    inputRingID = raw_input("\nEnter Ring ID: ")
    ringID = int(inputRingID)
    message = raw_input("Message: ")

    buffer = Buffer()
    buffer.putByte(ringID)
    buffer.putStr(message)

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server_address = (hostname, port)
    sent = sock.sendto(message, server_address)
