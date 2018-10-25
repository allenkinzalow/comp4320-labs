import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by allen on 10/22/2018.
 */
public class Master {

    private static final int GID = 12;
    private static final int PORT = 10010 + (GID % 30) * 5;
    private static final int BUFSIZE = 5; // Size of receive buffer

    public static void main(String[] args) {
        if (args.length > 1)
            throw new IllegalArgumentException("Parameter(s): [<Port>]");

        // Read in arguments.
        short serverPort = (args.length == 1) ? Short.parseShort(args[0]) : PORT;

        Master server = new Master(serverPort);
        server.listen();

    }

    private short port;
    private byte nextRID;
    private String nextSlaveIP;

    public Master(short port) {
        this.port = port;
        this.nextRID = 1;
        try {
            this.nextSlaveIP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Listen for incomming clients.
     */
    public void listen() {
        try {
            ServerSocket server = new ServerSocket(this.port);
            System.out.println("Listening on port: " + this.port);
            byte[] receivedBytes = new byte[BUFSIZE];
            while (true) { // Run forever, accepting and servicing connections
                Socket clntSock = server.accept(); // Get client connection
                System.out.println("Incomming connection from: " + clntSock.getInetAddress().getHostAddress());
                /**
                 * Receive input.
                 */
                InputStream in = clntSock.getInputStream();
                in.read(receivedBytes);
                Request request = new Request(clntSock.getInetAddress().getHostAddress(), receivedBytes);
                request.fromBuffer();

                /**
                 * Generate and send reponse.
                 */
                Response response = this.processRequest(request);
                System.out.println("Assigned RID: " + response.getAssignedRID() + " -- Assigned Slave IP: " + response.getNextSlaveIPString());
                OutputStream out = clntSock.getOutputStream();
                byte[] outBuffer = response.getBuffer().getByteArray();
                out.write(outBuffer, 0, outBuffer.length);

                clntSock.close(); // Close the socket. We are done with this client!
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Process an incomming request, return a reponse.
     *
     * @param request
     * @return
     */
    private Response processRequest(Request request) {
        Response response = new Response(request.getGID(), request.getMagicNumber(), this.nextRID, this.nextSlaveIP,
                Error.NONE);
        this.nextRID++;
        this.nextSlaveIP = request.getIpAddress();
        return response;
    }

    /**
     * Organization for errors.
     */
    enum Error {
        NONE(0), INVALID_BUFFER_SIZE(1), INVALID_MAGIC_NUMBER(2);

        private byte code;

        Error(int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return code;
        }
    }

    /**
     * Incomming request from client.
     */
    private class Request {
        private String ipAddress;
        private byte[] bytes;
        private byte GID;
        private int magicNumber;

        public Request(String ipAddress, byte[] bytes) {
            this.ipAddress = ipAddress;
            this.bytes = bytes;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public byte getGID() {
            return GID;
        }

        public int getMagicNumber() {
            return magicNumber;
        }

        /**
         * Read from the buffer for the request.
         *
         * @return
         */
        public void fromBuffer() throws IOException {
            Buffer buffer = new Buffer(this.bytes);
            /*
             * if(totalMessageLength != this.bytes.length) throw new
             * IOException("Total message length does not equal buffer size.");
             */
            this.GID = buffer.read();
            this.magicNumber = buffer.readWord();
        }
    }

    /**
     * An easy way to represent a response to the client.
     */
    private class Response {
        private byte GID;
        private int magicNumber;
        private byte assignedRID;
        private int nextSlaveIP;
        private Error error;

        public Response(byte GID, int magicNumber, byte assignedRID, String ipAddress, Error error) {
            this.GID = GID;
            this.magicNumber = magicNumber;
            this.assignedRID = assignedRID;
            this.nextSlaveIP = this.ipToInteger(ipAddress);
            this.error = error;
        }

        public byte getGID() {
            return GID;
        }

        public int getMagicNumber() {
            return magicNumber;
        }

        public byte getAssignedRID() {
            return assignedRID;
        }

        public int getNextSlaveIP() {
            return nextSlaveIP;
        }

        public Error getError() {
            return error;
        }

        /**
         * Produce the buffer for this response.
         *
         * @return
         */
        public Buffer getBuffer() {
            Buffer buffer = new Buffer(10);

            // GID - 1
            buffer.put(this.getGID());
            // Magic Number - 4
            buffer.putWord(this.getMagicNumber());
            // Assigned nextRID - 1
            buffer.put(this.getAssignedRID());
            // Next Slave - 4
            buffer.putWord(this.getNextSlaveIP());
            return buffer;
        }

        private int ipToInteger(String ipAddress) {
            int result = 0;
            String[] parts = ipAddress.split(".");
            for (String part : parts) {
                int p = Integer.parseInt(part);
                result = result << 8 | (p & 0xFF);
            }
            return result;
        }

        public String getNextSlaveIPString() {
            String ip = "";
            ip += ((byte)(this.nextSlaveIP >> 24)) + ".";
            ip += ((byte)(this.nextSlaveIP >> 16)) + ".";
            ip += ((byte)(this.nextSlaveIP >> 8)) + ".";
            ip += ((byte)(this.nextSlaveIP)) + "";
            return ip;
        }
    }

    /**
     * A buffer for reading and writing data.
     */
    public class Buffer {

        /**
         * The position of the buffer.
         */
        private int pointer = 0;

        /**
         * The actual byte array.
         */
        private byte[] buffer;

        /**
         * The maximum default buffer size.
         */
        private final static int MAX_BUFFER = 100;

        public Buffer(int size) {
            this.buffer = new byte[size];
        }

        public Buffer(byte[] buffer) {
            this.buffer = buffer;
        }

        /**
         * Initiate a buffer with a max size.
         */
        public Buffer() {
            this.buffer = new byte[MAX_BUFFER];
        }

        /**
         * Get the byte array.
         *
         * @return
         */
        public byte[] getByteArray() {
            return buffer;
        }

        /**
         * Put a single byte
         *
         * @param data
         */
        public void put(int data) {
            buffer[pointer++] = (byte) data;
        }

        /**
         * Read a single bit.
         *
         * @return
         */
        public byte read() {
            return (byte) (buffer[pointer++] & 255);
        }

        /**
         * Write a short to the buffer.
         *
         * @param data
         */
        public void putShort(short data) {
            buffer[pointer++] = (byte) (data >> 8);
            buffer[pointer++] = (byte) (data);
        }

        /**
         * Read a signed short from the buffer.
         *
         * @return
         */
        public short readShort() {
            return (short) (((buffer[pointer++] & 255) << 8) + (buffer[pointer++] & 255));
        }

        /**
         * Write a word/integer into the buffer.
         *
         * @param data
         */
        public void putWord(int data) {
            buffer[pointer++] = (byte) (data >> 24);
            buffer[pointer++] = (byte) (data >> 16);
            buffer[pointer++] = (byte) (data >> 8);
            buffer[pointer++] = (byte) (data);
        }

        /**
         * Read a word from the buffer.
         *
         * @return
         */
        public int readWord() {
            return ((buffer[pointer++] & 255) << 24) + ((buffer[pointer++] & 255) << 16)
                    + ((buffer[pointer++] & 255) << 8) + (buffer[pointer++] & 255);
        }
    }

}
