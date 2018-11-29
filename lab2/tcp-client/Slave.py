import socket
import sys
import struct
import thread

class Buffer:
    def __init__(self, data=[]):
        self.position = 0
        self.buffer = bytearray(data)

    def readStr(self, endIndex):
        subBuffer = bytearray()
        end = endIndex - self.position
        for i in range(0, end):
            subBuffer.append(self.buffer[self.position + i])
        self.movePos(end)
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
        x1 = (data >> 24) & 0x000000ff
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
        if len(data) > 64:
            data = data[:64]
        messageBuffer = bytearray(data)
        for b in messageBuffer:
            self.buffer.append(b)
            self.movePos()

    def movePos(self, amount=1):
        self.position = self.position + amount

    def printBuffer(self):
        for b in self.buffer:
            print b


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
        self.message = self.buffer.readStr(self.getMessageEnd())
        self.checkSum = self.buffer.readByte()

    def getMessageEnd(self):
        header = self.buffer.buffer
        end = len(header) - 1
        return end

    def computeChecksum(self):
        buffer = self.toBuffer()
        #buffer.printBuffer()
        header_size = len(buffer.buffer) - 1

        checksum = 0
        for i,b in enumerate(buffer.buffer):
            if i == header_size:
                continue
            checksum = checksum + (b & 0x000000ff)
            overflow = (checksum >> 8) & 0x000000ff
            if overflow > 0:
                checksum &= 0x000000ff
                checksum = checksum + overflow
        checksum = (~checksum) & 0x000000ff
        return checksum & 0x000000ff
    
    def toBuffer(self):
        buffer = Buffer(data=[])
        buffer.putByte(self.gid)
        buffer.putWord(self.magic)
        buffer.putByte(self.ttl)
        buffer.putByte(self.ridDest)
        buffer.putByte(self.ridSrc)
        buffer.putStr(self.message)
        buffer.putByte(self.checkSum)
        return buffer

class Message:
    def __init__(self, response, destRid, message):
        self.response = response
        self.destRid = destRid
        self.message = message
        self.readResponse()

    def readResponse(self):
        self.gid = self.response.gid
        self.magic = self.response.magic
        self.ttl = 255
        self.ridSrc = self.response.rid
        self.checksum = 0

    def computeChecksum(self):
        buffer = self.toBuffer()
        header_size = len(buffer.buffer) - 1
        checksum = 0
        for i,b in enumerate(buffer.buffer):
            if i == header_size:
                continue
            checksum = checksum + (b & 0x000000ff)
            overflow = (checksum >> 8) & 0x000000ff
            if overflow > 0:
                checksum &= 0x000000ff
                checksum = checksum + overflow
        checksum = (~checksum) & 0x000000ff
        return checksum & 0x000000ff
    
    def toBuffer(self):
        buffer = Buffer(data=[])
        buffer.putByte(self.gid)
        buffer.putWord(self.magic)
        buffer.putByte(self.ttl)
        buffer.putByte(self.destRid)
        buffer.putByte(self.ridSrc)
        buffer.putStr(self.message)
        buffer.putByte(self.checksum)
        return buffer


def listenForMessages(threadName, delay, host, port, myRid, nextSlaveIP):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server_address = (host, port)
    sock.bind(server_address)
    while True:
        data, _ = sock.recvfrom(4096)
        messageResponse = MessageResponse(data)
        retrievedChecksum = messageResponse.checkSum
        calculatedChecksum = messageResponse.computeChecksum()
        if (retrievedChecksum == calculatedChecksum):
            if (messageResponse.ridDest == myRid):
                print "Message: " + str(messageResponse.message)
            else:
                messageResponse.ttl = messageResponse.ttl - 1
                if messageResponse.ttl < 2:
                    # discard
                    print "Warning: Message Discarded"
                else:
                    #print "Forwarding"
                    newChecksum = messageResponse.computeChecksum()
                    messageResponse.checkSum = newChecksum
                    dottedIP = socket.inet_ntoa(struct.pack('>L', nextSlaveIP))
                    next_address = (dottedIP, port - 1)
                    newResponse = messageResponse.toBuffer()
                    forwardingBuffer = str(newResponse.buffer)
                    sock.sendto(forwardingBuffer, next_address)
        else:
            print "Error: Checksums do not match."
            print "Received Checksum: " + str(retrievedChecksum)
            print "Computed Checksum: " + str(calculatedChecksum)

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

try:
    thread.start_new_thread(
        listenForMessages,
        ("Thread-2", 4, hostname, udpPort, resp.rid, resp.nextSlaveIP))
except:
    print "Error: unable to start thread"

while (True):
    testing = 1
    inputRingID = raw_input("\nEnter Ring ID: ")
    ringID = int(inputRingID)
    message = raw_input("Message: ")
    sendingMessage = Message(resp, ringID, message)
    sendingMessage.checksum = sendingMessage.computeChecksum()
    print "Sending Checksum: " + str(sendingMessage.checksum)
    sendingBuffer = str(sendingMessage.toBuffer().buffer)
    #sendingMessage.buffer.printBuffer()
    
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    dottedIP = socket.inet_ntoa(struct.pack('>L', resp.nextSlaveIP))
    next_address = (dottedIP, udpPort - 1)
    sent = sock.sendto(sendingBuffer, next_address)
