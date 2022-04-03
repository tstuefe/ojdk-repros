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

# extract each indidual jar to get the dependent classes 
# this is ridiculouzs I know but the only way we get dependend classes pulled by maven build before
for ((ver=8;ver < 100;ver++)); do
        THISJAR=$repros_root/repros${ver}/target/repros${ver}-1.0.jar
	if [ -f $THISJAR ]; then
		mkdir extracted-${ver}
		pushd extracted-${ver}
		cp $THISJAR .
   	 	$openjdk_root/jdks/sapmachine11/bin/jar -xf $THISJAR
		rm -rf META-INF
		rm repros${ver}-1.0.jar
		popd
	fi
done


mkdir jar-root
pushd jar-root

# copy 8 classes into the jar root
rsync -az ../extracted-8/* .

# copy other classes into version dependent sub dirs
for ((ver=9;ver < 100;ver++)); do
	if [ -d ../extracted-${ver} ]; then
		mkdir -p META-INF/versions/${ver}
		rsync -az ../extracted-${ver}/* META-INF/versions/${ver}
	fi
done

cat << EOF >> $repros_root/kannweg/manifest-additions.txt
Multi-Release: true
EOF

$openjdk_root/jdks/sapmachine11/bin/jar --manifest=$repros_root/kannweg/manifest-additions.txt --create --verbose --file=$repros_root/repros.jar *




