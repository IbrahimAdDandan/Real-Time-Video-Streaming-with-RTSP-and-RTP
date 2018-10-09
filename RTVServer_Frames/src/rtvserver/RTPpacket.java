package rtvserver;

public class RTPpacket {

    //size of the RTP header:
    static int HEADER_SIZE = 12;

    //Fields that compose the RTP header
    public int Version;
    public int Padding;
    public int Extension;
    public int CC;
    public int Marker;
    public int PayloadType;
    public int SequenceNumber;
    public int TimeStamp;
    public int Ssrc;

    //Bitstream of the RTP header
    public byte[] header;

    //size of the RTP payload
    public int payload_size;
    //Bitstream of the RTP payload
    public byte[] payload;

    //--------------------------
    //Constructor of an RTPpacket object from header fields and payload bitstream
    //--------------------------
    @SuppressWarnings("SuspiciousSystemArraycopy")
    public RTPpacket(int PType, int Framenb, int Time, byte[] data, int data_length) {
        //fill by default header fields:
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 0;

        //fill changing header fields:
        SequenceNumber = Framenb;
        TimeStamp = Time;
        PayloadType = PType;

        //build the header bistream:
        //--------------------------
        header = new byte[HEADER_SIZE];
        header[0] = (byte) 128;
        header[1] = (byte) PType;
        byte header2 = (byte)(SequenceNumber / 128);
        //byte header2 = (byte)(SequenceNumber >> 8);
        header[2] = header2;
        byte header3 = (byte) (SequenceNumber % 128);
        //byte header3 = (byte)(SequenceNumber & 0xFF);
        header[3] = header3;
        byte header4 = (byte) (TimeStamp / 2097152);
        header[4] = header4;
        int temp = TimeStamp % 2097152;
        byte header5 = (byte) (temp / 16384);
        header[5] = header5;
        temp = (TimeStamp % 16384);
        byte header6 = (byte) (temp / 128);
        header[6] = header6;
        header[7] = (byte) (TimeStamp % 128);
        payload_size = data_length;
        payload = new byte[data_length];
        System.arraycopy(data, 0, payload, 0, data_length);
    }

    //--------------------------
    //getpayload: return the payload bistream of the RTPpacket and its size
    //--------------------------
    public int getpayload(byte[] data) {

        System.arraycopy(payload, 0, data, 0, payload_size);

        return (payload_size);
    }

    //--------------------------
    //getpayload_length: return the length of the payload
    //--------------------------
    public int getpayload_length() {
        return (payload_size);
    }

    //--------------------------
    //getlength: return the total length of the RTP packet
    //--------------------------
    public int getlength() {
        return (payload_size + HEADER_SIZE);
    }

    //--------------------------
    //getpacket: returns the packet bitstream and its length
    //--------------------------
    public int getpacket(byte[] packet) {
        //construct the packet = header + payload
        System.arraycopy(header, 0, packet, 0, HEADER_SIZE);
        System.arraycopy(payload, 0, packet, HEADER_SIZE, payload_size);

        //return total size of the packet
        return (payload_size + HEADER_SIZE);
    }

    //--------------------------
    //gettimestamp
    //--------------------------
    public int gettimestamp() {
        return (TimeStamp);
    }

    //--------------------------
    //getsequencenumber
    //--------------------------
    public int getsequencenumber() {
        return (SequenceNumber);
    }

    //--------------------------
    //getpayloadtype
    //--------------------------
    public int getpayloadtype() {
        return (PayloadType);
    }

}
