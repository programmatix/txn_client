#!/bin/sh
URL=$1
BASEFILE=$2
DIST=$3

CACHE=$HOME/server-packages

if [ ! -d ${CACHE} ]; then
  mkdir -p ${CACHE}
fi

pkill -KILL -f memcached
pkill -KILL -f epmd
pkill -KILL -f beam.smp
rm -rf /opt/couchbase

if [ "${DIST}" = "EL" ]; then
  rpm -e couchbase-server
else
  dpkg -P couchbase-server
fi


PACKAGE=${CACHE}/${BASEFILE}
if [ ! -e ${PACKAGE} ]; then
  wget --progress=dot:mega -O ${PACKAGE} ${URL};
fi

if [ "${DIST}" = "EL" ]; then
  rpm -ivh ${PACKAGE}
else
  dpkg -i ${PACKAGE}
fi