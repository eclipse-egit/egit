# EGit images

EGit icons are done in SVG. This directory contains the SVG sources, organized in a directory tree that mirrors
the placement of the final PNG versions in the EGit bundles.

## Creating an icon

To create a new icon, add the SVG source ("new_icon.svg") here in the appropriate sub-directory (create it if
necessary). Export it as a PNG file in two versions:

- a 16x16 pixel version named "new_icon.png"
- a 32x32 pixel version named "new_icon@2x.png" for HiDPI devices

Then move the PNG files to the correct places in the correct EGit bundles.

For color palettes, see the eclipse.platform.images project.

## License

All files are subject to the [Eclipse Public License (EPL) v1.0][1]

[1]: http://wiki.eclipse.org/EPL
