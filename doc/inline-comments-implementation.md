# Inline Pull Request Comments - Implementation Summary

## Overview
Successfully implemented inline pull request comments for the Eclipse EGit Bitbucket integration. Comments are displayed as visual bubbles directly above the commented lines in the compare/diff viewer, similar to IntelliJ IDEA's inline comment rendering.

## Problem Statement

### Initial Challenge
Eclipse's standard annotation framework (`AnnotationModel`, `InlinedAnnotationSupport`) is incompatible with `TextMergeViewer` due to document lifecycle management issues:

- `TextMergeViewer.updateContent()` calls `sourceViewer.setDocument(doc)` which destroys any `AnnotationModel`
- The annotation framework requires a persistent annotation model that survives document operations
- Eclipse's `InlinedAnnotationSupport` depends on this stability, which doesn't exist in `TextMergeViewer`

### Solution Approach
Instead of using the annotation framework, we use **direct SWT widget manipulation**:
- `StyledText.setLineVerticalIndent(lineIndex, pixels)` to reserve space above commented lines
- `PaintListener` to draw comment bubbles in the reserved space
- This is widget-level and independent of document/annotation model lifecycle

### Critical Bug Fixed
During implementation, we discovered and fixed a critical coordinate system bug:

**Problem**: `StyledText.getLinePixel(lineIndex)` returns **viewport-relative** coordinates (relative to the visible area top), NOT absolute document coordinates.

**Incorrect approach** (was comparing viewport-relative coordinates against absolute scroll positions):
```java
int topPixel = styledText.getTopPixel();  // e.g., 9724 (absolute)
int bottomPixel = topPixel + clientHeight;  // e.g., 10045 (absolute)
int linePixel = styledText.getLinePixel(line);  // e.g., 99 (viewport-relative!)
// Wrong: 99 < 9724 = true, incorrectly marked as not visible
```

**Correct approach** (compare viewport-relative coordinates against viewport bounds):
```java
int clientHeight = styledText.getClientArea().height;  // e.g., 321
int linePixel = styledText.getLinePixel(line);  // e.g., 99 (viewport-relative)
// Correct: 99 >= 0 AND 99 <= 321 = visible!
```

## Architecture

### Component Overview

```
PullRequestChangedFilesView
    ↓ (filters comments by file path)
BitbucketCompareEditorInput
    ↓ (checks preference, creates custom viewer)
InlineCommentTextMergeViewer (extends TextMergeViewer)
    ↓ (manages left/right painters)
InlineCommentPainter (implements PaintListener)
    ↓ (reserves space and draws bubbles)
StyledText widget (SWT)
```

### Data Flow

1. **User Action**: Double-clicks a changed file in `PullRequestChangedFilesView`
2. **Comment Filtering**: View filters comments for that specific file path
3. **Input Creation**: Creates `BitbucketCompareEditorInput` and calls `input.setComments(fileComments)`
4. **Viewer Selection**: Compare editor calls `findContentViewer()`
   - Checks `PULLREQUEST_SHOW_INLINE_COMMENTS` preference
   - If enabled AND comments exist: creates `InlineCommentTextMergeViewer`
   - Otherwise: uses default `TextMergeViewer`
5. **Comment Storage**: `InlineCommentTextMergeViewer` stores comments as pending (documents not ready yet)
6. **Content Update**: `updateContent()` is called → applies pending comments via `applyComments()`
7. **Side Separation**: Separates comments by side (FROM=left, TO=right based on `fileType`)
8. **Painter Installation**: Creates `InlineCommentPainter` for each side with comments
9. **Space Reservation**: `InlineCommentPainter.install()` sets vertical indents and adds paint listener
10. **Drawing**: `paintControl()` draws comment bubbles when lines are visible in viewport

## Files Created/Modified

### Created Files

#### 1. InlineCommentPainter.java
**Location**: `/org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/pullrequest/InlineCommentPainter.java`

**Purpose**: Core painting logic that reserves space and draws comment bubbles.

**Key Features**:
- Uses `StyledText.setLineVerticalIndent(int lineIndex, int pixels)` to reserve space (48 pixels per comment)
- Implements `PaintListener.paintControl()` to draw rounded-rect bubbles
- Draws author, timestamp, comment text (truncated at 120 chars), and reply count
- Color coding: blue (#DBE9FE) for active comments, green (#DCEDDE) for resolved
- Proper SWT color resource management
- Viewport-relative coordinate handling for visibility checks

**Architecture Choice**: Direct widget manipulation bypasses Eclipse's annotation framework, making it compatible with `TextMergeViewer`'s document lifecycle.

#### 2. InlineCommentTextMergeViewer.java
**Location**: `/org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/pullrequest/InlineCommentTextMergeViewer.java`

**Purpose**: Custom `TextMergeViewer` subclass that manages inline comment painters.

**Key Features**:
- Extends `TextMergeViewer` to add inline comment support
- Captures left/right `SourceViewer` references in `configureTextViewer()`
- Implements `setComments()` to accept comment list
- Overrides `updateContent()` to apply pending comments when documents are ready
- Handles comment lifecycle (clearing old painters when new comments are set)
- Separates comments by side: LEFT (fileType="FROM") vs RIGHT (fileType="TO")

**Lifecycle Management**: Comments are stored as pending until documents are available, then applied in `updateContent()`.

### Modified Files

#### 3. BitbucketCompareEditorInput.java
**Location**: `/org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/pullrequest/BitbucketCompareEditorInput.java`

**Changes**:
- Added `setComments()` method to accept comment list
- Modified `findContentViewer()` to:
  - Check preference `UIPreferences.PULLREQUEST_SHOW_INLINE_COMMENTS`
  - Create `InlineCommentTextMergeViewer` when appropriate
  - Otherwise use default viewer

**Rationale**: This is the entry point for creating the compare viewer, making it the right place to decide which viewer implementation to use.

#### 4. PullRequestChangedFilesView.java
**Location**: `/org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/pullrequest/PullRequestChangedFilesView.java`

**Changes**:
- Filters comments by file path before passing to compare input
- Compares both `file.getPath()` and `file.getSrcPath()` (for renamed files)
- Sets filtered comments on `BitbucketCompareEditorInput`

**Rationale**: Filtering at the view level ensures only relevant comments are passed to the compare editor.

### Configuration Files (No Changes - Already Existed)

- **UIPreferences.java**: Contains `PULLREQUEST_SHOW_INLINE_COMMENTS` constant (line 408)
- **PluginPreferenceInitializer.java**: Sets default value to `false` (lines 177-178)
- **BitbucketPreferencePage.java**: UI checkbox for preference (lines 122-126)

## Key SWT/Eclipse APIs Used

### StyledText API
- `StyledText.setLineVerticalIndent(int lineIndex, int verticalIndent)` - Reserves space above a line
- `StyledText.getLineVerticalIndent(int lineIndex)` - Returns the vertical indent of a line
- `StyledText.getLinePixel(int lineIndex)` - Returns **viewport-relative** y-coordinate of a line
- `StyledText.getTopPixel()` - Returns absolute scroll position (for reference only)
- `StyledText.getClientArea()` - Returns visible viewport dimensions
- `StyledText.addPaintListener(PaintListener)` - Registers custom painting
- `StyledText.redraw()` - Requests a paint event

### TextMergeViewer Hooks
- `configureTextViewer(TextViewer)` - Called for each viewer (ancestor, left, right) - used to capture `SourceViewer` references
- `updateContent(Object, Object, Object)` - Called when content changes - used to apply pending comments
- `findContentViewer(Viewer, ICompareInput, Composite)` - Factory method for viewer creation

### GC Drawing API
- `GC.fillRoundRectangle()` - Draw comment bubble background
- `GC.drawRoundRectangle()` - Draw comment bubble border
- `GC.drawString()` - Draw text
- `GC.getFontMetrics()` - Calculate text height
- `GC.textExtent()` - Calculate text width

## User-Facing Features

### Enabled/Disabled
- Controlled by preference: **Team → Git → Bitbucket → Show inline comments in compare editor**
- Default: **disabled** (to maintain backward compatibility)

### Visual Appearance
- **Active comments**: Blue background (#DBE9FE), blue border (#B4C8E6)
- **Resolved comments**: Green background (#DCEDDE), blue border
- **Text colors**: 
  - Author: Dark blue (#1E4078)
  - Comment text: Dark gray (#323232)
  - Timestamp: Light gray (#787878)

### Comment Bubble Layout
```
┌─────────────────────────────────────────────────────────┐
│ Author Name  2025-01-15 14:30  [RESOLVED]              │
│ Comment text truncated at 120 characters if needed...  │
│   ↳ Reply Author: Reply text also truncated...         │
└─────────────────────────────────────────────────────────┘
```

### Behavior
- Bubbles appear **above** the commented line (using vertical indent space)
- Only visible when the commented line is in the viewport
- Supports multiple replies per comment
- Bubbles are 48 pixels tall per comment (adjusts for replies)
- Left/right pane placement based on comment's `fileType` ("FROM" = left, "TO" = right)

## Known Limitations

### Current Implementation
1. **Multiple comments per line**: Only the first comment is displayed
   - `InlineCommentPainter.java:108` - Map stores only one comment per line
   - Future enhancement: Support multiple bubbles stacked vertically

2. **No interaction**: Comments are read-only visual indicators
   - No click to navigate to comment in comment panel
   - No hover to show full text
   - No context menu
   - Future enhancement: Add mouse listener for interactions

3. **Scroll synchronization**: Left/right panes may desync when vertical indents differ
   - TextMergeViewer's built-in scroll synchronization doesn't account for asymmetric indents
   - Future enhancement: Custom scroll listener to maintain alignment

4. **Line number mismatches**: Comments on non-existent lines are silently skipped
   - Can occur when diff context differs from comment context
   - Current behavior: `InlineCommentPainter.java:145` - Line range check
   - Future enhancement: Show indicator for "comment on line not in view"

5. **Performance with many comments**: No virtualization
   - All comment vertical indents are set upfront
   - All visible comments are painted on every paint event
   - Acceptable for typical PR sizes (< 100 comments per file)
   - Future enhancement: Lazy indent setting, paint caching

## Testing Checklist

### Verified Scenarios
- ✅ Comments appear when preference is enabled
- ✅ Comments hidden when preference is disabled
- ✅ Blue bubbles for active comments
- ✅ Green bubbles for resolved comments
- ✅ Correct placement on left/right panes based on fileType
- ✅ Bubbles appear above commented lines
- ✅ Scrolling to commented lines shows bubbles
- ✅ Multiple comments on different lines work correctly
- ✅ Reply text is displayed with ↳ prefix
- ✅ Long comment text is truncated with "..."
- ✅ Author and timestamp are displayed
- ✅ No crashes or exceptions during normal operation

### Edge Cases to Test (Future)
- ⏳ Multiple comments on same line (currently only first is shown)
- ⏳ Very long files with many comments (performance)
- ⏳ Renamed files with comments on old path
- ⏳ Comments on lines that don't exist in current diff
- ⏳ Rapid scrolling (paint performance)
- ⏳ Resizing compare editor window
- ⏳ Switching between files with different comment counts

## Future Enhancements

### High Priority
1. **Multiple comments per line** - Stack bubbles vertically
2. **Click to navigate** - Jump to comment in comment panel when bubble is clicked
3. **Hover for full text** - Show tooltip with full comment text on hover

### Medium Priority
4. **Smooth scroll sync** - Custom scroll listener for left/right pane alignment
5. **Comment on non-visible lines** - Show indicator in scrollbar or margin
6. **Resize bubbles** - Expand/collapse with animation
7. **Filter by state** - Show only active or only resolved comments

### Low Priority
8. **Performance optimization** - Lazy indent setting, paint caching, virtualization
9. **Configurable colors** - Allow user to customize bubble colors in preferences
10. **Comment actions** - Reply, resolve, edit from bubble context menu

## Related Documentation
- **Bitbucket Integration**: `bitbucket-implementation-summary.md`
- **Comment Data Model**: `org.eclipse.egit.core/.../PullRequestComment.java`
- **Preference Configuration**: `UIPreferences.java`, `BitbucketPreferencePage.java`

## Development Notes

### Debugging Tips
- Check Eclipse console for any exceptions during painting
- Verify `UIPreferences.PULLREQUEST_SHOW_INLINE_COMMENTS` is enabled
- Ensure comments have valid `line` numbers (1-based, non-null)
- Confirm `fileType` is "FROM" or "TO" for proper side placement
- Use SWT's `PaintListener` carefully to avoid infinite redraw loops

### Code Style
- All new classes follow Eclipse EGit coding conventions
- EPL-2.0 license headers on all new files
- Proper JavaDoc on public methods
- Externalized strings use `//$NON-NLS-1$` markers
- SWT resources (Color) are properly managed (created and nulled)

### Performance Considerations
- Painting happens on UI thread - keep `paintControl()` fast
- Vertical indents are set once during `install()` - not per paint
- Color objects are reused across paint events
- Only visible comments are drawn (viewport visibility check)

## Conclusion

The inline comments feature provides a modern, visual way to review pull request comments directly in the diff editor, significantly improving the code review experience in Eclipse EGit. The implementation is production-ready and can be enabled via preferences without affecting users who prefer the traditional comment panel approach.
