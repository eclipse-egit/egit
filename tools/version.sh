#!/bin/sh
# Copyright (C) 2009, Google Inc.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html

# Update all pom.xml with new build number
#
# TODO(spearce) This should be converted to some sort of
# Java based Maven plugin so its fully portable.
#

V=
J=

while [ $# -gt 0 ]
do
case "$1" in
--snapshot=*)
	V=$(echo "$1" | perl -pe 's/^--snapshot=//')
	if [ -z "$V" ]
	then
		echo >&2 "usage: $0 --snapshot=0.n.0"
		exit 1
	fi
	case "$V" in
	*-SNAPSHOT) : ;;
	*) V=$V-SNAPSHOT ;;
	esac
	shift
	;;

--release)
	V=$(git describe HEAD) || exit
	shift
	;;

--jgit=*)
	J=${1##--jgit=}
	shift
	;;

*)
	echo >&2 "usage: $0 {--snapshot=0.n.0 | --release} [--jgit=0.n.0]"
	exit 1
esac
done

case "$V" in
v*) V=$(echo "$V" | perl -pe s/^v//) ;;
'')
	echo >&2 "usage: $0 {--snapshot=0.n.0 | --release}"
	exit 1
esac

case "$V" in
*-SNAPSHOT)
	POM_V=$V
	OSGI_V="${V%%-SNAPSHOT}.qualifier"
	;;
*-[1-9]*-g[0-9a-f]*)
	POM_V=$(echo "$V" | perl -pe 's/-(\d+-g.*)$/.$1/')
	OSGI_V=$(perl -e '
		$ARGV[0] =~ /^(\d+)(?:\.(\d+)(?:\.(\d+))?)?-(\d+)-g(.*)$/;
		my ($a, $b, $c, $p, $r) = ($1, $2, $3, $4, $5);
		$b = '0' unless defined $b;
		$c = '0' unless defined $c;

		printf "%s.%s.%s.%6.6i_g%s\n", $a, $b, $c, $p, $r;
		' "$V")
	;;
*)
	POM_V=$V
	OSGI_V=$V
	;;
esac

to_version() {
	perl -e '
		$ARGV[0] =~ /^(\d+(?:\.\d+(?:\.\d+)?)?)/;
		print $1
	' "$1"
}

next_version() {
	perl -e '
		$ARGV[0] =~ /^(\d+)(?:\.(\d+)(?:\.(\d+))?)?/;
		my ($a, $b) = ($1, $2);
		$b = 0 unless defined $b;
		$b++;
		print "$a.$b.0";
		' "$1"
}

EGIT_V=$(to_version "$V")
EGIT_N=$(next_version "$EGIT_V")

[ -z "$J" ] && J=$V
JGIT_V=$(to_version "$J")
JGIT_N=$(next_version "$JGIT_V")

perl -pi~ -e '
	s/^(Bundle-Version:\s*).*$/${1}'"$OSGI_V"'/;
	s/(org.eclipse.egit.*;version=")[^"[(]*(")/${1}'"$EGIT_V"'${2}/;
	s/(org.eclipse.egit.*;version="\[)[^"]*(\)")/${1}'"$EGIT_V,$EGIT_N"'${2}/;
	s/(org.eclipse.egit.*;bundle-version="\[)[^"]*(\)")/${1}'"$EGIT_V,$EGIT_N"'${2}/;
	s/(org.eclipse.jgit.*;version="\[)[^"]*(\)")/${1}'"$JGIT_V,$JGIT_N"'${2}/;
	s/(org.eclipse.jgit;bundle-version="\[)[^"]*(\)")/${1}'"$JGIT_V,$JGIT_N"'${2}/;
	' $(git ls-files | grep META-INF/MANIFEST.MF)

perl -pi~ -e '
	if ($ARGV ne $old_argv) {
		$seen_version = 0;
		$old_argv = $ARGV;
	}
	if (!$seen_version) {
		$seen_version = 1 if (!/<\?xml/ &&
		s/(version=")[^"]*(")/${1}'"$OSGI_V"'${2}/);
	}
	s/(feature="org.eclipse.egit" version=")[^"]*(")/${1}'"$EGIT_V"'${2}/;
	' org.eclipse.egit.mylyn-feature/feature.xml

perl -pi~ -e '
	if ($ARGV ne $old_argv) {
		$seen_version = 0;
		$old_argv = $ARGV;
	}
	if (!$seen_version) {
		$seen_version = 1 if (!/<\?xml/ &&
		s/(version=")[^"]*(")/${1}'"$OSGI_V"'${2}/);
	}
	s/(plugin="org.eclipse.egit.core" version=")[^"]*(")/${1}'"$EGIT_V"'${2}/;
	s/(plugin="org.eclipse.egit.ui" version=")[^"]*(")/${1}'"$EGIT_V"'${2}/;
	' org.eclipse.egit.gitflow-feature/feature.xml

perl -pi~ -e '
	if ($ARGV ne $old_argv) {
		$seen_version = 0;
		$old_argv = $ARGV;
	}
	if (!$seen_version) {
		$seen_version = 1 if (!/<\?xml/ &&
		s/(version=")[^"]*(")/${1}'"$OSGI_V"'${2}/);
	}
	s/(feature="org.eclipse.jgit" version=")[^"]*(")/${1}'"$JGIT_V"'${2}/;
	' $(git ls-files | grep feature.xml)

perl -pi~ -e '
	s{<(version)>[^<\$]*</\1>}{<${1}>'"$POM_V"'</${1}>};
	' org.eclipse.egit-feature/pom.xml

perl -pi~ -e '
	if ($ARGV ne $old_argv) {
		$seen_version = 0;
		$old_argv = $ARGV;
	}
	if (!$seen_version) {
		$seen_version = 1 if
		s{<(version)>[^<\$]*</\1>}{<${1}>'"$POM_V"'</${1}>};
	}
	s{<(egit-version)>[^<\$]*</\1>}{<${1}>'"$POM_V"'</${1}>};
	' pom.xml

perl -pi~ -e '
	if ($ARGV ne $old_argv) {
		$seen_version = 0;
		$old_argv = $ARGV;
	}
	if ($seen_version < 2) {
		$seen_version++ if
		s{<(version)>[^<\$]*</\1>}{<${1}>'"$POM_V"'</${1}>};
	}
	' org.eclipse.egit.repository/pom.xml

perl -pi~ -e '
	if ($ARGV ne $old_argv) {
		$seen_version = 0;
		$seen_version2 = 0;
		$old_argv = $ARGV;
	}
	if (!$seen_version) {
		$seen_version = 1 if
		s{<(version)>[^<\$]*</\1>}{<${1}>'"$POM_V"'</${1}>};
	}
	if ($seen_version2 < 2) {
		$seen_version2++ if
		s|(<version\>)([\.\d]*)(\</version\>)|${1}'$EGIT_V'${3}|;
	}
	' org.eclipse.egit.ui.test/pom.xml

perl -pi~ -e '
	if ($ARGV ne $old_argv) {
		$seen_version = 0;
		$seen_version2 = 0;
		$old_argv = $ARGV;
	}
	if (!$seen_version) {
		$seen_version = 1 if
		s{<(version)>[^<\$]*</\1>}{<${1}>'"$POM_V"'</${1}>};
	}
	if ($seen_version2 < 3) {
		$seen_version2++ if
		s|(<version\>)([\.\d]*)(\</version\>)|${1}'$EGIT_V'${3}|;
	}
	' org.eclipse.egit.mylyn.ui.test/pom.xml

perl -pi~ -e '
	if ($ARGV ne $old_argv) {
		$seen_version = 0;
		$old_argv = $ARGV;
	}
	if (!$seen_version) {
		$seen_version = 1 if
		s{<(version)>[^<\$]*</\1>}{<${1}>'"$POM_V"'</${1}>};
	}
	s{<(jgit-version)>[^<]*</\1>}{<${1}>'"$J"'</${1}>};
	' $(git ls-files | grep pom.xml)

find . -name '*~' | xargs rm -f
git diff
