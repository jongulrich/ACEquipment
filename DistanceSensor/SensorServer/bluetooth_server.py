'''
Bluetooth socket support

Copyright 2018  Gunnar Bowman, Emily Boyes, Trip Calihan, Simon D. Levy, Shepherd Sims

MIT License
'''

import os

import bluetooth as bt

import RPi.GPIO as GPIO                    #Import GPIO library
import time                                #Import time library

TRIG = 23                                  #Associate pin 23 to TRIG
ECHO = 24                                  #Associate pin 24 to ECHO

class BluetoothServer(object):
    '''
    Provides an abstract class for serving sockets over Bluetooth.  You call the constructor and the start()
    method.  You must implement the method handleMessage(self, message) to handle messages from the client.
    '''

    def __init__(self):
        '''
        Constructor
        '''

        # Arbitrary service UUID to advertise
        self.uuid = "7be1fcb3-5776-42fb-91fd-2ee7b5bbb86d"

        self.client_sock = None
        
    def start(self):
        '''
        Serves a socket on the default port, listening for clients.  Upon client connection, runs a loop to 
        that receives period-delimited messages from the client and calls the sub-class's 
        handleMessage(self, message) method.   Sub-class can call send(self, message) to send a 
        message back to the client.   Begins listening again after client disconnects.
        '''

        # Make device visible
        os.system("hciconfig hci0 piscan")

        # Create a new server socket using RFCOMM protocol
        server_sock = bt.BluetoothSocket(bt.RFCOMM)

        # Bind to any port
        server_sock.bind(("", bt.PORT_ANY))

        # Start listening
        server_sock.listen(1)

        # Get the port the server socket is listening
        port = server_sock.getsockname()[1]

        # Start advertising the service
        bt.advertise_service(server_sock, "RaspiBtSrv",
                           service_id=self.uuid,
                           service_classes=[self.uuid, bt.SERIAL_PORT_CLASS],
                           profiles=[bt.SERIAL_PORT_PROFILE])

        # Outer loop: listen for connections from client
        while True:

            print("Waiting for connection on RFCOMM channel %d" % port)

            try:

                # This will block until we get a new connection
                self.client_sock, client_info = server_sock.accept()
                print("Accepted connection from " +  str(client_info))

                # Track strings delimited by '.'
                s = ''



                while True:
        
                    GPIO.setmode(GPIO.BCM)                     #Set GPIO pin numbering 
                    GPIO.setup(TRIG,GPIO.OUT)                  #Set pin as GPIO out
                    GPIO.setup(ECHO,GPIO.IN)                   #Set pin as GPIO in
                    GPIO.output(TRIG, False)                    #Set TRIG as LOW

                    time.sleep(0.00001) 
                    #print "Waitng For Sensor To Settle"
                    #time.sleep(2)                            #Delay of 2 seconds

                    GPIO.output(TRIG, True)                  #Set TRIG as HIGH
                    time.sleep(0.00001)                      #Delay of 0.00001 seconds
                    GPIO.output(TRIG, False)                 #Set TRIG as LOW

                    while GPIO.input(ECHO)==0:               #Check whether the ECHO is LOW
                        pulse_start = time.time()              #Saves the last known time of LOW pulse

                    while GPIO.input(ECHO)==1:               #Check whether the ECHO is HIGH
                        pulse_end = time.time()                #Saves the last known time of HIGH pulse 

                    pulse_duration = pulse_end - pulse_start #Get pulse duration to a variable

                    distance = pulse_duration * 17150        #Multiply pulse duration by 17150 to get distance
                    distance = round(distance, 2)            #Round to two decimal points

                    if distance > 2 and distance < 400:      #Check whether the distance is within range
                      print("Distance:",distance - 0.5,"cm")  #Print distance with 0.5 cm calibration
                    else:
                      print("Out Of Range")                   #display out of range

                    self.send(str(int(distance)))

            except IOError:
                pass

            except KeyboardInterrupt:

                if self.client_sock is not None:
                    self.client_sock.close()

                server_sock.close()

                print("Server going down")
                break

    def send(self, message):
        '''
        Appends a period to your message and sends the message back to the client.
        '''
        
        self.client_sock.send((message+'.').encode('utf-8'))
