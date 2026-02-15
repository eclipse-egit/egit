# OSGi Classloading Fix for NoClassDefFoundError

## The Problem

When running the Eclipse Application, you got:
```
java.lang.NoClassDefFoundError: org/eclipse/egit/core/internal/bitbucket/BitbucketClient
Caused by: java.lang.ClassNotFoundException: org.eclipse.egit.core.internal.bitbucket.BitbucketClient 
  cannot be found by org.eclipse.egit.ui_7.6.0.qualifier
```

## Why This Happened

### Compile-Time vs Runtime in OSGi

In OSGi (Eclipse's plugin system), there are two different mechanisms for package visibility:

1. **Compile-Time** (PDE - Plugin Development Environment):
   - Uses `Export-Package` with `x-friends` in MANIFEST.MF
   - Eclipse IDE respects x-friends and allows compilation
   - Shows "Access Restriction" warnings but compiles successfully

2. **Runtime** (OSGi Container):
   - Requires explicit `Import-Package` declarations
   - `x-friends` is a PDE-only concept, NOT part of OSGi spec
   - At runtime, OSGi only checks Import-Package/Export-Package matches

### What We Had Before

**org.eclipse.egit.core/META-INF/MANIFEST.MF:**
```
Export-Package: org.eclipse.egit.core.internal.bitbucket;version="7.6.0";
  x-friends:="org.eclipse.egit.ui"
```

**org.eclipse.egit.ui/META-INF/MANIFEST.MF:**
```
Import-Package: org.eclipse.egit.core.internal;version="[7.6.0,7.7.0)",
  (no bitbucket package imported)
```

Result:
- ✅ Compiles fine (PDE sees x-friends)
- ❌ Runtime fails (OSGi doesn't see the import)

## The Fix

Added explicit runtime import to **org.eclipse.egit.ui/META-INF/MANIFEST.MF**:

```
Import-Package: org.eclipse.egit.core.internal;version="[7.6.0,7.7.0)",
 org.eclipse.egit.core.internal.bitbucket;version="[7.6.0,7.7.0)",
 org.eclipse.egit.core.internal.credentials;version="[7.6.0,7.7.0)",
```

Now:
- ✅ Compiles fine (x-friends)
- ✅ Runtime works (explicit import)

## Key Takeaway

**When using internal packages across bundles in OSGi:**

1. In the **exporting bundle** (org.eclipse.egit.core):
   ```
   Export-Package: my.internal.package;x-friends:="consumer.bundle"
   ```

2. In the **consuming bundle** (org.eclipse.egit.ui):
   ```
   Import-Package: my.internal.package;version="[X.Y.Z,X.Y+1.0)"
   ```

Both are required:
- Export-Package (with x-friends) → compile-time visibility
- Import-Package → runtime visibility

## How to Apply This Fix

### Step 1: Clean Everything
```
Project > Clean... > Clean all projects
```

### Step 2: Refresh Projects
- Right-click on `org.eclipse.egit.core` → Refresh
- Right-click on `org.eclipse.egit.ui` → Refresh

### Step 3: Rebuild
Eclipse should auto-rebuild. If not:
```
Project > Build All
```

### Step 4: Restart Eclipse Application
- Stop any running Eclipse Application instances
- Launch again from Run Configurations

## Verification

The fix is working when:
1. Eclipse Application starts without errors
2. You can open: Window > Show View > Other... > Git > Pull Requests
3. The view opens without NoClassDefFoundError
4. You can open: Window > Preferences > Git > Bitbucket

## Why This Is Common in EGit

Look at the pattern in `org.eclipse.egit.ui/META-INF/MANIFEST.MF`:

```
Import-Package: org.eclipse.egit.core;version="[7.6.0,7.7.0)",
 org.eclipse.egit.core.internal;version="[7.6.0,7.7.0)",
 org.eclipse.egit.core.internal.credentials;version="[7.6.0,7.7.0)",
 org.eclipse.egit.core.internal.efs;version="[7.6.0,7.7.0)",
 org.eclipse.egit.core.internal.gerrit;version="[7.6.0,7.7.0)",
 org.eclipse.egit.core.internal.hosts;version="[7.6.0,7.7.0)",
 ...
```

**Every internal package from core that UI uses must be explicitly imported!**

This is standard practice in EGit - we just followed the same pattern for the new `bitbucket` package.

## Related Reading

- [OSGi Bundle Manifest Headers](https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html)
- [Eclipse PDE x-friends](https://help.eclipse.org/latest/topic/org.eclipse.pde.doc.user/reference/pde_manifest.htm)
- [Understanding OSGi Dependencies](https://www.vogella.com/tutorials/OSGi/article.html)

## Similar Issues You Might Encounter

### NoClassDefFoundError for PullRequest class

**Error:**
```
java.lang.NoClassDefFoundError: org/eclipse/egit/core/internal/bitbucket/PullRequest
```

**Solution:** Same fix - the Import-Package statement covers the entire package, including all classes:
- BitbucketClient
- PullRequest  
- PullRequestList
- All nested classes

### Fragment bundles (tests)

Test bundles are **fragments**, not regular bundles. They:
- Don't need Import-Package (they share host's classloader)
- Must declare Fragment-Host in their MANIFEST.MF
- Can access all host bundle classes directly

Example: `org.eclipse.egit.core.test` is a fragment of `org.eclipse.egit.core`
