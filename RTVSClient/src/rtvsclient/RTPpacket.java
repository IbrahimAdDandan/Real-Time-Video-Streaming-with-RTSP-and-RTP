package rtvsclient;

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
    //Constructor of an RTPpacket object from the packet bistream 
    //--------------------------
    public RTPpacket(byte[] packet, int packet_size) {
        //fill default fields:
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 0;

        //check if total packet size is lower than the header size
        if (packet_size >= HEADER_SIZE) {
            //get the header bitsream:
            header = new byte[HEADER_SIZE];
            System.arraycopy(packet, 0, header, 0, HEADER_SIZE);

            //get the payload bitstream:
            payload_size = packet_size - HEADER_SIZE;
            payload = new byte[payload_size];
            for (int i = HEADER_SIZE; i < packet_size; i++) {
                payload[i - HEADER_SIZE] = packet[i];
            }

            //interpret the changing fields of the header:
            PayloadType = header[1] & 127;
            SequenceNumber = (int) header[3] + 128 * ((int) header[2]);
            TimeStamp = (int) header[7] + 128 * ((int) header[6]) + 16384 * (int) ((int)header[5]) + 2097152 * (int) ((int)header[4]);
        }
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
