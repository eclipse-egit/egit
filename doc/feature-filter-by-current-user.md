# Feature: Filter Pull Requests by Current User

## Overview
By default, the Pull Requests view now shows only PRs authored by the current authenticated user. This significantly improves performance in large repositories with many open pull requests.

## Implementation Details

### Changes to BitbucketClient

**File**: `/org.eclipse.egit.core/src/org/eclipse/egit/core/internal/bitbucket/BitbucketClient.java`

#### New Method: `getCurrentUser()`
```java
public String getCurrentUser() throws IOException
```
Fetches the current authenticated user's information from the Bitbucket API endpoint `/rest/api/1.0/users/current`.

Returns JSON like:
```json
{
  "name": "jdoe",
  "emailAddress": "jdoe@example.com",
  "displayName": "John Doe",
  ...
}
```

#### Enhanced Method: `getPullRequests()` with User Filters
```java
public String getPullRequests(String projectKey, String repositorySlug, 
    String state, String authorUsername, String reviewerUsername, 
    int limit, int start) throws IOException
```

New parameters:
- `authorUsername` - Filter PRs by author (uses `author.id` query parameter)
- `reviewerUsername` - Filter PRs by reviewer/participant (uses `participant.username` query parameter)

Both parameters are optional (nullable).

### Changes to PullRequestsView

**File**: `/org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/pullrequest/PullRequestsView.java`

#### New Fields
- `showAllPRs` (boolean) - Toggle state for showing all PRs
- `currentUsername` (String) - Cached username of authenticated user
- `showAllPRsAction` (Action) - Toolbar action to toggle filter

#### New Toolbar Button: "Show All"
A checkbox button in the view toolbar that allows users to toggle between:
- **Unchecked (default)**: Shows only PRs authored by current user
- **Checked**: Shows all open PRs in the repository

#### Automatic User Detection
On first refresh:
1. Calls `client.getCurrentUser()`
2. Parses the JSON response to extract username
3. Caches the username for subsequent refreshes
4. Falls back to showing all PRs if user detection fails

#### Title Bar Updates
The view title now shows the filter status:
- `Pull Requests (5) - by jdoe` - Filtered by current user
- `Pull Requests (125) - All` - Showing all PRs

## User Experience

### Initial View Load
1. User opens Pull Requests view
2. View automatically detects current user from token
3. Fetches only PRs authored by current user
4. Displays with user filter indication in title

### Toggling Filter
1. Click "Show All" checkbox in toolbar
2. View refreshes to show all open PRs
3. Title updates to show "All"
4. Uncheck to return to user-filtered view

## Performance Benefits

### Before (Show All PRs)
```
GET /rest/api/1.0/projects/PROJ/repos/repo/pull-requests?state=OPEN&limit=100
→ Returns 125 PRs (all open PRs)
```

### After (Default: Current User Only)
```
GET /rest/api/1.0/projects/PROJ/repos/repo/pull-requests?state=OPEN&author.id=jdoe&limit=100
→ Returns 5 PRs (only user's PRs)
```

**Benefits**:
- 96% reduction in data transferred in this example
- Faster response time from Bitbucket API
- Reduced memory usage in Eclipse
- Faster UI rendering
- Better focus on user's own work

## API Query Parameters Used

### Bitbucket Data Center REST API
The implementation uses these query parameters:

- `state=OPEN` - Only open pull requests
- `author.id={username}` - Filter by PR author
- `participant.username={username}` - Filter by reviewer/participant
- `limit=100` - Maximum 100 results
- `start=0` - Pagination offset

Reference: [Bitbucket REST API Documentation](https://developer.atlassian.com/server/bitbucket/rest/v1000/api-group-pull-requests/)

## Error Handling

### If `getCurrentUser()` Fails
- Falls back to showing all PRs
- Sets `currentUsername` to empty string
- View still works, just without filtering
- User can still toggle "Show All"

### If User Not Authenticated
- API returns 401 Unauthorized
- Error displayed in Error Log view
- View shows "Not configured" message
- User directed to configure credentials in Preferences

## Future Enhancements

### Possible Additional Filters
1. **PRs where user is reviewer** - Add "Show as Reviewer" option
2. **Combination filter** - Author OR Reviewer
3. **Multiple users** - Filter by team members
4. **State filters** - Show MERGED or DECLINED PRs
5. **Search/filter box** - Free-text search in view

### Preference Setting
Add preference: "Default PR filter"
- Options: Current User (default), All PRs, As Reviewer

### Dropdown Filter
Replace checkbox with dropdown:
- My PRs
- PRs I'm reviewing
- All PRs
- Custom...

## Testing

### Manual Test Scenarios

#### Scenario 1: Default Behavior
1. Configure Bitbucket credentials
2. Open Pull Requests view
3. Verify: Only your PRs shown
4. Verify: Title shows "by {username}"

#### Scenario 2: Toggle to All
1. Click "Show All" checkbox
2. Verify: View refreshes
3. Verify: More PRs displayed
4. Verify: Title shows "All"

#### Scenario 3: Toggle Back
1. Uncheck "Show All"
2. Verify: View filters to user's PRs again
3. Verify: Title shows "by {username}"

#### Scenario 4: Refresh Behavior
1. Click Refresh button
2. Verify: Filter persists (doesn't reset)
3. Verify: Data updates according to current filter

### Test with Large Repository
Test the performance improvement:
1. Repository with 500+ open PRs
2. User has 10 PRs
3. Compare load times:
   - Show All: ~3-5 seconds
   - User filter: <1 second

## Code Example

### Simple Usage
```java
BitbucketClient client = new BitbucketClient(serverUrl, token);

// Get current user
String userJson = client.getCurrentUser();
// Extract username from JSON...

// Fetch only current user's PRs
String prs = client.getPullRequests(
    "PROJ", "repo", 
    "OPEN",      // state
    "jdoe",      // author filter
    null,        // reviewer filter
    100, 0       // limit, start
);
```

### With Error Handling
```java
BitbucketClient client = new BitbucketClient(serverUrl, token);
String username = null;

try {
    String userJson = client.getCurrentUser();
    username = parseUsername(userJson);
} catch (IOException e) {
    // Log error, continue without filter
    log.error("Could not fetch current user", e);
}

// Fetch PRs with optional filter
String prs = client.getPullRequests(
    projectKey, repoSlug,
    "OPEN", username, null, 100, 0
);
```

## Related Files

Modified:
- `/org.eclipse.egit.core/src/org/eclipse/egit/core/internal/bitbucket/BitbucketClient.java`
- `/org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/pullrequest/PullRequestsView.java`

## Summary

✅ **Performance**: Reduces data load by filtering PRs to current user by default  
✅ **User Control**: Toggle button allows switching to "Show All" when needed  
✅ **Automatic**: User detection happens automatically, no configuration needed  
✅ **Robust**: Falls back to showing all PRs if user detection fails  
✅ **Clear**: View title clearly indicates current filter state
