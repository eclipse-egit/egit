# Fix for TreeViewer Content Provider Error

## Issue
The `PullRequestsView` was throwing an `AssertionFailedException` at runtime:
```
assertion failed: Instances of AbstractTreeViewer must have a content provider of type ITreeContentProvider or ITreePathContentProvider
```

## Root Cause
The view was using `IStructuredContentProvider` for a `TreeViewer`, but `TreeViewer` requires either:
- `ITreeContentProvider` 
- `ITreePathContentProvider`

## Solution
Changed the content provider from `IStructuredContentProvider` to `ITreeContentProvider` and implemented the required tree-specific methods:
- `getChildren(Object)` - returns null (flat list, no children)
- `getParent(Object)` - returns null (flat list, no parent)
- `hasChildren(Object)` - returns false (flat list, no children)

## Files Modified
- `/org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/pullrequest/PullRequestsView.java`

## Changes
1. Changed import from `IStructuredContentProvider` to `ITreeContentProvider`
2. Updated content provider implementation to include tree-specific methods
3. Kept `getElements()` implementation unchanged for displaying the flat list of PRs

## Result
The view now properly implements the required content provider interface and should open without errors.
