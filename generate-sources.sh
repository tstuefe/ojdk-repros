NUM_SOURCES=16384
OUTDIR=./generated-test-sources
JARFILE=TESTDATA.jar

rm -rf $OUTDIR

set -e

java -cp $REPROS_JAR de.stuefe.repros.metaspace.GenerateSources --realistic -C ${NUM_SOURCES} -D $OUTDIR
pushd $OUTDIR
javac *.java
jar -cf $JARFILE *.class
popd

JAR=`realpath ./${OUTDIR}/${JARFILE}`

echo "generated $JAR "
ls -alt $JAR

