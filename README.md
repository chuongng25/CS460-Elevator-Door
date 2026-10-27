Basic elevator door sim. When we combine with the other groups, we will create an adapter that will hook into  group 3s bus

DOOR API: the five methods other teams use. Frozen by agreement.
DOORTWIN: the simulated hardware.
DOORGUI: mostly the panel. Commands the door through DOORAPI reads position and sets the sensors directly on the twin.
MAIN: headless self-test.

API
void    open();
void    close();
boolean isFullyOpen();
boolean isFullyClosed();
boolean hasObstruction();
Physical design
Door opening : 4 ft wide, 7 ft high, center opening
Leaf travel : 2 ft each 
Speed : 1.0 ft/s (0.305 m/s) 
Open/close time : 2.0 s 
Time step : 0.05s approx 
Constant velocity; acceleration is not modelled (but was mentioned maybe it should be?)
two sensors that p much do the same thing, one for above one for below.
and a weight sensor in the sill. Either one reverses a closing door. The
reversal is decided inside the twin with no message to anything else, so it
still works if the car's wireless link drops.

make sim     the GUI
make test    self-test
make clean
NEEDS JDK

What we need from 3 and the other team:
Team #03's bus is the integration path. Pub/sub, 8-byte messages, subtopic =
car number which is why DOORTWIN carries a `carId`. Nothing in DOORAPI,
the physics, or the panel changes:

bus.subscribe(id, "door/command", carId);
String cmd = bus.getMessage("door/command", carId);
if ("open".equals(cmd)) twin.open();
bus.publish(id, "door/state " + carId + " " + twin.getState());

Twin: Right now Twins speed for each door is 2ft/s. If you change the Door Travel to be bigger then it will be slower, increase the doorspeed its faster. The duration it takes for the doors to fully open/close is the 2 devided by eachter and is stored in Default Duration. The Enums at the top are what states the door can be in. the constructor gives each door and carID (as each door will be connected to an elevator) and then starts the ticker. The ticker creates a thread that basically runs a tick approx every 50ms and calls the update method. the important thing is we did not use a constant for this, so even though its almost always 0.05 approx, the doors open/close time will always be 2 seconds (even if the door moves at slightly different speeds). on start the loop goes and starts calling update. Basically, the update method sets teh states of the doors based on position (which is caculated using the system time in threads run). when opening, it adds to the position until the fraction position/Duration >= 1 which is fully open at which point the state is set to open (instead of opening). The same applies for closing, except you subtract until the ratio is <= 0 which means closed (changes from closing). If there is an obstruction and we are closing the doors, its immididiatly set to opening for the next tick and we exit the loop as to get to the next tick. The rest of the methods are mainly helpers for the GUI/Main.

Main: 5 test cases to show the program works without the GUI. The first verifies the doors start closed, meaning the position is <= 0 and the state is closed. The doors should always start closed so a pass if this works. The next test opens the doors and checks if they fully open. The next test begins to close them, and about a half second in obstructs the Light beam sensor at the top. If its not fully open after a sleep, it failed. The next test does the same for the Sill sensor. The final test closes the door fully and verifies they are actully fully closed.

GUI: The GUI basically checks for position using the animation timer to call UpdateDoorStatus() which on each run polls the position from the twin and multiplies that fraction by the amount of pixels to set the doors open/closed. The rest is just building our layout.

