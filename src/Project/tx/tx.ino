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
// #include <SoftwareSerial.h> // required when using the pro mini

/************ Constant Definitions ************/

#define RF69_FREQ     915.0 // use 915.0 MHz

#define DEST_ADDRESS  1 // where to send packets to
#define MY_ADDRESS    2 // ...and where to receive them

#define RFM69_INT     2 // RFM69
#define RFM69_CS      4 // interface
#define RFM69_RST     5 // ports
/* For use with pro mini:
#define GPS_TX        6 // GPS interface
#define GPS_RX        7 // ports
*/
#define GPSSerial Serial1 // Pins 18 and 19 on mega

#define SD_CS         10 // SD card module
#define SD_CD         9  // interface ports

#define RD_PIN        1   // interrupt pin for radiation detection
#define RD_PERIOD     5e3 // ms between CPS updates
#define MINUTE        6e4 // ms

#define GPSECHO       true  // specify whether to echo the GPS data to console
                            // change to false in production

#define WAITSER       true  // specify whether to wait for serial monitor
                            // change to false in production

/************ Variable Declarations ************/

// SoftwareSerial GPSSerial(GPS_RX,GPS_TX); // required when using the pro mini

RH_RF69 rf69(RFM69_CS, RFM69_INT); // radio driver instance

// Class to manage message delivery and receipt, using the driver declared above
RHReliableDatagram rf69_manager(rf69, MY_ADDRESS);

Adafruit_GPS GPS(&GPSSerial); // GPS driver instance
char lat[11] = "";        // store
char lon[11] = "";        // GPS
char timestamp[22] = "";  // data
char altitude[10] = "";   // strings

unsigned long previousMillis = 0; // timestamp
volatile unsigned int pulseCount = 0; // number of radiation pulses
unsigned int CPM = 0; // most recent CPM value

File image;                     // SD
unsigned int imageNumber = 1;   // card
char filename[20];              // stuff

char radioPacket[RH_RF69_MAX_MESSAGE_LEN]; // single packet for transmission

void EnableTimerInterrupt(); // function prototype

/************ Initialization Routine ************/

void setup() 
{
  // Open serial communications
  Serial.begin(115200);
  if (WAITSER) {
    while (!Serial) {} // wait for port to open
  }

  // Setup pins
  pinMode(RFM69_RST, OUTPUT);
  digitalWrite(RFM69_RST, LOW);
  pinMode(SD_CD, INPUT);

  // Setup GPS
  GPS.begin(9600);
  GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_RMCONLY);
  GPS.sendCommand(PMTK_SET_NMEA_UPDATE_1HZ);
  EnableTimerInterrupt();
  
  // Setup SD card
  Serial.print("Initializing SD card...");
  if (!digitalRead(SD_CD)) {
    Serial.println("ERROR: No card detected");
  } else if (!SD.begin(SD_CS)) {
    Serial.println("ERROR: Could not initialize");
  } else {
    Serial.println("success");
  }

  // Setup radio
  Serial.print("Initializing radio...");
  // Manually reset, then initialize
  digitalWrite(RFM69_RST, HIGH);
  delay(10);
  digitalWrite(RFM69_RST, LOW);
  delay(10);
  if (!rf69_manager.init()) {
    Serial.println("ERROR: Could not initialize");
  } else if (!rf69.setFrequency(RF69_FREQ)) {
    Serial.println("ERROR: setFrequency failed");
  } else {
    // Set power and AES key of radio
    rf69.setTxPower(20);
    uint8_t key[] = { 0x1c, 0xef, 0xb2, 0x33, 0xe9, 0x0b, 0xf2, 0x3c,
                      0xb7, 0xee, 0x23, 0x3f, 0xb0, 0x88, 0xa6, 0xcd };
    rf69.setEncryptionKey(key);
    Serial.println("success");
  }
  
  // Setup ISR for radiation module
  attachInterrupt(RD_PIN, RadDetect, RISING);

  UpdateCPM();
  UpdateGPS(); 
}

/************ Main Loop ************/

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

/************ Function Definitions ************/

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

void TransmitData() {
// Send all the data, including the CPM, GPS, and image
  Serial.print("Transmitting ");
  Serial.println(filename);

  // Send the CPM and GPS coordinates
  sprintf(radioPacket, "Timestamp: %s\nLocation: %s,%s\nAltitude: %s\nCPM: %u", timestamp,lat,lon,altitude,CPM);
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
