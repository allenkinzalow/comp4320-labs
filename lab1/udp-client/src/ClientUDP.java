import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

/**
 * Created by allen on 9/2/2018.
 */
public class ClientUDP {

    private static final String SERVER = "localhost";
    private static final int PORT = 10012;
    private static final int TIMEOUT = 3000;

    public static void main(String[] args) throws IOException {
        if ((args.length < 2) || (args.length > 3)) // Test for correct # of args
            throw new IllegalArgumentException("Parameter(s): <Talker> <Server> [<Port>]");

        // Read in arguments.
        String client = args[0];
        InetAddress serverAddress = InetAddress.getByName(args.length >= 2 ? args[1] : SERVER);
        short serverPort = (args.length == 3) ? Short.parseShort(args[2]) : PORT;

        System.out.println("Initializing " + client + " on " + serverAddress.getHostAddress() + ":" + serverPort);

        // Initialize the UDP Client.
        ClientUDP clientUDP = new ClientUDP(serverAddress, serverPort);
        // listen for user input.
        clientUDP.listen();
    }

    private InetAddress server;
    private short port;

    public ClientUDP(InetAddress server, short port) {
        this.server = server;
        this.port = port;
    }

    /**
     * Listen for user console input.
     */
    public void listen() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter an OP code: ");
        while (true) {
            if (scanner.hasNext()) {
                try {
                    byte opcode = scanner.nextByte();
                    Operation operation = Operation.find(opcode);
                    short[] operands = new short[operation.getRequiredParams()];
                    if (operation != null) {
                        for (int param = 1; param <= operation.getRequiredParams(); param++) {
                            System.out.print("Enter Operand #" + param + ": ");
                            operands[param - 1] = scanner.nextShort();
                        }
                        long startTime = System.currentTimeMillis();
                        Response response = this.sendRequest(new Request(operation.getType(), operands));
                        this.printResponse(response);
                        long endTime = System.currentTimeMillis();
                        long timeTaken = endTime - startTime;
                        System.out.println("Time taken for request: " + timeTaken + " milliseconds.");
                    } else
                        System.out.println("Invalid OP code. Use (0-6).");
                    System.out.print("Enter an OP code: ");
                } catch (Exception e) {
                    System.out.println("Please use integers for opcode and operands.");
                }
            }
        }
    }

    /**
     * Dispatch an operation request
     * 
     * @param request
     */
    private Response sendRequest(Request request) {
        Operation operation = Operation.find(request.getOpcode());
        if (operation == null)
            return null;
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(TIMEOUT);

            Buffer requestBuffer = request.getBuffer();
            byte[] bytesToSend = requestBuffer.getByteArray();

            // Send packet
            DatagramPacket sendPacket = new DatagramPacket(bytesToSend, bytesToSend.length, this.server, this.port);
            socket.send(sendPacket);

            DatagramPacket receivePacket = // Receiving packet
                    new DatagramPacket(new byte[bytesToSend.length], bytesToSend.length);
            socket.receive(receivePacket);

            byte[] receivedBytes = receivePacket.getData();
            Buffer received = new Buffer(receivedBytes);
            byte tml = received.read();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tml; i++)
                sb.append(String.format("%02X ", receivedBytes[i]));
            System.out.println("Received bytes: " + sb.toString());
            byte requestID = received.read();
            byte error = received.read();
            int result = received.readWord();
            socket.close();
            return new Response(requestID, result, error);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Print the results of a server response to the user.
     * 
     * @param response
     */
    private void printResponse(Response response) {
        if (response.getError() > 0) {
            System.out.println("Error code: " + response.getError() + ". Please try again.");
            return;
        }
        System.out.println("Response for Request #" + response.getRequestID() + " is: " + response.getResult());
    }

    /**
     * A simple enum defining operations.
     */
    enum Operation {
        ADD(0, 2), SUBTRACT(1, 2), OR(2, 2), AND(3, 2), RIGHT(4, 2), LEFT(5, 2), NOT(6, 1);

        private byte type;
        private int requiredParams;

        Operation(int type, int requiredParams) {
            this.type = (byte) type;
            this.requiredParams = requiredParams;
        }

        public byte getType() {
            return type;
        }

        public int getRequiredParams() {
            return requiredParams;
        }

        public static Operation find(int type) {
            for (Operation op : Operation.values())
                if (op.getType() == type)
                    return op;
            return null;
        }
    }

    private class Request {
        private byte opcode;
        private short[] operands;

        public Request(byte opcode, short[] operands) {
            this.opcode = opcode;
            this.operands = operands;
        }

        public byte getOpcode() {
            return opcode;
        }

        public short[] getOperands() {
            return operands;
        }

        /**
         * Generate a random value.
         * 
         * @return
         */
        private byte generateRequestID() {
            return (byte) (Math.random() * Byte.MAX_VALUE);
        }

        /**
         * Produce the buffer for this request.
         * 
         * @return
         */
        public Buffer getBuffer() {
            Buffer buffer = new Buffer();

            // TML
            buffer.put(4 + (this.getOperands().length * 2));
            // RequestID
            buffer.put(this.generateRequestID());
            // Op Code
            buffer.put(this.getOpcode());
            // # of Operands
            buffer.put(this.getOperands().length);
            // Operands
            for (short operand : this.getOperands())
                buffer.putShort(operand);
            return buffer;
        }
    }

    /**
     * An easy way to represent a response.
     */
    private class Response {
        private byte requestID;
        private int result;
        private byte error;

        public Response(byte requestID, int result, byte error) {
            this.requestID = requestID;
            this.result = result;
            this.error = error;
        }

        public byte getRequestID() {
            return requestID;
        }

        public int getResult() {
            return result;
        }

        public byte getError() {
            return error;
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
