#include <SoftwareSerial.h>

SoftwareSerial mySerial(11,12); // RX, TX

const int kDPinActivity = 2;
const int kDPinLight = 13;
const int kAPinBright = 5;

// Commands strings from remote controller
const String kCmdLightOn = "light on";
const String kCmdLightOff = "light off";
const String kCmdSetPrefix = "set:";

// Status string
const String kCmdPrefix = "command:";
const String kStatusActivityDetected = "activity detected";

const char kPacketDelimiter = '#';

// The base brightness to decide day.
int min_brightness_of_day = 10;
int max_brightness_of_night = 5;

void setup() {
  pinMode(kDPinLight, OUTPUT);
  pinMode(kDPinActivity, INPUT);
  pinMode(kAPinBright, INPUT);
  
  digitalWrite(kDPinLight, HIGH); // Trun off
  
  Serial.begin(9600);
  Serial.println("Start");
  mySerial.begin(9600);
}

bool needLightOn(int activity, int brightness) {
  // the lower the value is, the brighter.
  if (brightness > max_brightness_of_night) {
    // during the day
    return false;
  }
  if (activity) {
    return true;
  }
  return false;
}

bool needLightOff(int brightness) {
  if (brightness > min_brightness_of_day) {
    return true;
  }
  return false;
}

// command structure 
// [command name]:[value]
void processSettingCmd(String cmd) {
  const char* kBrightnessDayValue = "brightness day:";
    const char* kBrightnessNightValue = "brightness night:";
  
  if (cmd.startsWith(kBrightnessNightValue)) {
    String value = cmd.substring(String(kBrightnessNightValue).length());
    max_brightness_of_night = value.toInt();
  } else if (cmd.startsWith(kBrightnessDayValue)) {
    String value = cmd.substring(String(kBrightnessDayValue).length());
    min_brightness_of_day = value.toInt();
  }
}

void processCmd(String cmd) {
  if (cmd.equals(kCmdLightOn)) {
    digitalWrite(kDPinLight, LOW); // Turn on
    Serial.println(kCmdLightOn);
  } else if (cmd.equals(kCmdLightOff)) {
    digitalWrite(kDPinLight, HIGH); // Turn off
    Serial.println(kCmdLightOff);
  } else if (cmd.startsWith(kCmdSetPrefix)) {
    processSettingCmd(cmd.substring(kCmdSetPrefix.length()));
  }
}

// Push partial string of command
// and return a command when there is a command completed
String pushCommandFragment(String fragment) {
  static String buffer;
  
  buffer += fragment;
  int idx = buffer.indexOf(kPacketDelimiter);
  if (idx == -1) {
    return "";
  }
  
  String complete_cmd = buffer.substring(0, idx);
  if (buffer.length() > idx + 1) { 
    buffer = buffer.substring(idx + 1);
  } else {
    buffer = "";
  }
  
  return complete_cmd;
}

// check activity is detected 
// and create a response packet if response is needed.
String checkActicityAndCreatePacket(int activity) {
  static int last_status = 0;
  // prevent creating duplicated packets.
  if (last_status == activity) {
    return "";
  }
  last_status = activity;
  
  if (!activity) {
    return "";
  }
  
  String packet = kStatusActivityDetected;
  packet += kPacketDelimiter;
  return packet;
}

// The loop function runs over and over again forever
void loop() {
  String send_packet = "";
  
  int activity = digitalRead(kDPinActivity);
  send_packet += checkActicityAndCreatePacket(activity);
  //Serial.print("activity: ");
  //Serial.println(activity);
  
  int brightness = analogRead(kAPinBright);
  //Serial.print("bright: ");
  //Serial.println(brightness);
  
  // Parse command from remote controller
  String cmd_fragment = "";
  while (mySerial.available()) {
    char c = mySerial.read();
    cmd_fragment.concat(c);
    delay(1);
  }

  while(1) {
    // Combine the fragment with the ones recieved before.
    String cmd = pushCommandFragment(cmd_fragment);
    if (cmd.length() == 0) {
      break;
    }
    
    processCmd(cmd);
    Serial.print("command in: ");
    Serial.println(cmd);
    
    // run more loop with empty fragment
    // in case that there are more packets available.
    cmd_fragment = "";
  }
  
  if (Serial.available())
    mySerial.write(Serial.read());

  // send packet if there's one to send.
  if (send_packet.length() > 0) {
    mySerial.print(send_packet);
    //Serial.println(send_packet);
  }
}

