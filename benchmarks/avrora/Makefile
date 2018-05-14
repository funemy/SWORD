#
# Makefile for avrora source code and documentation
#

BIN_DIR      = bin
DOC_DIR      = doc
SRC_DIR      = src

AVRORA_SRC  := $(shell find $(SRC_DIR)/avrora -name '*.java')
CCK_SRC     := $(shell find $(SRC_DIR)/cck -name '*.java')
JINTGEN_SRC := $(shell find $(SRC_DIR)/jintgen -name '*.java')

JAVAC        = javac
JAVADOC      = javadoc
CFLAGS       = -source 1.4 -d $(BIN_DIR)
C5FLAGS      = -source 1.5 -d $(BIN_DIR)
DOCFLAGS     = -breakiterator -sourcepath $(SRC_DIR) -d $(DOC_DIR)

all: avrora

avrora:
	@$(JAVAC) $(CFLAGS) $(CCK_SRC) $(AVRORA_SRC)

cck:
	@$(JAVAC) $(CFLAGS) $(CCK_SRC)

jintgen:
	@$(JAVAC) $(C5FLAGS) $(CCK_SRC) $(JINTGEN_SRC)

clean:
	@rm -rf bin/cck bin/avrora bin/jintgen doc/*.html doc/cck doc/avrora doc/jintgen

doc: doc/index.html

doc/index.html:
	@$(JAVADOC) $(DOCFLAGS) $(AVRORA_SRC)

.PHONY: all clean doc
