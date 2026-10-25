# CS460 Project 2: Elevator Door Assembly (DOR)

**CS 460 Software Engineering — University of New Mexico (Team #01)**

---

## 1. Architecture & Flat File Structure

The project strictly follows the **Digital Twin (DTW)** hardware abstraction pattern in a clean, flat 5-file layout:

```text
src/
├── DoorApi.java       # Contract: The 5 agreed-upon door control & inquiry APIs
├── DoorManager.java   # Client Bridge: Implements DoorApi over TCP Socket connection
├── DoorTwin.java      # Hardware Twin: Center-opening sliding door kinematics & Socket server
├── DoorGUI.java       # View: JavaFX graphical simulation & manual diagnostic buttons
└── Main.java          # Controller / CLI: Interactive terminal test harness
```

---

## 2. System Invariants (Strict Rules)

1. **Flat Structure Invariant (No File Bloat)**:
   - Keep all code strictly inside the flat `src/` directory.
   - **DO NOT** create nested packages (e.g. `elevator/door/view`), speculative helpers, or unnecessary wrapper classes.
   - Any new functionality must fit cleanly into the existing 5 core files unless explicitly requested.

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
# Compile all source files
make compile

# Run interactive CLI Test Harness (Menu options 0-5)
make cli     # or make test

# Launch JavaFX Digital Twin GUI Simulation
make sim
```
