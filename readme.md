# Team Mahogany - Tufts University

## Who

We help first-responders best evaluate terrain risks in post-disaster scenarios by iterating on current drone software/hardware and adding support for harmful radiation detection. Similar tech exists in a military setting, but we are developing an open-source software/hardware prototype for use by public organizations such as [Help.NGO - Global DIRT](http://globaldirt.org/).

## What

Our project is a prototype for a black-box attachment for a drone/other unmanned vehicle that reports standard image, thermal, and radiation data over radio to a radio receiver and displays the reported infomation in an intuitive user interface, in real time -- no internet required.

## Getting Started

[/docs](https://github.com/kdorosh/Capstone/tree/master/docs) contains documentation for the capstone project.

[/src](https://github.com/kdorosh/Capstone/tree/master/src) contains the git repos both researched and used for the project.

Download sample .osm maps [here](http://download.geofabrik.de/). Download the bz2-compressed (.osm.bz2) version. If you'll have an internet connection running the GUI, this step is unnecessary.

## The Hardware and Firmware

A GoPro HERO3+ Black Edition is used to record images and save them to a micro SD card. 

Code running on an Arduino Pro uses a master/slave configuration to toggle reading images from the microSD card and writing to the transmitter's buffer for transmission over RF69 using [RadioHead's](https://github.com/PaulStoffregen/RadioHead/tree/3d02f09670eb3880067e989998309dcfa2aa7a68) Arduino library.

The [Adafruit_GPS](https://github.com/adafruit/Adafruit_GPS/tree/77fe3484374837cecf2dd8387f3a62b1d5c832f9) GPS chip is also read by the Arduino Pro and sent to the transmitter. This chip is fairly cheap and only works reliably outdoors.

A similar circuit controls the Geiger-Müller counter, reporting data to the Arduino Pro in CPS (counts per second).

The thermal camera and circuit never reached a final prototype due to time constraints.

Due to limitations in the transmitter buffer size (60 bytes) we can only transmit small images in "real-time". A better transmitter would fix this problem.

A second Arduino Pro is connected to the RF69 receiver and a serial USB connector, designed to plug into a computer running our GUI.

[/src/Project](https://github.com/kdorosh/Capstone/tree/master/src/Project) contains some test code for basic radio transmission and the Team Mahogany code to flash both Arduino Pros.

* Team Mahogany Code
  * [Transmitter code](https://github.com/kdorosh/Capstone/blob/master/src/Project/tx/tx.ino) --> Gets flashed to the Arduino Pro attached to RF69 transmitter, GoPro, GPS, and Geiger-Müller circuits.
  * [Receiver code](https://github.com/kdorosh/Capstone/blob/master/src/Project/rx/rx.ino) --> Gets flashed to the second Arduino Pro, attached to the RF69 receiver and a serialport.
  * This [python script](https://github.com/kdorosh/Capstone/blob/master/src/Project/ComArduino.py) saves received transmissions to JPGs. This functionality is duplicated; The GUI has its own code to monitor the serial port and save images to disk.
  * The rest of the code is test code for the Arduino Pros.

## The GUI/Data Fusion Software

The GUI is built off of [josm](https://github.com/openstreetmap/josm), the java branch of the OpenStreetMap project. The source code for our version of josm can be found at [/src/josm](https://github.com/kdorosh/Capstone/tree/master/src/josm).

The OpenStreetMap project is dubbed "the free wiki world map". A collaborative project where anyone can contribute updates to free, editable world maps via image uploads/location tagging within tools such as [iD](https://github.com/kdorosh/Capstone/tree/master/src/iD) and [josm](https://github.com/openstreetmap/josm), it supports offline map overlays via its specialized ".osm" file format. Thus, josm became the perfect foundation for the GUI.

The GUI is responsible for reading image data, GPS data, and radiation data from the serialport and overlaying it an understandable format in real-time. By patching the incoming data into the JPG EXIF metadata before saving to disk, we have standardized our image data with JPG standards.

The data remains on disk for future use after program termination. The radiation and image overlays are separated into separate UI layers, using a heatmap to intuitively display radiation data. Simple clickable tags that can be cycled through and centered display the images themselves.

WebODM or a similar tool would have been ideal to take overlapping images and stitch them together. The GUI tool is designed with future improvements to data fusion in mind.

* Other tools researched but eventually unused/modifications abandoned
  * [Ardupilot](https://github.com/ArduPilot/ardupilot/tree/5646afac1cc029ef41934af9b5b9b4d00a87bcf6) - Open-source firmware to control many commercial drones.
  * [MissionPlanner](https://github.com/ArduPilot/MissionPlanner/tree/e0ccac239ae19d79b74bdb155547a69b3c530a7d) - Open-source mission planning tool that works with any drone running ardupilot firmware. The original plan was to integrate our information into MissionPlanner itself. We pivoted (early last semester) toward a black-box standalone solution as superior to decouple ourselves from specific drones, or drones at all.
  * [WebODM](https://github.com/kdorosh/Capstone/tree/master/src/WebODM) - A web-based tool for post-flight processing of drone imagery, built on top of [OpenDroneMap](https://github.com/OpenDroneMap/OpenDroneMap). The secondary plan was to leverage the image-stitching and clean UI base of WebODM for our uses, packagaing the web tool into a desktop application into a native desktop app using [Electron](https://github.com/electron/electron). This complex project suffered recent undocumented changes becoming more trouble than it was worth --> abandoned code pivoted to more standard desktop development using [josm](https://github.com/openstreetmap/josm) early this semester.
  * [iD](https://github.com/kdorosh/Capstone/tree/master/src/iD) - A web-based branch of the OpenStreetMap project briefly considered between WebODM and josm.

## Build and Run

**Build the Team Mahogany Arduino firmware**

Download the newest version of the [Arduino IDE](https://www.arduino.cc/en/Main/Software).

Verify and flash the transmitter code at `src/Project/tx` to the transmitter Arduino Pro.

Verify and flash the receiver code at `src/Project/rx` to the receiver Arduino Pro.

The Team Mahogany code uses Radiohead's and Adafruit_GPS's libraries. To compile our code you must clone the [RadioHead repo](https://github.com/PaulStoffregen/RadioHead/tree/3d02f09670eb3880067e989998309dcfa2aa7a68) and the [Adafruit_GPS repo](https://github.com/adafruit/Adafruit_GPS/tree/77fe3484374837cecf2dd8387f3a62b1d5c832f9) to your `Documents/Arduino/libraries` (Linux/OS X) or `My Documents/Arduino/libraries` (Windows) folder.

**Run the Team Mahogany Arduino firmware**

Once you have verified and compiled the code in the Arduino IDE, it automatically flashes to the chip and runs. The lights on the Arduino Mega's should light up and flash to signal that the code is running.

**Build the Team Mahogany GUI**

```
$ cd src/josm
$ ant dist
```

The executable jar is saved at `src/josm/dist/josm-custom.jar`.

Clean the josm project
```
$ cd src/josm
$ ant clean
```

[Set up an IntelliJ environment](https://josm.openstreetmap.de/wiki/DevelopersGuide/CompilingUsingIntelliJ) to change the code.

**Run the Team Mahogany GUI**

Run the generated `src\josm\dist\josm-custom.jar`. Click `File -> Open` to open downloaded .osm maps, if neeeded, for offline function (slow).

Click `Tools -> Run Disaster Response Simulation` to open run the simulated version of the GUI. Simulated data comes from your provided directory (choose `simulated_serialport_data` in repo root).

Click `Tools -> Run Disaster Response Software` to run the full version.

## Built With

* [RadioHead](https://github.com/PaulStoffregen/RadioHead/tree/3d02f09670eb3880067e989998309dcfa2aa7a68) - Open-source RF69 radio arduino firmware
* [Adafruit_GPS](https://github.com/adafruit/Adafruit_GPS/tree/77fe3484374837cecf2dd8387f3a62b1d5c832f9) - Open-source GPS chip firmware
* [JOSM](https://github.com/openstreetmap/josm) - Open-source java offshoot of OpenStreetMap project

## Future Work

In the future we would like to implement image stitching into the GUI, akin to node-OpenDroneMap's functionality. Additionally, a topographic-style map layer for radiation data would be interesting, if we had enough data.

Further, we would use a better transmitter for improved range and transmission speed. We would complete the thermal camera circuitry and work on sizing down the prototyped hardware into a smaller form-factor.

## Authors

This project would have been impossible without the tireless work of developers of the numerous open-sourced projects we leveraged (and chose not to).

Team Mahogany:
* **Kevin Dorosh**    - *CS: Spearheaded git repo & GUI*
* **Richard Preston** - *Comp E: Focused on firmware and the circuits*
* **Ryan Stocking**   - *EE: Killed it on the hardware design and implementation*
* **Elyse Cooper**    - *EE: Nailed the hardware implementation and team coordination*

## License

Adafruit_GPS is licensed under [BSD license](https://github.com/adafruit/Adafruit_GPS/blob/77fe3484374837cecf2dd8387f3a62b1d5c832f9/license.txt). 
RadioHead is licensed under [GPL v2](https://github.com/PaulStoffregen/RadioHead/blob/3d02f09670eb3880067e989998309dcfa2aa7a68/LICENSE). 
JOSM is licensed under the [GPL v2](http://www.gnu.org/licenses/old-licenses/gpl-2.0.html) and [GPL v3](http://www.gnu.org/licenses/gpl.html). 

Thus, this project is similarly licensed under the GPL v3 and BSD licenses.

## Acknowledgments

* Hat tip again to open-sourced developers everywhere.
* Thanks to the Tufts EE and CS departments for their support, hardware ($$$!), and communication on this project.
* Thanks to Victor (Tufts graduate student) for taking the time to catch us up on the project and flying the drone.
* Special thanks to our sponsor, Dr. Karen Panetta, for her support of us and the project.
* Special thanks to our professors, Prof. Lasser and Prof. Guyer, for their continued feedback on the project.