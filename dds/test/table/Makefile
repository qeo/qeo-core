EXECUTABLE = table

BASE       = ../../src

DDS_SRCS   = ${BASE}/co/handle.c ${BASE}/co/table.c ${BASE}/co/pool.c ${BASE}/co/error.c \
	     ${BASE}/co/skiplist.c ${BASE}/co/random.c ${BASE}/co/sys.c ${BASE}/co/db.c

all:	table

CSRCS = main.c ${DDS_SRCS}

prog_OBJS = ${CSRCS:.c=.o}

CC = gcc

INC_PATH = -I${BASE}/include -I../../api/headers

CFLAGS   = -Wall -g -DCDR_ONLY -DNO_LOCKS -DDDS_DEBUG ${INC_PATH}

table:	${prog_OBJS}
	gcc -O0 ${INC_PATH} ${prog_OBJS} -o table -lrt

clean:
	rm -f ${prog_OBJS} table tags core core.*
