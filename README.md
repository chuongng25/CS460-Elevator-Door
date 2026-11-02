# CS460 Project 2: Elevator Door Assembly (DOR)

**CS 460 Software Engineering — University of New Mexico (Team #01)**

---

## 1. Architecture & Flat File Structure

The project strictly follows the **Digital Twin (DTW)** hardware abstraction pattern in a clean, flat 7-file layout:

```text
src/
├── DoorApi.java       # Contract: The 5 agreed-upon door control & inquiry APIs
├── DoorBus.java       # Software Bus message format (8 bytes) & door topic numbers (no networking)
├── DoorManager.java   # The brain: all door rules; controls the door only through DoorApi
├── DoorDash.java      # Transport: the one socket to the T03 Software Bus, delivers messages both ways
├── DoorTwin.java      # Hardware Twin: simulated door (implements DoorApi, makes no decisions)
├── DoorGUI.java       # View: JavaFX graphical simulation & manual diagnostic buttons
└── Main.java          # Test harness: runs the door side and the obstruction + close tests over the bus
```

---

## 2. System Invariants (Strict Rules)

1. **Flat Structure Invariant (No File Bloat)**:
   - Keep all code strictly inside the flat `src/` directory.
   - **DO NOT** create nested packages (e.g. `elevator/door/view`), speculative helpers, or unnecessary wrapper classes.
   - Any new functionality must fit cleanly into the existing 7 core files unless explicitly requested (DoorBus.java and DoorDash.java were added on request for the Software Bus).

2. **API Contract Invariant**:
   - `DoorApi.java` defines the exact contract required by the instructor and cross-team specifications:
     - `void open()`
     - `void close()`
     - `boolean isFullyOpen()`
     - `boolean isFullyClosed()`
     - `boolean hasObstruction()`
   - **DO NOT** alter, rename, or overload these method signatures.

3. **Commit Convention Invariant**:
   - All commits must strictly follow **Conventional Commits** scoped to `door`:
     `<type>(door): <brief description>`
   - Allowed types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`.
   - Examples:
     - `feat(door): implement center-opening physics and obstruction safety reversal`
     - `test(door): add interactive CLI menu in Main for DoorApi operations`
     - `refactor(door): streamline socket listener thread and command parser`

---

## 3. Quick Start & Execution

```bash
make compile     # compile all source files

# Start the T03 Software Bus first (in the T03 repo): SBMain 6000
make test        # tests with no GUI: rules + messages (+ the real bus if it is running)
make cli         # test harness: its own door + the 7 test cases, all over the bus
# GUI demo: start the bus, then `make sim`, then:
#   java -cp out Main --client     (same tests, driving the door in the GUI)
make sim         # JavaFX GUI
```

---

## 4. Software Bus Interface (T03)

Other teams talk to the door through the T03 Software Bus. Our side receives their requests,
and `DoorManager` decides what to do and controls the door through `DoorApi`.

```text
 Other teams -> T03 Software Bus <-> DoorDash <-> DoorManager --DoorApi--> Door (DoorTwin now, hardware later)
```

A message is 8 bytes: `[topic][subtopic][6-byte big-endian body]`. The bus knows nothing about
doors. The subtopic is the car (elevator) number, 1-255; 0 means all cars. The format and topic
numbers are in `DoorBus.java`.

| Topic | Name | Direction | Body |
|------:|------|-----------|------|
| 7 | `DOOR_STATE` (in T03 `Topics`) | door -> bus | 0 closed, 1 opening, 2 open, 3 closing |
| 8 | `DOOR_COMMAND` | bus -> door | 1 = open, 0 = close |
| 9 | `DOOR_OBSTRUCTION` | door -> bus | 1 = blocked, 0 = clear |

Topics 8 and 9 are Team 01's own numbers and are **not yet in the shared T03 topic table**.

**Door rules (`DoorManager`)**
1. Blocked while closing: reopen.
2. Close requested while blocked: ignored.
3. Open requested while closing: reverse and open.
4. Open while already open or opening, or close while already closed or closing: nothing happens.
5. Open for a while (10 seconds by default) with nothing in the way: close by itself (the wait restarts while blocked;
   the time is a constructor argument, and 0 turns auto-close off).

**Starting the door side**
```java
DoorTwin door = new DoorTwin(1);                       // or a real-hardware DoorApi
DoorManager manager = new DoorManager(door, 1);        // car number 1
manager.start();
new DoorDash(manager, "localhost", 6000).start();      // Software Bus host and port
```
The T03 bus must already be running (`SBMain <port>` in the T03 repo).

Notes:
- The T03 bus never pushes: `DoorDash` asks for new messages every 20 ms.
- The bus keeps no history and never echoes a program's own messages, so `DoorManager` re-sends
  the door's status every 500 ms; a program that connects late catches up within that time.
- The manager only knows what `DoorApi` reports (fully open, fully closed, blocked), so it
  remembers which way it last sent the door to tell opening from closing.
