# Eclipse Git Plugin

_EGit_ is a set of Eclipse plugins for working with Git repositories. It is
based on the _JGit_ library, which is a Git implementation in pure Java. This
package is licensed under the _EPL 2.0_. Please refer to the `LICENSE` file
for the complete license.

## Components

This package is composed of the following major components.

### Implementation

- __org.eclipse.egit__: Eclipse branding plugin for _EGit_.
- __org.eclipse.egit.core__: An Eclipse plugin providing an interface to
    org.eclipse.jgit and support routines to allow processing in an
    Eclipse workspace. It also supplies the team provider implementation.
- __org.eclipse.egit.gitflow__: bundle implementing support for the
    [gitflow](https://nvie.com/posts/a-successful-git-branching-model/)
    branching model.
- __org.eclipse.egit.gitflow.ui__: bundle implementing a user interface
    for the gitflow branching model.
- __org.eclipse.egit.mylyn__: bundle integrating _EGit_ with _Eclipse Mylyn_
    which provides integration for Eclipse with task tracking systems.
- __org.eclipse.egit.mylyn.ui__: bundle integrating _EGit_ user interface with
    _Eclipse Mylyn_ task based user interface.
- __org.eclipse.egit.target__: Eclipse target platform providing EGit
    3rd party dependencies for the build and for running EGit in Eclipse
    workspace.
- __org.eclipse.egit.ui__: An Eclipse plugin providing the user interface on
    top of org.eclipse.egit.core.
- __org.eclipse.egit.ui.importer__: An Eclipse plugin integrating the Eclipse
    smart import wizard to improve importing projects from Git repositories.

### Tests

- __org.eclipse.egit.core.junit__: Reusable classes used by _EGit_ tests
- __org.eclipse.egit.core.test__: Unit tests for org.eclipse.egit.core.
- __org.eclipse.egit.gitflow.test__: Unit tests for org.eclipse.egit.gitflow.
- __org.eclipse.egit.mylyn.ui.test__: UI tests for org.eclipse.egit.mylyn.ui.
- __org.eclipse.egit.ui.importer.test__: UI tests for org.eclipse.egit.ui.smartimport.
- __org.eclipse.egit.ui.test__: UI tests for org.eclipse.egit.ui.

### Packaging

- __org.eclipse.egit.doc__: Documentation bundle packaging EGit documentation.
    Raw documentation is written in the [wiki](https://wiki.eclipse.org/EGit/User_Guide).
- __org.eclipse.egit-feature__: Eclipse feature for installing the core
    implementation bundles
- __org.eclipse.egit.gitflow-feature__: Eclipse feature for installing the
    optional gitflow bundle.
- __org.eclipse.egit.mylyn-feature__: Eclipse feature for installing the EGit
    task based interface integration.
- __org.eclipse.egit.repository__: Definitions for the EGit p2 repository
    which can be used to install and upgrade EGit, includes all the features
    and plugins from the JGit p2 repository.
- __org.eclipse.egit.source-feature__: Eclipse feature for installing EGit
    source bundles to help debugging EGit in Eclipse.

## Compatibility

- In general, EGit supports at least the latest two Eclipse releases.
  For details, please see https://wiki.eclipse.org/EGit/FAQ
- JGit and EGit releases are versioned according to
  [OSGi semantic versioning](https://www.osgi.org/wp-content/uploads/SemanticVersioning.pdf)
- Newer version of EGit may implement new functionality, remove
  existing functions and change others without other notice than what
  is written in the release notes, commit log and source files themselves.

## Features

The following list is not complete, but it gives an overview of the
features:

- __org.eclipse.egit.core__
  - Supplies an Eclipse team provider.
  - Connect/disconnect the provider to a project.
  - Search for the repositories associated with a project by
    autodetecting the Git repository directories.
  - Store which repositories are tied to which containers in the
    Eclipse workspace.
  - Tracks moves/renames/deletes and reflects them in the cache
    tree.
  - Resolves through linked containers.

- __org.eclipse.egit.ui__
  - Connect team provider wizard panels.
  - Connect to Git team provider by making a new repository.
  - Connect to Git team provider by searching local filesystem
    for existing repository directories.
  - Team actions: track (add), untrack (remove), disconnect, show
    history, compare version.
  - Resource decorator shows file/directory state in the package
    explorer and other views.
  - Creating new commits or amending commits.
  - View for staging changes (whole files and partial staging),
    showing their differences and committing them.
  - Graphical history viewer with the ability to compare versions
    using eclipse built-in compare editor.
  - Clone, push, pull, fetch
  - Merge, rebase, cherry-pick

## Missing Features

- signing support is incomplete
  - verifying signed objects
  - signing tags
  - signing pushes

## Support

Post questions or comments to the egit-dev@eclipse.org mailing list.
You need to be [subscribed](https://dev.eclipse.org/mailman/listinfo/egit-dev)
to post.

## Contributing

See the [EGit Contributor Guide](https://wiki.eclipse.org/EGit/Contributor_Guide).

## About Git

More information about Git, its repository format, and the canonical
C based implementation can be obtained from the [Git website](https://git-scm.com/).
