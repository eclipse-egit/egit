#!/bin/sh
#
# Downloads and grafts in the old EGit project history, before
# the project moved to eclipse.org.
#
# It is recommended that you DO NOT use this script on your main
# work repository, or that if you do, you remove the graft before
# attempting to push content to a remote repository.  Grafts cause
# the history traversal system to change behavior, which can break
# other algorithms that depend upon it.


URL=git://repo.or.cz/egit.git
PRE=a9578ba7361b66ab403c6605a1b87fb7b2f94c6e
POST=dfbdc456d8645fc0c310b5e15cf8d25d8ff7f84b

GIT_DIR=$(git rev-parse --git-dir) &&
grafts="$GIT_DIR/info/grafts" &&

if grep $PRE "$grafts" >/dev/null 2>/dev/null
then
  echo 'Graft already installed; doing nothing.' >&2
else
  git remote add old-egit "$URL" &&
  git fetch old-egit &&
  echo $POST $PRE >>"$GIT_DIR/info/grafts" &&
  echo 'Graft installed.' >&2
fi
