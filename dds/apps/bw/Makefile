#######################################################
#
# Makefile for DDS test application.
#
#######################################################

EXECUTABLE = bw

BASE       = ../../src

RTPS       = ${BASE}/rtps
TRANS      = ${BASE}/trans
DISC       = ${BASE}/disc
CACHE      = ${BASE}/cache
DCPS	   = ${BASE}/dcps
DDS	   = ${BASE}/dds
CO         = ${BASE}/co
DBG        = ${BASE}/dbg
DYNIP	   = ${BASE}/dynip
#TYPE       = ${BASE}/typecode
TYPE       = ${BASE}/xtypes
SQL        = ${BASE}/sql

dds_CSRCS  = ${DDS}/dds.c ${DDS}/domain.c ${DDS}/locator.c ${DDS}/guid.c \
	     ${DDS}/dds_seq.c
type_CSRCS = ${TYPE}/xtypecode.c ${TYPE}/xtypes.c ${TYPE}/xdata.c \
	     ${TYPE}/xcdr.c ${TYPE}/tsm.c ${TYPE}/pl_cdr.c ${TYPE}/pid.c \
	     ${TYPE}/xtopic.c ${TYPE}/builtin.c ${TYPE}/vtc.c
#type_CSRCS = ${TYPE}/typecode.c ${TYPE}/cdr.c ${TYPE}/pl_cdr.c ${TYPE}/pid.c
dcps_CSRCS = ${DCPS}/dcps_main.c ${DCPS}/uqos.c ${DCPS}/guard.c
cache_CSRCS= ${CACHE}/cache.c
disc_CSRCS = ${DISC}/disc.c
rtps_CSRCS = ${RTPS}/rtps_main.c ${RTPS}/rtps_mux.c
trans_CSRCS= ${TRANS}/ip/rtps_ip.c ${TRANS}/ip/ri_udp.c ${TRANS}/ip/ri_dtls.c
co_CSRCS   = ${CO}/pool.c ${CO}/sys.c ${CO}/error.c ${CO}/ipc.c ${CO}/timer.c \
	     ${CO}/sock.c ${CO}/skiplist.c ${CO}/str.c ${CO}/heap.c ${CO}/md5.c \
	     ${CO}/db.c ${CO}/handle.c ${CO}/tty.c ${CO}/log.c ${CO}/ctrace.c \
	     ${CO}/thread.c ${CO}/prof.c ${CO}/hash.c ${CO}/strseq.c \
	     ${CO}/random.c ${CO}/nmatch.c ${CO}/ipfilter.c ${CO}/libx.c
sql_CSRCS  = ${SQL}/scan.c ${SQL}/parse.c ${SQL}/bytecode.c
dbg_CSRCS  = ${DBG}/debug.c
dynip_CSRCS= ${DYNIP}/di_main.c ${DYNIP}/di_linux.c

prog_CSRCS = main.c ${dds_CSRCS} ${type_CSRCS} ${dcps_CSRCS} ${disc_CSRCS} \
	     ${cache_CSRCS} ${rtps_CSRCS} ${trans_CSRCS} ${dbg_CSRCS} \
	     ${sql_CSRCS} ${dynip_CSRCS} ${co_CSRCS}
prog_COBJS = ${prog_CSRCS:.c=.o}
prog_CHDRS = ${BASE}/include/*.h ../../api/headers/dds/*.h

prog_OBJS  = ${prog_COBJS}

${EXECUTABLE}:	${prog_OBJS}
	gcc -o ${EXECUTABLE} ${prog_OBJS} ${LDFLAGS}

#######################################################
# Setup Compile Environment

CC       = gcc

INC_PATH = -I${BASE}/include -I../../api/headers
LIB_PATH = 
LIBS     = 

DEFINES  = -DFILT_VMWARE -DPTHREADS_USED -DRTPS_USED -DXTYPES_USED -DDDS_BUILTINS -DDDS_IPV6 -DDDS_TYPECODE -DDDS_DYN_IP
#-DDDS_DEBUG -DCTRACE_USED 
#-DFORCE_MALLOC
#-DCTRACE_USED -DDDS_DEBUG
#-DCTRACE_USED -DLOCK_TRACE -DDDS_DEBUG -DPROFILE 
#-DLOCK_TRACE -DCTRACE_USED
#-DRTPS_TRACE -DDDS_DEBUG 
#-DVALGRIND_USED
#-DDDS_STATUS
#-DFORCE_MALLOC

OPTTYPE  = -O3
# -O3
# If profiling: add -pg to both CC and LD options.

CFLAGS   = -Wall -Wextra -pedantic -Wno-long-long ${OPTTYPE} -g ${INC_PATH} ${DEFINES}
LDFLAGS  = ${LIB_PATH} ${LIBS} -lnsl -lrt -g

all: ${EXECUTABLE}

#######################################################
# Rules for cleaning

clean:
	\rm -f ${prog_OBJS} ${EXECUTABLE}

reallyclean: clean
	\rm -f .log_file* ${BASE}/*/tags Makefile.bak

########################################################

# Object sizes

size:

	\size ${prog_OBJS} ${EXECUTABLE}

# Lint (if available)

lint:	${prog_CSRCS}
	\flint std.lnt ${prog_CSRCS}

# Additional dependencies.

depend:
	\makedepend ${INC_PATH} ../*/*.[ch]
