import socket
import sys
import struct
import thread

class Buffer:
    def __init__(self, data=[0] * 100):
        self.position = 0
        self.buffer = bytearray(data)

    def readStr(self, endIndex):
        subBuffer = bytearray()
        end = endIndex - self.position
        for i in range(0, end):
            subBuffer.append(self.buffer[self.position + i])
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
        self.message = self.buffer.readStr(self.getMessageEnd())
        self.checkSum = self.buffer.readByte()

    def getMessageEnd(self):
        header = self.buffer.buffer
        end = len(header) - 1
        return end

    def computeChecksum(self):
        buffer = self.toBuffer()
        header = bytearray()
        for i in range(0,len(buffer.buffer) - 2):
            header.append(buffer.buffer[i])

        checksum = 0
        for b in header:
            checksum = checksum + (b & 0xFF)
            overflow = (checksum >> 8) & 0x000000ff
            if overflow > 0:
                checksum &= 0xFF
                checksum = checksum + overflow
        checksum = (~checksum) & 0x000000ff
        return checksum & 0x000000ff
    
    def toBuffer(self):
        buffer = Buffer()
        buffer.putByte(self.gid)
        buffer.putWord(self.magic)
        buffer.putByte(self.ttl)
        buffer.putByte(self.ridDest)
        buffer.putByte(self.ridSrc)
        buffer.putStr(self.message)
        buffer.putByte(self.checkSum)
        return buffer


def testChecksum():
    resp = '\x02Joy!\xff\x01\x00hello\x02'
    messageResponse = MessageResponse(resp)

    actualValue = messageResponse.computeChecksum()
    expectedValue = 104
    if (actualValue == expectedValue):
        print "Pass"
    else:
        print "Fail"
        print "Expected: " + str(expectedValue)
        print "Actual: " + str(actualValue)

testChecksum()