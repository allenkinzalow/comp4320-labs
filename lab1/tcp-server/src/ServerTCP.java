import java.io.IOException;

/**
 * Created by allen on 9/2/2018.
 */
public class ServerTCP {

    public static void main(String[] args) {

    }

    private interface Execute { int execute(int o1, int o2); }

    enum Operation {
        ADD(0, "Add", 2, (int o1, int o2) -> { return o1 + 02; }),
        SUBTRACT(1, "Subtract", 2, (int o1, int o2) -> { return o1 - 02; }),
        OR(2, "OR", 2, (int o1, int o2) -> { return o1 | 02; }),
        AND(3, "AND", 2, (int o1, int o2) -> { return o1 & 02; }),
        RIGHT(4, "Right Shift", 2, (int o1, int o2) -> { return o1 >> 02; }),
        LEFT(5, "Left Shift", 2, (int o1, int o2) -> { return o1 << 02; }),
        NOT(6, "Not", 1, (int o1, int o2) -> { return ~o1; });

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
        public void fromBuffer(byte[] bytes) throws IOException {
            Buffer buffer = new Buffer(bytes);
            byte totalMessageLength = buffer.read();
            if(totalMessageLength != bytes.length)
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
        public byte getRequestID() { return requestID; }
        public int getResult() { return result; }
        public byte getError() { return error; }

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
            buffer.put(this.getError());
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
            return (short) ((buffer[pointer++] << 8 & 255) + (buffer[pointer++] & 255));
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
            return (buffer[pointer++] << 24 & 255) + (buffer[pointer++] << 16 & 255)
                    + (buffer[pointer++] << 8 & 255) + (buffer[pointer++] & 255);
        }
    }

}
