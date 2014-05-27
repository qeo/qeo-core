#######################################################
#
# Makefile for DDS test application.
#
#######################################################

EXECUTABLE = dds

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
TYPE       = ${BASE}/xtypes
SQL	   = ${BASE}/sql
SECURITY   = ${BASE}/security

dds_CSRCS  = ${DDS}/dds.c ${DDS}/domain.c ${DDS}/locator.c ${DDS}/guid.c \
	     ${DDS}/dds_seq.c ${DDS}/uqos.c ${DDS}/guard.c
type_CSRCS = ${TYPE}/xtypecode.c ${TYPE}/xtypes.c ${TYPE}/xdata.c \
	     ${TYPE}/xcdr.c ${TYPE}/tsm.c ${TYPE}/pl_cdr.c ${TYPE}/pid.c \
	     ${TYPE}/xtopic.c ${TYPE}/builtin.c ${TYPE}/vtc.c
dcps_CSRCS = ${DCPS}/dcps_main.c ${DCPS}/dcps_dpfact.c ${DCPS}/dcps_part.c \
	     ${DCPS}/dcps_pub.c ${DCPS}/dcps_sub.c ${DCPS}/dcps_topic.c \
	     ${DCPS}/dcps_reader.c ${DCPS}/dcps_writer.c ${DCPS}/dcps_event.c \
	     ${DCPS}/dcps_waitset.c ${DCPS}/dcps_builtin.c ${DCPS}/dcps_qos.c \
	     ${DCPS}/dcps_entity.c ${DCPS}/dcps_dbg.c
cache_CSRCS= ${CACHE}/cache.c
disc_CSRCS = ${DISC}/disc.c
rtps_CSRCS = ${RTPS}/rtps_main.c ${RTPS}/rtps_mux.c ${RTPS}/rtps_msg.c \
	     ${RTPS}/rtps_slbw.c ${RTPS}/rtps_slbr.c ${RTPS}/rtps_slrw.c \
	     ${RTPS}/rtps_sfbr.c ${RTPS}/rtps_sfbw.c ${RTPS}/rtps_sfrr.c \
	     ${RTPS}/rtps_sfrw.c ${RTPS}/rtps_clist.c ${RTPS}/rtps_frag.c \
	     ${RTPS}/rtps_dbg.c ${RTPS}/rtps_trace.c ${RTPS}/rtps_ft.c \
	     ${RTPS}/rtps_fwd.c
trans_CSRCS= ${TRANS}/ip/rtps_ip.c ${TRANS}/ip/ri_udp.c ${TRANS}/ip/ri_dtls.c \
	     ${TRANS}/ip/ri_tcp.c ${TRANS}/ip/ri_tcp_sock.c ${TRANS}/ip/ri_tls.c
sec_CSRCS  = ${SECURITY}/security.c
co_CSRCS   = ${CO}/pool.c ${CO}/sys.c ${CO}/error.c ${CO}/ipc.c ${CO}/timer.c \
	     ${CO}/sock.c ${CO}/skiplist.c ${CO}/str.c ${CO}/heap.c ${CO}/md5.c \
	     ${CO}/db.c ${CO}/handle.c ${CO}/tty.c ${CO}/log.c ${CO}/ctrace.c \
	     ${CO}/thread.c ${CO}/prof.c ${CO}/hash.c ${CO}/strseq.c \
	     ${CO}/nmatch.c ${CO}/random.c ${CO}/ipfilter.c ${CO}/libx.c \
	     ${CO}/config.c ${CO}/cmdline.c \
	     ../secplug/msecplug.c ../secplug/xmlparse.c ../security/engine_fs.c
sql_CSRCS  = ${SQL}/scan.c ${SQL}/parse.c ${SQL}/bytecode.c
dbg_CSRCS  = ${DBG}/debug.c
dynip_CSRCS= ${DYNIP}/di_main.c ${DYNIP}/di_linux.c

prog_CSRCS = main.c ${dds_CSRCS} ${type_CSRCS} ${dcps_CSRCS} ${disc_CSRCS} \
	     ${cache_CSRCS} ${dbg_CSRCS} ${sql_CSRCS} ${co_CSRCS} \
	     ${dynip_CSRCS} ${sec_CSRCS} 

#comment the following && don't define RTPS_USED to drop RTPS/Discovery
prog_CSRCS += ${rtps_CSRCS} ${trans_CSRCS}

prog_COBJS = ${prog_CSRCS:.c=.o}
prog_CHDRS = ${BASE}/include/*.h ../../api/headers/dds/*.h

prog_OBJS  = ${prog_COBJS}

${EXECUTABLE}:	${prog_OBJS}
	gcc -o ${EXECUTABLE} ${prog_OBJS} ${LDFLAGS}

#######################################################
# Setup Compile Environment

CC       = gcc

INC_PATH = -I${BASE}/include -I../../api/headers -I/usr/include/libxml2 -I../security/
#LIB_PATH =
LIB_PATH = -L../../../qeo-c/output/openssl/HOSTLINUX/ 
LIBS     = 

DEFINES  = -DPTHREADS_USED -DRTPS_USED -DCDD_USED -DDDS_DEBUG -DEXTRA_STATS -DLOG_FILE -DXTYPES_USED -DDDS_BUILTINS -DDDS_IPV6 -DDDS_TYPECODE -DDDS_AUTO_LIVELINESS -DDDS_DYN_IP -DDDS_TCP -DBIGDATA -DRTPS_TRACE -DDDS_TRACE -DDDS_FORWARD -DEXTRA_STATS -DUSE_BYTESWAP -DDDS_SECURITY -DMSECPLUG_WITH_SECXML -DMSG_TRACE -DFORWARD_TRC -DTTY_NORMAL -DFORCE_MALLOC -DUSE_ABORT
# -DTTY_NORMAL
#-DDDS_IP_BCAST
#-DDDS_AUTO_LIVELINESS
#-DFORCE_MALLOC
#-DDDS_IP_BCAST
#-DLOG_FILE -DRTPS_TRACE -DRTPS_SEDP_TRACE
#-DDDS_NO_MCAST
#-DDDS_SERVER
#-DPROFILE 
#-DSTATIC_DISCOVERY
#-DCDD_USED
#-DLOG_FILE 
#-DRTPS_USED
#-DRTPS_TRACE 
#-DDDS_DEBUG 
#-DLOCK_TRACE
#-DCTRACE_USED
#-DCTRACE_START
#-DDUMP_LOCATORS
#-DLOG_DOMAIN
#-DVALGRIND_USED
#-DDDS_STATUS
#-DFORCE_MALLOC
#-DFILT_VMWARE
#-DBIGDATA
#-DMSG_TRACE

OPTTYPE  = -O0
# -fprofile-arcs -ftest-coverage
PROFILE  = 
#-pg
CFLAGS   = -Wall -Wextra -pedantic -Wno-long-long ${OPTTYPE} ${INC_PATH} ${DEFINES} -g ${PROFILE}
LDFLAGS  = ${LIB_PATH} ${LIBS} -lnsl -lrt -lssl -lcrypto -lxml2 -g -lgcov -lpthread ${PROFILE}

all: ${EXECUTABLE}

#######################################################
# Rules for cleaning

clean:
	rm -f ${prog_OBJS} ${EXECUTABLE} core.*

reallyclean: clean
	rm -f .tdds_hist .tdds_log_* ${BASE}/*/tags Makefile.bak tags ./.depend

########################################################

# Lines of code

wc:
	wc Makefile ${prog_CSRCS} ../../co/llist.c \
	../../co/random.c ${prog_CHDRS} ../../tools/shm/*shm.c ../../tools/ctget.c \
	../bw/main.c ../latency/main.c ../shapes/main.c ../rr/main.c ../cdd/main.c \
	../cdr/*.[ch] ../inst/*.c ../limits/*.[ch] ../md5/*.c ../../trans/raw/rtps_raw.c \
	../api/*.[ch] ../rshapes/main.c

# Object sizes

size:

	size ${prog_OBJS} ${EXECUTABLE}

# coverage

coverage:
	cd ../api/coverage
	lcov -b ../../dcps --directory=../../.. --capture --output-file api.info

# Lint (if available)
LINT=/home/jvoe/bin/flint

lint:	${prog_CSRCS}
	${LINT} std.lnt ${prog_CSRCS}

# Additional dependencies.

depend:	.depend
