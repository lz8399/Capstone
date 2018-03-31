/*
 * tx.ino
 * Controls the transmission module for Team Mahogany's Senior Design Project
 */

#include <SPI.h>
#include <RH_RF69.h>
#include <RHReliableDatagram.h>
#include <SD.h>
#include <Adafruit_GPS.h>
#include <SoftwareSerial.h>

/************ Constant Definitions ************/

#define RF69_FREQ     915.0 // use 915.0 MHz

#define DEST_ADDRESS  1 // where to send packets to
#define MY_ADDRESS    2 // ...and where to receive them

#define RFM69_INT     2 // RFM69
#define RFM69_CS      4 // interface
#define RFM69_RST     5 // ports

#define GPS_TX        8 // GPS interface
#define GPS_RX        9 // ports

#define SD_PIN        10 // SD card module interface port

#define RD_PIN        1   // interrupt pin for radiation detection
#define RD_PERIOD     5e3 // ms between CPS updates
#define MINUTE        6e4 // ms

#define DEVMODE       1 // specify whether to wait for serial monitor
                        // change to 0 in production

/************ Variable Declarations ************/

RH_RF69 rf69(RFM69_CS, RFM69_INT); // radio driver instance

// Class to manage message delivery and receipt, using the driver declared above
RHReliableDatagram rf69_manager(rf69, MY_ADDRESS);

SoftwareSerial GPSSerial(GPS_RX, GPS_TX); // setup serial port
Adafruit_GPS GPS(&GPSSerial); // GPS driver instance

unsigned long previousMillis = 0;     // timestamp
volatile unsigned int pulseCount = 0; // number of radiation pulses
unsigned int CPM = 0;                 // most recent CPM value

File image;
char radioPacket[RH_RF69_MAX_MESSAGE_LEN];
unsigned int imageNumber = 1;
char filename[20];

void setup() 
{
  // Open serial communications
  Serial.begin(115200);
  if (DEVMODE) {
    while (!Serial) { delay(1); } // wait for port to open
  }

  // Setup GPS
  Serial.print("Initializing GPS...");
  GPS.begin(9600);
  GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_RMCONLY);
  GPS.sendCommand(PMTK_SET_NMEA_UPDATE_1HZ);
  GPS.standby();
  if (!GPS.wakeup()) {
    Serial.println("FAILED");
    return;
  }
  Serial.println("success");

  // Setup SD card
  Serial.print("Initializing SD card...");
  if (!SD.begin(SD_PIN)) {
    Serial.println("FAILED");
    return;
  }
  Serial.println("success");

  // Setup radio pins
  pinMode(RFM69_RST, OUTPUT);
  digitalWrite(RFM69_RST, LOW);
  
  Serial.print("Initializing radio...");
  // Manually reset, then initialize
  digitalWrite(RFM69_RST, HIGH);
  delay(10);
  digitalWrite(RFM69_RST, LOW);
  delay(10);
  if (!rf69_manager.init()) {
    Serial.println("FAILED");
    return;
  }
  Serial.println("success");

  // Set frequency, power, and AES key of radio
  if (!rf69.setFrequency(RF69_FREQ)) {
    Serial.println("ERROR: setFrequency failed");
  }
  rf69.setTxPower(20);
  uint8_t key[] = { 0x1c, 0xef, 0xb2, 0x33, 0xe9, 0x0b, 0xf2, 0x3c,
                    0xb7, 0xee, 0x23, 0x3f, 0xb0, 0x88, 0xa6, 0xcd };
  rf69.setEncryptionKey(key);

  // Setup ISR for radiation module
  attachInterrupt(RD_PIN, RadDetect, RISING);

  UpdateCPM();
  UpdateGPS(); 
}

void loop() {

  // Attempt to open the image file
  sprintf(filename, "JPEG_%d.JPG", imageNumber);
  image = SD.open(filename);

  // If successful, transmit the data and close the file
  if (image) {
    TransmitData();
    image.close();
  }

  UpdateCPM();
  UpdateGPS();
}

void TransmitData() {
// Send all the data, including the CPM, GPS, and image

  Serial.print("Transmitting ");
  Serial.println(filename);

  // Send the CPM and GPS coordinates
  sprintf(radioPacket, "Location: %f,%f\nCPM: %u", GPS.latitude, GPS.longitude, CPM);
  TransmitPacket();

  // Now send the image
  int numPackets = ceil(image.available()/RH_RF69_MAX_MESSAGE_LEN);
  for (int i = 0; i<numPackets; i++) {
    image.read(radioPacket,RH_RF69_MAX_MESSAGE_LEN);
    TransmitPacket();
  }
}

void TransmitPacket() {
// Send a single packet

  Serial.println("Sending packet:");
  Serial.println(radioPacket);
  Serial.print("Awaiting acknowledgdement...");
  if (!rf69_manager.sendtoWait((uint8_t *)radioPacket, strlen(radioPacket), DEST_ADDRESS)) {
    Serial.println("FAILED");
    return;
  }
  Serial.println("success");
}

void UpdateGPS() {
// Update the GPS coordinates if a new sentence is received

  GPS.read();
  if (GPS.newNMEAreceived()) {
    GPS.parse(GPS.lastNMEA());
  }
}

void UpdateCPM() {
// Update the CPM and reset pulsecount if period has been reached

  unsigned long currentMillis = millis();
  unsigned long interval = currentMillis - previousMillis;
  if (interval >= RD_PERIOD) {
    CPM = pulseCount * (MINUTE / (int)interval);
    pulseCount = 0;
  }
  previousMillis = currentMillis;  
}

void RadDetect() {
// ISR called when radiation is detetected; counts number of pulses

  pulseCount++;
}
