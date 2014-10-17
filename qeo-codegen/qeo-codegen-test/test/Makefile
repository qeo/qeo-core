PLATFORM = i686-linux
CC = gcc

TARGET = qdmtest
LIBDIR = $(QEO_HOME)/c/lib/$(PLATFORM)
LIBS = -lqeo
CFLAGS = -m32 -g -Wall -Werror -I$(QEO_HOME)/c/include

ifeq ($(QEO_HOME),)
$(error QEO_HOME is not defined)
endif

.PHONY: default all clean

default: $(TARGET)
all: default

OBJECTS = $(patsubst src/%.c,bin/%.o,$(wildcard src/*.c))
HEADERS = $(wildcard src/*.h)

bin/%.o: src/%.c $(HEADERS)
	$(CC) $(CFLAGS) -c $< -o $@

$(TARGET): $(OBJECTS)
	$(CC) -m32 -L$(LIBDIR) -Wl,-rpath,$(LIBDIR) $(OBJECTS) -Wall $(LIBS) -o $@
	@echo "$(TARGET) created."

