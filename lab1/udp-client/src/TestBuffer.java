public class TestBuffer {

    public static void main(String[] args) {
        new TestBuffer();
    }

    public TestBuffer () {
        Buffer buffer = new Buffer(100);
        buffer.putWord(500);
        buffer.printArray();
        buffer.reset();
        System.out.println(buffer.readWord());
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

        public void reset() {
            this.pointer = 0;
        }

        /**
         * Get the byte array.
         * @return
         */
        public byte[] getByteArray() {
            return buffer;
        }

        public void printArray() {
            for(int i = 0; i < pointer; i++)
                System.out.print(this.buffer[i] + " ");
            System.out.println();
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
