'''
ComArduino.py
 - Interfaces with Arduino via serial port
 - Saves data to file (named with the first 20 characters)
 - Works with Python 3
 - Needs pyserial to work; to install use the following cmd:
    > python3 -m pip install pyserial
 - Call from cmd line like this (arguments are optional):
    > python3 ComArduino.py <BAUD_RATE> <SERIAL_PORT> <DEBUG>
'''

import serial
import sys
from datetime import datetime

def saveMsg( msg ):
    # TODO: Actually parse the message instead of just saving it to a file
    timestamp = str(datetime.now())
    with open(timestamp + '.jpg', 'w') as img:
        print(msg, file=img)

def main( BAUD_RATE=115200, SERIAL_PORT='/dev/cu.usbserial-A904MIWL', DEBUG=True ):

    # open serial communications
    ser = serial.Serial(SERIAL_PORT, BAUD_RATE)
    try:
        print('Waiting for Arduino...', end='')
        while ser.inWaiting() == 0:
            pass # do nothing until Arduino wakes up
        print('OK')
        
        reading = False
        crntChar = ''
        while True: # infinite loop
            msg = ''
            crntChar = ser.read() # read in a char from the serial port
            if DEBUG: # print it to the console in debug mode
                print(char, end='')
            
            if not reading and crntChar == '<': # start reading on '<' character
                reading = True
                
            if reading:
                if crntChar == '>': # stop reading on '>' character
                    saveMsg(msg)
                    reading = False
                else:
                    msg = msg + crntChar                
            
    except KeyboardInterrupt:
        quit()

if __name__ == '__main__':
    if len(sys.argv) > 1: sys.argv[1] = int(sys.argv[1])
    if len(sys.argv) > 3: sys.argv[3] = bool(sys.argv[3])
    main( *sys.argv[1:4] )