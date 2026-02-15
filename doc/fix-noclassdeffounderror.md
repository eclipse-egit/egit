# Fix Applied: NoClassDefFoundError for BitbucketClient

## Issue
```
java.lang.NoClassDefFoundError: org/eclipse/egit/core/internal/bitbucket/BitbucketClient
  at org.eclipse.egit.ui.internal.pullrequest.PullRequestsView$8.run(PullRequestsView.java:264)
Caused by: java.lang.ClassNotFoundException: org.eclipse.egit.core.internal.bitbucket.BitbucketClient 
  cannot be found by org.eclipse.egit.ui_7.6.0.qualifier
```

## Root Cause
The `org.eclipse.egit.ui` bundle could not load the `BitbucketClient` class at runtime because:
1. The package was exported with `x-friends` (compile-time only)
2. But was NOT imported in the UI bundle's Import-Package (required for runtime)

In OSGi:
- **x-friends** = PDE compile-time visibility
- **Import-Package** = OSGi runtime classloading

Both are needed when crossing bundle boundaries!

## Fix Applied

### File: `/org.eclipse.egit.ui/META-INF/MANIFEST.MF`

**Added to Import-Package section (line 43):**
```
org.eclipse.egit.core.internal.bitbucket;version="[7.6.0,7.7.0)",
```

**Complete Import-Package section now includes:**
```
Import-Package: org.eclipse.egit.core;version="[7.6.0,7.7.0)",
 org.eclipse.egit.core.attributes;version="[7.6.0,7.7.0)",
 org.eclipse.egit.core.credentials;version="[7.6.0,7.7.0)",
 org.eclipse.egit.core.info;version="[7.6.0,7.7.0)",
 org.eclipse.egit.core.internal;version="[7.6.0,7.7.0)",
 org.eclipse.egit.core.internal.bitbucket;version="[7.6.0,7.7.0)",    ← NEW
 org.eclipse.egit.core.internal.credentials;version="[7.6.0,7.7.0)",
 ...
```

### File: `/org.eclipse.egit.core/META-INF/MANIFEST.MF`

**Already had (no change needed):**
```
Export-Package: ...
 org.eclipse.egit.core.internal.bitbucket;version="7.6.0";x-friends:="org.eclipse.egit.ui,org.eclipse.egit.core.test",
 ...
```

## How to Apply This Fix

### Option 1: Already Applied (If you're reading this)
The fix is already in the files. Just:

1. **Clean workspace:**
   ```
   Project > Clean... > Clean all projects > OK
   ```

2. **Wait for auto-rebuild** (watch progress bar in bottom-right)

3. **Restart your Eclipse Application launch configuration**

### Option 2: Manual Verification

If you need to verify or manually apply:

1. Open: `/org.eclipse.egit.ui/META-INF/MANIFEST.MF`

2. Find the `Import-Package:` section

3. Ensure this line exists:
   ```
   org.eclipse.egit.core.internal.bitbucket;version="[7.6.0,7.7.0)",
   ```

4. It should be alphabetically between:
   - `org.eclipse.egit.core.internal;version="[7.6.0,7.7.0)",`
   - `org.eclipse.egit.core.internal.credentials;version="[7.6.0,7.7.0)",`

5. Save and let Eclipse rebuild

## Verification Steps

After applying the fix:

### 1. Check for Compilation Errors
- Problems view should show no errors (warnings are OK)
- Only "Access restriction" warnings expected (these are safe)

### 2. Run Eclipse Application
```
Run > Run Configurations... > Eclipse Application > Run
```

### 3. Open the Pull Requests View
```
Window > Show View > Other... > Git > Pull Requests
```

**Expected result:** View opens without errors

### 4. Check the Error Log
```
Window > Show View > Error Log
```

**Expected result:** No NoClassDefFoundError or ClassNotFoundException

### 5. Test the Preferences Page
```
Window > Preferences > Git > Bitbucket
```

**Expected result:** Preference page appears with fields for:
- Server URL
- Project Key
- Repository Slug
- Personal Access Token

## Why This Pattern Exists in EGit

Check any internal package usage in EGit - they ALL follow this pattern:

```bash
# In org.eclipse.egit.ui/META-INF/MANIFEST.MF
grep "org.eclipse.egit.core.internal" MANIFEST.MF
```

You'll see:
- `org.eclipse.egit.core.internal`
- `org.eclipse.egit.core.internal.credentials`
- `org.eclipse.egit.core.internal.efs`
- `org.eclipse.egit.core.internal.gerrit` ← Similar to our bitbucket!
- `org.eclipse.egit.core.internal.hosts`
- etc.

**Every internal package that UI uses is explicitly imported!**

We simply followed the established pattern.

## Common Questions

### Q: Why not use Require-Bundle instead?
**A:** Import-Package is preferred because:
- More flexible (version ranges)
- Better encapsulation
- Less coupling between bundles
- Standard OSGi best practice

### Q: Will this work for the test bundle?
**A:** Yes! The test bundle (`org.eclipse.egit.core.test`) is a **fragment**, which means:
- It shares the host bundle's classloader
- No Import-Package needed
- It can access all classes in `org.eclipse.egit.core` automatically

### Q: Do I need to change version numbers?
**A:** No! The version range `[7.6.0,7.7.0)` means:
- Minimum: 7.6.0 (inclusive)
- Maximum: 7.7.0 (exclusive)
- This matches the current EGit version

## Status

✅ **FIX APPLIED AND READY**

The NoClassDefFoundError should no longer occur after:
1. Cleaning the workspace
2. Rebuilding
3. Restarting the Eclipse Application

If you still see the error after these steps, check:
- Both MANIFEST.MF files have the changes
- Eclipse has auto-built (check timestamp on .class files)
- No typos in the package name

## Related Documentation

See also:
- `/doc/osgi-classloading-fix.md` - Detailed explanation of OSGi classloading
- `/doc/plugin-validation-troubleshooting.md` - General troubleshooting guide
- `/doc/bitbucket-implementation-summary.md` - Complete implementation overview
