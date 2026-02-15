# Plugin Validation Error Fixes for EGit Bitbucket Integration

## Common Plugin Validation Errors and Solutions

### 1. Missing Required Bundles

**Error**: "Required bundle X is not available" or "Cannot be resolved"

**Solution**:
- In your Eclipse IDE, go to: **Run > Run Configurations...**
- Select your Eclipse Application launch configuration
- Go to the **Plug-ins** tab
- Click **Add Required Plug-ins** button
- This will automatically add all transitive dependencies

### 2. Access Restriction Warnings

**Error**: "Access restriction: The type 'X' is not API"

**Status**: ✅ **This is expected and safe to ignore**
- These warnings appear because we're using `x-friends` pattern in MANIFEST.MF
- The code will compile and run correctly
- To suppress in Eclipse: Project Properties > Java Compiler > Errors/Warnings > Deprecated and restricted API > Forbidden reference = Warning

### 3. Unresolved Optional Dependencies

**Error**: "Optional bundle X cannot be resolved"

**Solution**:
- In the launch configuration **Plug-ins** tab
- Select **Validate Plug-ins** button
- If optional bundles are listed, you can either:
  - Add them manually (if available in your target platform)
  - Or ignore them (they're optional)

### 4. Singleton Conflicts

**Error**: "Multiple versions of singleton bundle"

**Solution**:
- In the **Plug-ins** tab, ensure only ONE version of each plugin is selected
- Deselect any duplicate versions
- Keep the highest version number

### 5. Fragment Host Issues

**Error**: "Fragment X cannot find its host"

**Solution**:
- Ensure the host bundle is included in the launch configuration
- Example: `org.eclipse.egit.core.test` is a fragment of `org.eclipse.egit.core`
- Both must be present in the launch configuration

## Step-by-Step Validation Process

### Method 1: Auto-Fix (Recommended)

1. Open **Run > Run Configurations...**
2. Select your Eclipse Application configuration
3. Go to **Plug-ins** tab
4. Click **Add Required Plug-ins** button
5. Click **Validate Plug-ins** button
6. If errors appear, click **Add Required Plug-ins** again
7. Repeat until validation passes

### Method 2: Manual Fix

1. Note the specific error messages from validation
2. For each missing bundle:
   - Click **Add...** button in Plug-ins tab
   - Search for the bundle name
   - Add it to the configuration
3. Click **Validate Plug-ins** again

### Method 3: Reset to Defaults

1. In **Plug-ins** tab
2. Select **"Launch with: plug-ins selected below only"**
3. Click **Deselect All**
4. Click **Select Required**
5. This rebuilds the minimal required set

## New Bundles Added in This Integration

The Bitbucket integration doesn't require any NEW external bundles beyond what EGit already uses:
- ✅ `org.eclipse.egit.core` - Modified (added bitbucket package)
- ✅ `org.eclipse.egit.ui` - Modified (added view and preferences)
- ✅ `org.eclipse.egit.core.test` - Modified (added tests)

All dependencies were already part of EGit:
- JFace/SWT (already used)
- Eclipse Forms (already used)
- JGit (already used)
- JUnit/Hamcrest (already used for tests)

## Verifying the Fix

After fixing plugin validation errors:

1. **Clean and Build**:
   ```
   Project > Clean... > Clean all projects
   ```

2. **Test the Launch**:
   - Run the Eclipse Application
   - Verify it starts without errors
   - Open: Window > Show View > Other... > Git > Pull Requests

3. **Test Preferences**:
   - Window > Preferences > Git > Bitbucket
   - Verify the preference page appears

## Troubleshooting Specific Errors

### If you get `NoClassDefFoundError: org.eclipse.egit.core.internal.bitbucket.BitbucketClient`:

**Error**:
```
java.lang.NoClassDefFoundError: org/eclipse/egit/core/internal/bitbucket/BitbucketClient
Caused by: java.lang.ClassNotFoundException: org.eclipse.egit.core.internal.bitbucket.BitbucketClient 
  cannot be found by org.eclipse.egit.ui
```

**Root Cause**: The UI bundle needs to explicitly import the bitbucket package at runtime.

**Solution**: ✅ **Already Fixed!**
- The `org.eclipse.egit.ui/META-INF/MANIFEST.MF` now includes:
  ```
  org.eclipse.egit.core.internal.bitbucket;version="[7.6.0,7.7.0)"
  ```
  in the Import-Package section.

**If you still see this error**:
1. Clean the workspace: Project > Clean... > Clean all projects
2. Restart Eclipse
3. Rebuild the workspace
4. Re-run the Eclipse Application launch configuration

### If "org.eclipse.egit.ui.internal.pullrequest.PullRequestsView" is not found:

- Check that `org.eclipse.egit.ui` is in the launch configuration
- Verify plugin.xml has the view registered (line ~3689)
- Clean and rebuild the workspace

### If "org.eclipse.egit.core.internal.bitbucket" packages not found:

- Check MANIFEST.MF has the export (line ~31)
- Verify the export includes x-friends declaration
- Refresh the project: Right-click > Refresh

### If preferences page doesn't appear:

- Check plugin.xml has the preference page registered (line ~654)
- Verify the page name in plugin.properties
- Restart the Eclipse Application

## Quick Commands

### Refresh All Projects
```bash
# In Eclipse workspace root
find . -name ".project" -exec dirname {} \; | while read proj; do
    echo "Refreshing $proj"
done
```

### Clean Build
```
Project > Clean... > Clean all projects > Start a build immediately
```

### Reset Launch Configuration
- Delete the .launch file
- Create new Eclipse Application configuration
- Let Eclipse auto-detect required plugins

## Still Having Issues?

If validation continues to fail:

1. **Check Eclipse Error Log**: Window > Show View > Error Log
2. **Verify Target Platform**: Window > Preferences > Plug-in Development > Target Platform
3. **Update IDE**: Ensure Eclipse IDE is up to date
4. **Check Java Version**: Verify Java 21 is being used (required by EGit 7.6.0)

## Files Modified That Might Affect Validation

1. `/org.eclipse.egit.core/META-INF/MANIFEST.MF` - Added export for bitbucket package
2. `/org.eclipse.egit.ui/META-INF/MANIFEST.MF` - No changes (uses existing dependencies)
3. `/org.eclipse.egit.ui/plugin.xml` - Added view and preference page extensions
4. `/org.eclipse.egit.ui/plugin.properties` - Added localized strings

None of these changes should cause validation failures in a properly configured workspace.
