#include <SPI.h>
#include <RH_RF69.h>
#include <SD.h>

/************ Radio Setup ***************/

// Change to 434.0 or other frequency, must match RX's freq!
#define RF69_FREQ 915.0

#if defined (__AVR_ATmega328P__)  // Feather 328P w/wing
  #define RFM69_INT     2  // 
  #define RFM69_CS      4  //
  #define RFM69_RST     5  // "A" changed from 2
  #define LED           13
#endif

// Singleton instance of the radio driver
RH_RF69 rf69(RFM69_CS, RFM69_INT);

/************ Radio Setup ***************/

/************ General Variable Declaration ***************/

int16_t packetnum = 0;  // packet counter, we increment per xmission
 
char radiopacket[64]; 
 
//RFM69 radio = RFM69(RFM69_CS, RFM69_IRQ, IS_RFM69HCW, RFM69_IRQN);

#define MainPeriod 1000 //Time period to count 

long previousMillis = 0; // will store last time of the cycle end
volatile unsigned long duration=0; // accumulates pulse width
volatile unsigned long int pulsecount=0;
volatile unsigned long previousMicros=0;
int Frequency = 0;
volatile unsigned long int newcount = 0;
int input = 3;
int interruptPin = 1;
int i = 0; //This is my test variable for message transmission

String cps;
String message;
String counts = "CPS: ";

/************ General Variable Declaration ***************/

/************ FileI/O Variable Declaration ***************/
int SDpin = 10;
File myFile;

/************ FileI/O Variable Declaration ***************/

void setup() 
{
  Serial.begin(115200);
  // Open serial communications and wait for port to open:

//setup for the SD portion
  while (!Serial) {
    ; // wait for serial port to connect. Needed for native USB port only
  }
  Serial.print("Initializing SD card...");
  if (!SD.begin(SDpin)) {
    Serial.println("initialization failed!");
    return;
  }
  Serial.println("initialization done.");
  //Above code is for connection purposes only and has nothing to do with the transmission
//end setup for SD portion

//setup for the radio portion
  pinMode(LED, OUTPUT);     
  pinMode(RFM69_RST, OUTPUT);
  digitalWrite(RFM69_RST, LOW);

  // manual reset
  digitalWrite(RFM69_RST, HIGH);
  delay(10);
  digitalWrite(RFM69_RST, LOW);
  delay(10);
  
  if (!rf69.init()) {
    Serial.println("RFM69 radio init failed");
    while (1);
  }
  Serial.println("RFM69 radio init OK!");
  // Defaults after init are 434.0MHz, modulation GFSK_Rb250Fd250, +13dbM (for low power module)
  // No encryption
  if (!rf69.setFrequency(RF69_FREQ)) {
    Serial.println("setFrequency failed");
  }

  // If you are using a high power RF69 eg RFM69HW, you *must* set a Tx power with the
  // ishighpowermodule flag set like this:
  rf69.setTxPower(20);  // range from 14-20 for power, 2nd arg must be true for 69HCW

  // The encryption key has to be the same as the one in the server
  uint8_t key[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                    0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
  rf69.setEncryptionKey(key);
  
  pinMode(LED, OUTPUT);

  Serial.print("RFM69 radio @");  Serial.print((int)RF69_FREQ);  Serial.println(" MHz");

  attachInterrupt(interruptPin, radiationdetect,RISING); 
}


void loop() {
  
  //(1). Wait in a while loop while we are looking for the next file
  char fileString[50] = {0};
  sprintf(fileString, "JPEG_%d.JPG", i);
  //should be preset to whatever file we are expecting to start with from the GoPro
  
  myFile = SD.open(fileString);
  while (!myFile){
    myFile = SD.open(fileString);
  }
  
  //(2). Access and Store the GPS value

  //3. Access and Store the CPS value
  countsperminute(); //Call function that calculates the number of counts detected
  Serial.print("\n");

  //(4). Transmit the GPS and CPS values
  message = counts +  cps; //Message to be transmitted 
  Serial.println(message);
  message.toCharArray(radiopacket,64);
  moduletx(); //Transmit

  //5. Read/Transmit the image file in chunks
  if (myFile) {
    Serial.println("TestText.txt:");//print for local verification

    int val1 = myFile.available();//total available bytes in file
    int val2 = floor(val1/60);//amount of 60 byte packets to process
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

void Blink(byte PIN, byte DELAY_MS, byte loops) {
  for (byte i=0; i<loops; i++)  {
    digitalWrite(PIN,HIGH);
    delay(DELAY_MS);
    digitalWrite(PIN,LOW);
    delay(DELAY_MS);
  }
}

void moduletx() {
  //Serial.print("Sending "); Serial.println(radiopacket);
  
  // Send a message!
  rf69.send((uint8_t *)radiopacket, strlen(radiopacket));
  rf69.waitPacketSent();

  // Now wait for a reply
  uint8_t buf[RH_RF69_MAX_MESSAGE_LEN];
  uint8_t len = sizeof(buf);

  if (rf69.waitAvailableTimeout(100))  { 
    // Should be a reply message for us now   
    if (rf69.recv(buf, &len)) {
      //Serial.print("Got a reply: ");
      Serial.println((char*)buf);
      Blink(LED, 20, 3); //blink LED 3 times, 50ms between blinks
    } else {
      Serial.println("Receive failed");
    }
  } //else {
    //Serial.println("No reply, is another RFM69 listening?");
  //}
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
