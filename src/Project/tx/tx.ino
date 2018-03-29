/*
 * tx.ino
 * Controls the transmission module for Team Mahogany's Senior Design Project
 */

#include <SPI.h>
#include <RH_RF69.h>
#include <RHReliableDatagram.h>
#include <SD.h>

/************ Radio Setup ***************/

// Use 915.0 MHz
#define RF69_FREQ 915.0

// Where to send packets to
#define DEST_ADDRESS   1
// ...and where to receive them
#define MY_ADDRESS     2

// Setup RFM69 interface ports
#define RFM69_INT     2 
#define RFM69_CS      4
#define RFM69_RST     5  // changed from 2 in example code
#define LED           13

// Specify whether or not to print to serial monitor
#define DEVMODE       1  // change to 0 in production

// Singleton instance of the radio driver
RH_RF69 rf69(RFM69_CS, RFM69_INT);

// Class to manage message delivery and receipt, using the driver declared above
RHReliableDatagram rf69_manager(rf69, MY_ADDRESS);

/************ General Variable Declaration ***************/

int16_t packetnum = 0;  // packet counter, we increment per xmission

#define MainPeriod 1000 // time period to count 

long previousMillis = 0; // will store last time of the cycle end
volatile unsigned long duration=0; // accumulates pulse width
volatile unsigned long int pulsecount=0;
volatile unsigned long previousMicros=0;
int Frequency = 0;
volatile unsigned long int newcount = 0;
int input = 3;
int interruptPin = 1;
int i = 0; //This is my test variable for message transmission

// TODO: eliminate the use of strings in the code
String cps;
String message;

/************ FileI/O Variable Declaration ***************/
int SDpin = 10;
File myFile;


void setup() 
{
  // Open serial communications
  Serial.begin(115200);
  if (DEVMODE) {
    while (!Serial) { delay(1); } // wait for port to open
  }

  // Setup SD card
  Serial.print("Initializing SD card...");
  if (!SD.begin(SDpin)) {
    Serial.println("FAILED");
    return;
  }
  Serial.println("success");

  // Setup radio
  pinMode(LED, OUTPUT);     
  pinMode(RFM69_RST, OUTPUT);
  digitalWrite(RFM69_RST, LOW);

  Serial.print("Initializing radio...");
  // manual reset
  digitalWrite(RFM69_RST, HIGH);
  delay(10);
  digitalWrite(RFM69_RST, LOW);
  delay(10);
  
  if (!rf69_manager.init()) {
    Serial.println("FAILED");
    return;
  }
  Serial.println("success");

  if (!rf69.setFrequency(RF69_FREQ)) {
    Serial.println("ERROR: setFrequency failed");
  }

  rf69.setTxPower(20);

  uint8_t key[] = { 0x1c, 0xef, 0xb2, 0x33, 0xe9, 0x0b, 0xf2, 0x3c,
                    0xb7, 0xee, 0x23, 0x3f, 0xb0, 0x88, 0xa6, 0xcd };
  rf69.setEncryptionKey(key);

  // Setup ISR for radiation module
  attachInterrupt(interruptPin, radiationdetect, RISING); 
}

// Non-stack variables
uint8_t radiopacket[RH_RF69_MAX_MESSAGE_LEN];

void loop() {
  
  // 1) Wait in a while loop while we are looking for the next file
  // TODO: Investigate if this while loop could hog control from the rest of the program
  char fileString[50] = {0};
  sprintf(fileString, "JPEG_%d.JPG", i);
  //should be preset to whatever file we are expecting to start with from the GoPro

  myFile = SD.open(fileString);
  while (!myFile){
    myFile = SD.open(fileString);
  }
  
  // 2) TODO: Access and Store the GPS value

  // 3) Access and Store the CPS value
  // TODO: Rewrite this function.
  countsperminute(); // call function that calculates the number of counts detected

  // 4) Transmit the GPS and CPS values
  message = cps; // message to be transmitted 
  Serial.println(message);
  message.toCharArray(radiopacket,RH_RF69_MAX_MESSAGE_LEN);
  moduletx(); //Transmit

  // 5) Read/Transmit the image file in chunks
  if (myFile) {
    Serial.println("TestText.txt:"); // print for local verification

    int bytes_available = myFile.available(); // total available bytes in file
    int num_packets = floor(bytes_available/60); // amount of 60 byte packets to process
    char dataString[60] = {0};
    
    for(int i = 0; i<val2; i++){
      byte imagePacket[60] = {0};
      myFile.read(imagePacket,60);
      String packet1;
      for (int j=0; j <= 59 ; j++){
          sprintf(dataString,"%02X",imagePacket[j]);
          String myString = String(dataString);
          packet1 = packet1+myString;
      }
      Serial.print(packet1);
      message = packet1;
      message.toCharArray(radiopacket,64);
      moduletx();
    }
    
    int val3 = val1-(val2*60);
    char dataString2[val3] = {0};
    
    byte imagePacket2[val3] = {0};
    myFile.read(imagePacket2,val3);
    String packet1;
    for (int j=0; j < val3 ; j++){
      sprintf(dataString2,"%02X",imagePacket2[j]);
      String myString = String(dataString2);
      packet1 = packet1+myString;
    }
    Serial.print(packet1);
    message = packet1;
    message.toCharArray(radiopacket,64);
    moduletx();
   
    myFile.close();

  } else {
    // if the file didn't open, print an error:
    Serial.println("error opening test.txt");
  }


  //6. Final Steps
  //delay(1000); //Time to sleep
  i = i + 1;
  
}

/* FUNCTION DISABLED DUE TO NOT BEING USED. SHOULD BE REMOVED IN PRODUCTION 
void Blink(byte PIN, byte DELAY_MS, byte loops) {
  for (byte i=0; i<loops; i++)  {
    digitalWrite(PIN,HIGH);
    delay(DELAY_MS);
    digitalWrite(PIN,LOW);
    delay(DELAY_MS);
  }
}
*/
void TransmitPacket(uint8_t * radiopacket) {
// Send a single packet

  Serial.print("Sending packet. Awaiting acknowledgdement...");
  if (!rf69_manager.sendtoWait(radiopacket, sizeof(radiopacket), DEST_ADDRESS)) {
    Serial.println("FAILED");
    return;
  }
  Serial.println("success");
}

void countsperminute() {
 unsigned long currentMillis = millis();
    if (currentMillis - previousMillis >= MainPeriod) 
    { 
      previousMillis = currentMillis;   
      // need to bufferize to avoid glitches
      unsigned long _duration = duration;
      unsigned long _pulsecount = pulsecount;
      newcount = pulsecount;
      duration = 0; // clear counters
      pulsecount = 0; //initialize count
      float Freq = 1e6 / float(_duration);    //Duration is in uSecond so it is 1e6 / T (You could also check the pulsein arduino function)

      Freq *= _pulsecount; // calculate F
      Frequency = int(Freq); //Ensure the output produced in an integer
      if(Frequency <= 0) Frequency = 0;
      if(Frequency != 0) Frequency = Frequency + 1; //Make up for any initial delays by adding a 1 to count
      cps = String(newcount);
      //Serial.print("Counts/s: ");
      //Serial.print(cpm);
    }
    //delay(10);
}

void radiationdetect() // interrupt handler for detecting counting number of completed cycles edge
{
  unsigned long currentMicros = micros();
  duration += currentMicros - previousMicros;
  previousMicros = currentMicros;
  pulsecount++;
}
