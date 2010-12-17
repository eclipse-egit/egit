#!/bin/sh
# Script to shrink images to reduce download size of the images contained
# in the egit user documentation
#
# This requires the following tools to be installed
# - OptiPNG (http://optipng.sourceforge.net/)
#   installation: $ sudo apt-get install optipng
#   on Mac OSX using MacPorts: $ sudo port install optipng
#- pngnq (http://pngnq.sourceforge.net/)
#   installation: $ sudo apt-get install pngnq
#   Mac OSX binary can be found here http://pornel.net/pngnq

EXT_NQ8='-nq8.png'

for FILENAME in $(find . -type f -name '*.png')
do
	echo "=============================="
    echo "FILENAME " $FILENAME
	stat -f "%Dz bytes" $FILENAME
    optipng $FILENAME
	pngnq -n 256 -s 3 -e $EXT_NQ8 $FILENAME
	BASENAME="${FILENAME%'.png'}"
	mv -f $BASENAME$EXT_NQ8 $FILENAME
    echo "shrinked $FILENAME"
	stat -f "%Dz bytes" $FILENAME
done