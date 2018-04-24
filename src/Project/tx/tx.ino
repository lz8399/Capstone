/*
 * tx.ino
 * Controls the transmission module for Team Mahogany's Senior Design Project
 * For use with Arduino Mega
 */

#include <SPI.h>
#include <RH_RF69.h>
#include <RHReliableDatagram.h>
#include <SD.h>
#include <Adafruit_GPS.h>

/************ Constant Definitions ************/

// Transmitter Constants
#define RF69_FREQ     915.0 // use 915.0 MHz

#define DEST_ADDRESS  1 // where to send packets to
#define MY_ADDRESS    2 // ...and where to receive them

#define RFM69_INT     2 // RFM69
#define RFM69_CS      4 // interface
#define RFM69_RST     5 // ports

// GPS Constants
#define GPSSerial     Serial1 // Pins 18 and 19 on mega
#define GPSECHO       false   // specify whether to echo the GPS data to console

// Radiation Module Constants
#define RD_PIN        1   // interrupt pin for radiation detection
#define RD_PERIOD     5e3 // ms between CPS updates
#define MINUTE        6e4 // ms

// File I/O Constants
#define SD_PIN        53

// Others
#define WAITSER       false // specify whether to wait for serial monitor

/************ Variable Declarations ************/

// Transmitter Variables
RH_RF69 rf69(RFM69_CS, RFM69_INT);                  // radio driver instance
RHReliableDatagram rf69_manager(rf69, MY_ADDRESS);  // class to manage transmission
uint8_t radioPacket[RH_RF69_MAX_MESSAGE_LEN];       // single packet for transmission
uint16_t timeout = 100; // ms

// GPS Variables
Adafruit_GPS GPS(&GPSSerial); // GPS driver instance
char lat[11] = "";        // store
char lon[11] = "";        // GPS
char timestamp[22] = "";  // data
char altitude[10] = "";   // strings

// Radiation Module Variables
unsigned long previousMillis = 0;     // timestamp
volatile unsigned int pulseCount = 0; // number of radiation pulses
unsigned int CPM = 0;                 // most recent CPM value
void EnableTimerInterrupt(); // function prototype

// File I/O Variables
File image;
char filename[20] = "JPEG_123.JPG";

/************ Initialization Routine ************/

void setup() {
  
  // Open serial communications
  Serial.begin(115200);
  if (WAITSER) {
    while (!Serial) {} // wait for port to open
  }

  // Set up hardware
  SetupPins();
  SetupSD();
  SetupRadio();
  SetupGPS();
  
  // Setup ISR for radiation module
  attachInterrupt(RD_PIN, RadDetect, RISING);
  
}

/************ Main Loop ************/

void loop() {

  UpdateCPM();
  if (UpdateGPS()) {
    TransmitStats();
  }
  
  // Try to read an image from the SD card
  EnableSD();
  image = SD.open(filename);

  // If successful, transmit the data and close the file
  if (image) {
    TransmitImage();
    image.close();
  }

}

/************ Function Definitions ************/

void SetupPins() {
  //Multiplexer Pins
  pinMode(52, OUTPUT);
  pinMode(50, OUTPUT);
  pinMode(48, OUTPUT);

  //CS Pins
  pinMode(53, OUTPUT);
  pinMode(49, OUTPUT);

  //Demux pins
  pinMode(46, OUTPUT);
  pinMode(44, OUTPUT);

  //Other pins
  pinMode(13, OUTPUT);     
  pinMode(RFM69_RST, OUTPUT);
}

void EnableRadio() {
  // CS
  digitalWrite(53, HIGH);
  digitalWrite(49, LOW);

  // Demux
  digitalWrite(44, HIGH);
  digitalWrite(46, HIGH);

  // Mux
  digitalWrite(52, LOW);
  digitalWrite(50, LOW);
  digitalWrite(48, HIGH);

  // Others
  digitalWrite(RFM69_RST, LOW);
}

void SetupRadio() {
  Serial.print("Initializing radio...");

  EnableRadio();
  
  // Manually reset, then initialize
  digitalWrite(RFM69_RST, HIGH);
  delay(10);
  digitalWrite(RFM69_RST, LOW);
  delay(10);
  if (!rf69_manager.init()) {
    Serial.println("ERROR: Could not initialize"); return;
  }
  if (!rf69.setFrequency(RF69_FREQ)) {
    Serial.println("ERROR: setFrequency failed"); return;
  }
  
  // Set power, AES key, and timeout of radio
  rf69.setTxPower(20);
  uint8_t key[] = { 0x1c, 0xef, 0xb2, 0x33, 0xe9, 0x0b, 0xf2, 0x3c,
                    0xb7, 0xee, 0x23, 0x3f, 0xb0, 0x88, 0xa6, 0xcd };
  rf69.setEncryptionKey(key);
  rf69_manager.setTimeout(timeout);
  Serial.println("success");
}

void EnableSD() {
  // CS
  digitalWrite(49, HIGH);
  digitalWrite(53, LOW);

  // Demux
  digitalWrite(44, LOW);
  digitalWrite(46, LOW);

  // Mux
  digitalWrite(52, LOW);
  digitalWrite(50, LOW);
  digitalWrite(48, LOW);
}

void SetupSD() {
  Serial.print("Initializing SD card...");

  EnableSD();
  
  if (!SD.begin(SD_PIN)) {
    Serial.println("FAILED");
    return;
  }
  Serial.println("success");
}

void SetupGPS() {
  GPS.begin(9600);
  GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_RMCONLY);
  GPS.sendCommand(PMTK_SET_NMEA_UPDATE_1HZ);
  EnableTimerInterrupt();
}

SIGNAL(TIMER0_COMPA_vect) {
// Look for any new GPS data and store it once a millisecond
  char c = GPS.read();

#ifdef UDR0
  if (GPSECHO) // print out the data if desired
    if (c) UDR0 = c;  
#endif
}

void EnableTimerInterrupt() {
// Sets up the TIMER0_COMPA_vect interrupt to trigger once every ms
  OCR0A = 0xAF;
  TIMSK0 |= _BV(OCIE0A);
}

bool TransmitStats() {
// Send the CPM and GPS data; return true if ack is received, false otherwise
  Serial.println("Transmitting Stats");
  
  EnableRadio();
  
  char radioPacket[RH_RF69_MAX_MESSAGE_LEN];
  sprintf(radioPacket, "Timestamp: %s\nLocation: %s,%s\nAltitude: %s\nCPM: %u", timestamp,lat,lon,altitude,CPM);
  Serial.println(radioPacket);
  
  return !rf69_manager.sendtoWait((uint8_t *)radioPacket, strlen(radioPacket), DEST_ADDRESS);
}

int TransmitImage() {
// Send the image; returns the number of lost packets
  Serial.print("Transmitting ");
  Serial.println(filename);

  EnableRadio();

  uint8_t radioPacket[RH_RF69_MAX_MESSAGE_LEN];
  int numPackets = ceil(image.available()/RH_RF69_MAX_MESSAGE_LEN);
  int lostPackets = 0;
  
  for (int i = 0; i<numPackets; i++) {
    image.read(radioPacket,RH_RF69_MAX_MESSAGE_LEN);
    if (!rf69_manager.sendtoWait(radioPacket, RH_RF69_MAX_MESSAGE_LEN, DEST_ADDRESS)) {
      lostPackets++;
    }
  }
}

void TransmitPacket(uint8_t len) {
// Send a single packet

  // print the packet to the console
  Serial.write(radioPacket,len);
  Serial.println();
  Serial.print("Awaiting acknowledgdement...");
  if (!rf69_manager.sendtoWait(radioPacket, len, DEST_ADDRESS)) {
    Serial.println("FAILED");
    return;
  }
  Serial.println("success");
}

bool UpdateGPS() {
// Update the GPS coordinates if a new sentence is received
// Returns true if updated, false if not
  if (GPS.newNMEAreceived()) {
    if (!GPS.parse(GPS.lastNMEA())) return false;
  }
  if (!GPS.fix) {
    return false;
  }

  // values have been updated; prepare them for transmission
  dtostrf(GPS.latitude,9,4,lat);
  lat[9] = GPS.lat;
  lat[10] = 0;
  dtostrf(GPS.longitude,9,4,lon);
  lon[9] = GPS.lon;
  lon[10] = 0;
  
  dtostrf(GPS.altitude,9,2,altitude);

  // date in dd/mm/yyyy format
  sprintf(timestamp, "%d/%d/20%d %d:%d:%d.%d", GPS.day,GPS.month,GPS.year,GPS.hour,GPS.minute,GPS.seconds,GPS.milliseconds);
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
