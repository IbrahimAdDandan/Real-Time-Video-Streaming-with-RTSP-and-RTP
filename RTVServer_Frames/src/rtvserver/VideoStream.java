package rtvserver;

import java.io.*;

public class VideoStream {

    FileInputStream fis; //video file
    int frame_nb; //current frame nb
    int i;
    String[] frames;
    //-----------------------------------
    //constructor
    //-----------------------------------

    public VideoStream(String filename) throws Exception {

        //init variables
        frame_nb = 0;
        i = 1;
        frames = new String[30];
        for (int j = 0; j < frames.length; j++) {
            frames[j] = "E:\\Frames\\" + (j + 1) + ".jpg";
        }
    }

    //-----------------------------------
    // getnextframe
    //returns the next frame as an array of byte and the size of the frame
    //-----------------------------------
    /*
  ----------Orginal---------------
  public int getnextframe(byte[] frame) throws Exception
  {
    int length = 0;
    String length_string;
    byte[] frame_length = new byte[5];

    //read current frame length
    fis.read(frame_length,0,5);
	
    //transform frame_length to integer
    length_string = new String(frame_length);
    length = Integer.parseInt(length_string);
	
    return(fis.read(frame,0,length));
  }
  //--------------------------------------*/
//  public int getnextframe(byte[] frame) throws Exception
//  {
//    @SuppressWarnings("UnusedAssignment")
//    int length = 0;
//    String length_string;
//    byte[] frame_length = new byte[5];
//
//    //read current frame length
//    fis.read(frame_length,0,5);
//	
//    //transform frame_length to integer
//    length_string = new String(frame_length);
//    //length = Integer.parseInt(length_string);
//    return(fis.read(frame,0,1000));
//  }
    public int getnextframe(byte[] frame) throws Exception {
        if (this.i > 29) {
            i = 0;
        }
        fis = new FileInputStream(frames[i]);
        this.i++;

        return (fis.read(frame));
    }
}
