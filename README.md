# Real Time Video Streaming with RTSP and RTP

a server that encapsulates stored video frames into RTP packets, grab
video frame, add RTP headers, create UDP segments, send segments to UDP
socket, include seq numbers and time stamps. Also a client side of RTSP,
issue play/pause commands.

## The Server

The server implementation is based on state machine coding style.
SETUP, PLAY, PAUSE, and TEARDOWN are messages sent by Client
using RTSP.
the server reads the frames (images) with extension .jpg from the path: "E:/frames/" and reapeat every 29 frames; the frames' names are numbers 0,1,2,3,...29.

## The Client

The client job is to send RTSP messages and receive RTP stream.