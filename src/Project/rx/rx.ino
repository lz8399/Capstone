/*
 * rx.ino
 * Controls the receiver module for Team Mahogany's Senior Design Project
 * For use with Arduino Pro Mini
 */

#include <SPI.h>
#include <RH_RF69.h>
#include <RHReliableDatagram.h>

/************ Constant Definitions ************/

#define RF69_FREQ     915.0 // use 915.0 MHz

#define MY_ADDRESS    1 // server address

#define RFM69_INT     3 // RFM69
#define RFM69_CS      4 // interface
#define RFM69_RST     5 // ports

#define WAITSER       true // specify whether to wait for serial monitor

/************ Variable Declarations ************/

RH_RF69 rf69(RFM69_CS, RFM69_INT); // radio driver instance

// Class to manage message delivery and receipt, using the driver declared above
RHReliableDatagram rf69_manager(rf69, MY_ADDRESS);

uint8_t buf[RH_RF69_MAX_MESSAGE_LEN]; // received message buffer

/************ Initialization Routine ************/

void setup() 
{
  Serial.begin(115200);
  if (WAITSER) {
    while (!Serial) {} // wait for port to open
  }
 
  pinMode(RFM69_RST, OUTPUT);
  digitalWrite(RFM69_RST, LOW);

  // manual reset
  digitalWrite(RFM69_RST, HIGH);
  delay(10);
  digitalWrite(RFM69_RST, LOW);
  delay(10);
  
  Serial.println("Initializing radio...")
  if (!rf69_manager.init()) {
    Serial.println("ERROR: Could not initialize"); return;
  }
  if (!rf69.setFrequency(RF69_FREQ)) {
    Serial.println("ERROR: setFrequency failed"); return;
  }
  
  // Set power and AES key of radio
  rf69.setTxPower(20);
  uint8_t key[] = { 0x1c, 0xef, 0xb2, 0x33, 0xe9, 0x0b, 0xf2, 0x3c,
                    0xb7, 0xee, 0x23, 0x3f, 0xb0, 0x88, 0xa6, 0xcd };
  rf69.setEncryptionKey(key);
  
  Serial.println("success");
}

/************ Main Loop ************/

void loop() {
  if (rf69_manager.available())
  {
    // Wait for a message addressed to us from the client
    uint8_t len = sizeof(buf);
    uint8_t from;
    if (rf69_manager.recvfromAck(buf, &len, &from)) {
      buf[len] = 0; // zero out remaining string
      Serial.println((char*)buf);
    }
  }
}