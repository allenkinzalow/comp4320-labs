import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by allen on 9/2/2018.
 */
public class ServerTCP {

    private static final int PORT = 10012;
    private static final int BUFSIZE = 32; // Size of receive buffer

    public static void main(String[] args) {
        if ((args.length < 1) || (args.length > 2)) // Test for correct # of args
            throw new IllegalArgumentException("Parameter(s): <Talker> [<Port>]");

        // Read in arguments.
        String client = args[0];
        short serverPort = (args.length == 2) ? Short.parseShort(args[1]) : PORT;

        ServerTCP server = new ServerTCP(serverPort);
        server.listen();
    }

    private short port;
    public ServerTCP(short port) {
        this.port = port;
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

                /**
                * Receive input.
                */
                InputStream in = clntSock.getInputStream();
                in.read(receivedBytes);
                Request request = new Request(receivedBytes);
                request.fromBuffer();
                System.out.println("Incomming request: " + request.getRequestID());

                /**
                * Generate and send reponse.
                */
                Response response = this.processRequest(request);
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
     * @param request
     * @return
     */
    private Response processRequest(Request request) {
        Operation operation = Operation.find(request.getOpcode());
        if(operation != null) {
            if(request.getOperands().length != operation.getRequiredParams())
                return new Response(request.getRequestID(), 0, Error.INCORRECT_OPERAND_LENTH);
            int result = operation.getExecutor().execute(request.getOperands());
            return new Response(request.getRequestID(), result, Error.NONE);
        } else
            return new Response(request.getRequestID(), 0, Error.INVALID_OPERATION);

    }

    /**
     * Interface for arithmetic operations.
     */
    private interface Execute { int execute(short ... operands); }

    /**
     * Representation of available operations that clients can request.
     */
    enum Operation {
        ADD(0, "Add", 2, (short[] o) -> { return o[0]  + o[1]; }),
        SUBTRACT(1, "Subtract", 2, (short[] o) -> { return o[0]  - o[1]; }),
        OR(2, "OR", 2, (short[] o) -> { return o[0]  | o[1]; }),
        AND(3, "AND", 2, (short[] o) -> { return o[0]  & o[1]; }),
        RIGHT(4, "Right Shift", 2, (short[] o) -> { return o[0]  >> o[1]; }),
        LEFT(5, "Left Shift", 2, (short[] o) -> { return o[0] << o[1]; }),
        NOT(6, "Not", 1, (short[] o) -> { return ~o[0]; });

        private int type;
        private String name;
        private int requiredParams;
        private Execute executor;

        Operation(int type, String name, int requiredParams, Execute executor) {
            this.type = type;
            this.name = name;
            this.requiredParams = requiredParams;
            this.executor = executor;
        }

        public int getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public int getRequiredParams() { return requiredParams; }

        public Execute getExecutor() {
            return executor;
        }

        public static Operation find(int type) {
            for(Operation op : Operation.values())
                if(op.getType() == type)
                    return op;
            return null;
        }
    }

    /**
     * Organization for errors.
     */
    enum Error {
        NONE(0),
        INVALID_OPERATION(1),
        INCORRECT_OPERAND_LENTH(2);

        private byte code;
        Error(int code) {
            this.code = (byte)code;
        }

        public byte getCode() {
            return code;
        }
    }

    /**
     * Incomming request from client.
     */
    private class Request {
        private byte[] bytes;
        private byte opcode;
        private byte requestID;
        private short[] operands;

        public Request(byte[] bytes) {
            this.bytes = bytes;
        }

        public byte getOpcode() { return opcode; }
        public short[] getOperands() { return operands; }
        public byte getRequestID() { return requestID; }

        /**
         * Generate a random value.
         * @return
         */
        private byte generateRequestID() {
            return (byte)(Math.random() * Byte.MAX_VALUE);
        }

        /**
         * Read from the buffer for the request.
         * @return
         */
        public void fromBuffer() throws IOException {
            Buffer buffer = new Buffer(this.bytes);
            byte totalMessageLength = buffer.read();
            if(totalMessageLength != this.bytes.length)
                throw new IOException("Total message length does not equal buffer size.");
            this.requestID = buffer.read();
            this.opcode = buffer.read();
            byte opcodeLength = buffer.read();
            // Operands
            for(int index = 0; index < opcodeLength; index++)
                this.operands[index] = buffer.readShort();
        }
    }

    /**
     * An easy way to represent a response to the client.
     */
    private class Response {
        private byte requestID;
        private int result;
        private Error error;
        public Response(byte requestID, int result, Error error) {
            this.requestID = requestID;
            this.result = result;
            this.error = error;
        }
        public byte getRequestID() { return requestID; }
        public int getResult() { return result; }
        public Error getError() { return error; }

        /**
         * Produce the buffer for this response.
         * @return
         */
        public Buffer getBuffer() {
            Buffer buffer = new Buffer();

            // TML
            buffer.put(6);
            // RequestID
            buffer.put(this.getRequestID());
            // Error
            buffer.put(this.getError().getCode());
            // Result
            buffer.putWord(this.getResult());
            return buffer;
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
         * @return
         */
        public byte[] getByteArray() {
            return buffer;
        }

        /**
         * Put a single byte
         * @param data
         */
        public void put(int data) {
            buffer[pointer++] = (byte) data;
        }

        /**
         * Read a single bit.
         * @return
         */
        public byte read() {
            return (byte)(buffer[pointer++] & 255);
        }

        /**
         * Write a short to the buffer.
         * @param data
         */
        public void putShort(short data) {
            buffer[pointer++] = (byte) (data >> 8);
            buffer[pointer++] = (byte) (data);
        }

        /**
         * Read a signed short from the buffer.
         * @return
         */
        public short readShort() {
            return (short) (((buffer[pointer++] & 255) << 8) + (buffer[pointer++] & 255));
        }

        /**
         * Write a word/integer into the buffer.
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
         * @return
         */
        public int readWord() {
            return ((buffer[pointer++] & 255) << 24) + ((buffer[pointer++] & 255) << 16)
                    + ((buffer[pointer++] & 255) << 8) + (buffer[pointer++] & 255);
        }
    }

}
