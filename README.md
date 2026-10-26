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

