JAVA_HOME ?= $(firstword $(wildcard \
    /opt/homebrew/opt/openjdk*/libexec/openjdk.jdk/Contents/Home \
    /usr/local/opt/openjdk*/libexec/openjdk.jdk/Contents/Home \
    /Library/Java/JavaVirtualMachines/*/Contents/Home \
))

JAVAC = $(if $(JAVA_HOME),$(JAVA_HOME)/bin/javac,javac)
JAVA  = $(if $(JAVA_HOME),$(JAVA_HOME)/bin/java,java)
OUT   = out
MODULES = javafx.controls,javafx.fxml
JAVA_FLAGS = -XX:+IgnoreUnrecognizedVMOptions --enable-native-access=ALL-UNNAMED,javafx.graphics --sun-misc-unsafe-memory-access=allow

JAVAFX_LIB ?= $(firstword $(wildcard \
    $(JAVAFX_HOME)/lib \
    $(HOME)/Library/javafx-sdk/*/lib \
    $(HOME)/Downloads/javafx-sdk-*/lib \
    /opt/homebrew/opt/openjfx/lib \
    /usr/local/opt/openjfx/lib \
    /usr/share/openjfx/lib \
))

SRCS = $(shell find src -name "*.java")

.PHONY: all sim test cli server compile clean

all: cli

compile:
	@mkdir -p $(OUT)
	@$(JAVAC) -d $(OUT) --module-path "$(JAVAFX_LIB)" --add-modules $(MODULES) $(SRCS)

server: compile
	@$(JAVA) -cp $(OUT) DoorTwin

cli: compile
	@$(JAVA) -cp $(OUT) Main

test: compile
	@$(JAVA) -cp $(OUT) Main --test

sim: compile
	@$(JAVA) $(JAVA_FLAGS) --module-path "$(JAVAFX_LIB)" --add-modules $(MODULES) -cp $(OUT) DoorGUI

clean:
	@python3 -c "import shutil; shutil.rmtree('$(OUT)', ignore_errors=True)"
