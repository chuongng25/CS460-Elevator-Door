# Uses javac/java from PATH. Override if you need a specific JDK:
#   make sim JAVAC=/path/to/javac JAVA=/path/to/java
JAVAC ?= javac
JAVA  ?= java
OUT   = out
SRCS  = $(wildcard src/*.java)

JAVAFX_LIB ?= $(firstword $(wildcard \
    $(JAVAFX_HOME)/lib \
    $(HOME)/Downloads/javafx-sdk-*/lib \
    $(USERPROFILE)/Downloads/javafx-sdk-*/lib \
    /opt/homebrew/opt/openjfx/lib \
    /usr/local/opt/openjfx/lib \
    /usr/share/openjfx/lib \
))

FX_FLAGS = $(if $(JAVAFX_LIB),--module-path "$(JAVAFX_LIB)" --add-modules javafx.controls)

.PHONY: all compile sim test clean

all: sim

compile:
	@"$(JAVAC)" -d $(OUT) $(FX_FLAGS) $(SRCS)

sim: compile
	@"$(JAVA)" $(FX_FLAGS) -cp $(OUT) DoorGUI

test: compile
	@"$(JAVA)" -cp $(OUT) Main

clean:
	-@rm -rf $(OUT) || rmdir /s /q $(OUT)
