/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rtvserver;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 *
 * @author Ibrahim AdDandan
 */
public class RTVServer extends JFrame implements ActionListener {

    /* ------------------
   RTVServer
   usage: java Server [RTSP listening port]
   ---------------------- */
    //RTP variables:
    //----------------
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
    DatagramPacket senddp; //UDP packet containing the video frames

    InetAddress ClientIPAddr; //Client IP address
    int RTP_dest_port = 0; //destination port for RTP packets  (given by the RTSP Client)

    //GUI:
    //----------------
    JLabel label;

    //Video variables:
    //----------------
    int imagenb = 0; //image nb of the image currently transmitted
    VideoStream video; //VideoStream object used to access video frames
    //static int MJPEG_TYPE = 32; //RTP payload type for M1V (MPEG 1 Video) video
    static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
    static int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 500; //length of the video in frames

    Timer timer; //timer used to send the images at the video frame rate
    byte[] buf; //buffer used to store the images to send to the client 

    //RTSP variables
    //----------------
    //rtsp states
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    //rtsp message types
    final static int SETUP = 3;
    final static int PLAY = 4;
    final static int PAUSE = 5;
    final static int TEARDOWN = 6;
    final static int FORWARD = 7;
    static byte[] b;
    static int state; //RTSP Server state == INIT or READY or PLAY
    Socket RTSPsocket; //socket used to send/receive RTSP messages
    //input and output stream filters
    static BufferedReader RTSPBufferedReader;
    static BufferedWriter RTSPBufferedWriter;
    static String VideoFileName; //video file requested from the client
    static int RTSP_ID = 123456; //ID of the RTSP session
    int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session

    final static String CRLF = "\r\n";
    JPanel jpanel;

    //--------------------------------
    //Constructor
    //--------------------------------
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public RTVServer() throws ClassNotFoundException {

        //init Frame
        super("Server");

        timer = new Timer(FRAME_PERIOD, this);
        timer.setInitialDelay(0);
        timer.setCoalesce(true);
        buf = new byte[15000];
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                //stop the timer and exit
                timer.stop();
                System.exit(0);
            }
        });
        jpanel = new JPanel();
        jpanel.setPreferredSize(new java.awt.Dimension(250, 100));
        label = new JLabel("Send frame #        ", JLabel.CENTER);
        label.setFont(new Font("Comic Sans MS", Font.CENTER_BASELINE, 16));
        jpanel.add(label, BorderLayout.CENTER);
        jpanel.setBackground(Color.getHSBColor(0, 0, 25));
        getContentPane().add(jpanel);
    }

    //------------------------------------
    //main
    //------------------------------------
    public static void main(String argv[]) throws Exception {

        /*    VideoStream vs = new VideoStream("E:/1.m1v");
        b = new byte[15];
        int frame = vs.getnextframe(b);
        System.out.print(frame);
        System.out.print(Arrays.toString(b));
         */
        //create a RTVServer object
        RTVServer theServer = new RTVServer();

        //show GUI:
        theServer.pack();
        theServer.setVisible(true);

        //get RTSP socket port from the command line
        int RTSPport = 1035;
        //int RTSPport = Integer.parseInt(argv[0]);

        try ( //Initiate TCP connection with the client for the RTSP session
                ServerSocket listenSocket = new ServerSocket(RTSPport)) {
            theServer.RTSPsocket = listenSocket.accept();
        }

        //Get Client IP address
        theServer.ClientIPAddr = theServer.RTSPsocket.getInetAddress();

        //Initiate RTSPstate
        state = INIT;

        //Set input and output stream filters:
        RTSPBufferedReader = new BufferedReader(new InputStreamReader(theServer.RTSPsocket.getInputStream()));
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theServer.RTSPsocket.getOutputStream()));

        //Wait for the SETUP message from the client
        System.out.print("Wait for the SETUP message from the client");
        int request_type;
        boolean done = false;
        while (!done) {
            request_type = theServer.parse_RTSP_request(); //blocking

            if (request_type == SETUP) {
                done = true;

                //update RTSP state
                state = READY;
                System.out.println("New RTSP state: READY");

                //Send response
                theServer.send_RTSP_response();

                //init the VideoStream object:
                theServer.video = new VideoStream(VideoFileName);

                //init RTP socket
                theServer.RTPsocket = new DatagramSocket();
            }
        }

        //loop to handle RTSP requests
        while (true) {
            //parse the request
            request_type = theServer.parse_RTSP_request(); //blocking

            if ((request_type == PLAY) && (state == READY)) {
                //send back response
                theServer.send_RTSP_response();
                //start timer
                theServer.timer.start();
                //update state
                state = PLAYING;
                System.out.println("New RTSP state: PLAYING");
            } else if ((request_type == PAUSE) && (state == PLAYING)) {
                //send back response
                theServer.send_RTSP_response();
                //stop timer
                theServer.timer.stop();
                //update state
                state = READY;
                System.out.println("New RTSP state: READY");
            }else if ((request_type == FORWARD) && (state == PLAYING)) {
                //send back response
                theServer.send_RTSP_response();
                //decrease timer's delay
                theServer.timer.setDelay(15);
                //update state
                state = FORWARD;
                System.out.println("New RTSP state: FORWARD");
            }else if ((request_type == FORWARD) && (state == FORWARD)) {
                //send back response
                theServer.send_RTSP_response();
                //decrease timer's delay
                theServer.timer.setDelay(100);
                //update state
                state = PLAYING;
                System.out.println("New RTSP state: PLAYING");
            } else if (request_type == TEARDOWN) {
                //send back response
                theServer.send_RTSP_response();
                //stop timer
                theServer.timer.stop();
                //close sockets
                theServer.RTSPsocket.close();
                theServer.RTPsocket.close();

                System.exit(0);
            }
        }
    }

    //------------------------
    //Handler for timer
    //------------------------
    @Override
    public void actionPerformed(ActionEvent e) {

        //if the current image nb is less than the length of the video
        if (imagenb < VIDEO_LENGTH) {
            //update current imagenb
            imagenb++;

            try {
                //get next frame to send from the video, as well as its size
                int image_length = video.getnextframe(buf);

                //Builds an RTPpacket object containing the frame
                RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb * this.timer.getDelay(), buf, image_length);

                //get to total length of the full rtp packet to send
                int packet_length = rtp_packet.getlength();

                //retrieve the packet bitstream and store it in an array of bytes
                byte[] packet_bits = new byte[packet_length];
                rtp_packet.getpacket(packet_bits);

                //send the packet as a DatagramPacket over the UDP socket 
                senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
                RTPsocket.send(senddp);

                System.out.println("Send frame #" + imagenb);
                System.out.println("Send time " + rtp_packet.gettimestamp());
                System.out.println("Send seqNum " + rtp_packet.getsequencenumber());
                //update GUI
                label.setText("Send frame #" + imagenb);
            } catch (Exception ex) {
                System.out.println("Exception caught: " + ex);
                System.exit(0);
            }
        } else {
            //if we have reached the end of the video file, stop the timer
            timer.stop();
        }
    }

    //------------------------------------
    //Parse RTSP Request
    //------------------------------------
    @SuppressWarnings("RedundantStringConstructorCall")
    private int parse_RTSP_request() {
        int request_type = -1;
        try {
            //parse request line and extract the request_type:
            String RequestLine = RTSPBufferedReader.readLine();
            System.out.println("RTSP Server - Received from Client:");
            System.out.println(RequestLine);

            StringTokenizer tokens = new StringTokenizer(RequestLine);
            String request_type_string = tokens.nextToken();

            //convert to request_type structure:
            if ((new String(request_type_string)).compareTo("SETUP") == 0) {
                request_type = SETUP;
            } else if ((new String(request_type_string)).compareTo("PLAY") == 0) {
                request_type = PLAY;
            } else if ((new String(request_type_string)).compareTo("PAUSE") == 0) {
                request_type = PAUSE;
            } else if ((new String(request_type_string)).compareTo("TEARDOWN") == 0) {
                request_type = TEARDOWN;
            }else if ((new String(request_type_string)).compareTo("FORWARD") == 0) {
                request_type = FORWARD;
            }

            if (request_type == SETUP) {
                //extract VideoFileName from RequestLine
                VideoFileName = tokens.nextToken();
            }

            //parse the SeqNumLine and extract CSeq field
            String SeqNumLine = RTSPBufferedReader.readLine();
            System.out.println(SeqNumLine);
            tokens = new StringTokenizer(SeqNumLine);
            tokens.nextToken();
            RTSPSeqNb = Integer.parseInt(tokens.nextToken());

            //get LastLine
            String LastLine = RTSPBufferedReader.readLine();
            System.out.println(LastLine);

            if (request_type == SETUP) {
                //extract RTP_dest_port from LastLine
                tokens = new StringTokenizer(LastLine);
                for (int i = 0; i < 3; i++) {
                    tokens.nextToken(); //skip unused stuff
                }
                //RTP_dest_port = Integer.parseInt(tokens.nextToken());
                RTP_dest_port = 25000;
            }
            //else LastLine will be the SessionId line ... do not check for now.
        } catch (IOException | NumberFormatException ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
        return (request_type);
    }

    //------------------------------------
    //Send RTSP Response
    //------------------------------------
    private void send_RTSP_response() {
        try {
            RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
            RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);
            RTSPBufferedWriter.write("Session: " + RTSP_ID + CRLF);
            RTSPBufferedWriter.flush();
            System.out.println("RTSP Server - Sent response to Client.");
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
    }
}
