Eclipse Git Plugin
==================

EGit is an Eclipse plugin for working with Git repositories. It is based
on the JGit library, which is a Git implementation in pure Java.

This package is licensed under the EPL. Please refer to the LICENSE file
for the complete license.

This package is composed of the following major components:

- org.eclipse.egit.core

    An Eclipse plugin providing an interface to org.eclipse.jgit
    and support routines to allow processing against the Eclipse
    workspace and resource APIs, rather than the standard Java
    file APIs. It also supplies the team provider implementation.

- org.eclipse.egit.ui

    An Eclipse plugin providing the user interface on top of
    org.eclipse.egit.core.

- org.eclipse.egit.core.test

    Unit tests for org.eclipse.egit.core.

- org.eclipse.egit.ui.test

    UI tests for org.eclipse.egit.ui.

- org.eclipse.egit

    A plugin for packaging

- org.eclipse.egit-feature

    Also packaging. This project is for building an Eclipse "feature"
    out of the plugins above.

- org.eclipse.egit.repository

    This package is for producing a p2 repository, i.e. a web site
    you can point your eclipse at and just upgrade.

There are other components which provide integration with other plugins.

Warnings/Caveats
----------------

- Symbolic links are supported on Java 7 and higher and require that the
  optional JGit Java 7 feature is installed. For remaining issues
  with symbolic link support see
  https://bugs.eclipse.org/bugs/show_bug.cgi?id=429304.

- CRLF conversion works for some things, but is in general still being
  worked on.

Compatibility
-------------

- In general, EGit supports at least the latest two Eclipse releases.
  For details, please see https://wiki.eclipse.org/EGit/FAQ

- Newer version of EGit may implement new functionality, remove
  existing functions and change others without other notice than what
  is written in the commit log and source files themselves.


Package Features
----------------

The following list is not complete, but it gives an overview of the
features:

- org.eclipse.egit.core

    * Supplies an Eclipse team provider.

    * Connect/disconnect the provider to a project.

    * Search for the repositories associated with a project by
      autodetecting the Git repository directories.

    * Store which repositories are tied to which containers in the
      Eclipse workspace.

    * Tracks moves/renames/deletes and reflects them in the cache
      tree.

    * Resolves through linked containers.

- org.eclipse.egit.ui

    * Connect team provider wizard panels.

    * Connect to Git team provider by making a new repository.

    * Connect to Git team provider by searching local filesystem
      for existing repository directories.

    * Team actions: track (add), untrack (remove), disconnect, show
      history, compare version.

    * Resource decorator shows file/directory state in the package
      explorer and other views.

    * Creating new commits or amending commits.

    * View for staging changes (whole files and partial staging),
      showing their differences and committing them.

    * Graphical history viewer with the ability to compare versions
      using eclipse built-in compare editor.

    * Clone, push, pull, fetch

    * Merge, rebase, cherry-pick


Missing Features
----------------

- gitattributes support

  In particular CRLF conversion is not yet fully implemented.


Support
-------

Post question or comments to the egit-dev@eclipse.org mailing list.
You need to be subscribed to post, see here:

https://dev.eclipse.org/mailman/listinfo/egit-dev


Contributing
------------

See the EGit Contributor Guide:

http://wiki.eclipse.org/EGit/Contributor_Guide


About Git
---------

More information about Git, its repository format, and the canonical
C based implementation can be obtained from the Git websites:

http://git-scm.com/
