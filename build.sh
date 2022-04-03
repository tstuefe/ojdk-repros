#!/bin/bash

repros_root=$(pwd)
#openjdk_root=$repros_root/../..
openjdk_root=/shared/projects/openjdk

set -e

. /shared/projects/maven/setenv.sh

mvn clean install

rm -rf kannweg

mkdir kannweg
pushd kannweg

mkdir jar-root
pushd jar-root

rsync -avz $repros_root/repros8/target/classes/* .

for ((ver=9;ver < 100;ver++)); do
	if [ -d $repros_root/repros${ver}/target/classes/* ]; then
		mkdir -p META-INF/versions/${ver}
		rsync -avz $repros_root/repros${ver}/target/classes/de META-INF/versions/${ver}
	fi
done

cat << EOF >> $repros_root/kannweg/manifest-additions.txt
Multi-Release: true
EOF

$openjdk_root/jdks/sapmachine11/bin/jar --manifest=$repros_root/kannweg/manifest-additions.txt --create --verbose --file=$repros_root/repros.jar *




