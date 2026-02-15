# Plan: Bitbucket Data Center Pull Request Integration

**TL;DR**: Add a new "Pull Requests" view to EGit that displays open pull requests from Bitbucket Data Center. This involves creating a REST client for the Bitbucket API, a domain model for pull requests, a new Eclipse ViewPart for listing PRs, and preference/credential handling for Bitbucket server configuration.

## Steps

1. **Create Bitbucket REST client in core**: Add a new `bitbucket` package in [`org.eclipse.egit.core/src/org/eclipse/egit/core/internal`](/home/phkurrle/git/egit/org.eclipse.egit.core/src/org/eclipse/egit/core/internal) with a `BitbucketClient` class using `HttpURLConnection` (similar to [`ConfigureGerritAfterCloneTask`](/home/phkurrle/git/egit/org.eclipse.egit.core/src/org/eclipse/egit/core/op/ConfigureGerritAfterCloneTask.java)) to call the [Bitbucket PR REST API](https://developer.atlassian.com/server/bitbucket/rest/v1000/api-group-pull-requests/).

2. **Add PR domain model**: Create `PullRequest` and `PullRequestList` classes in the new core bitbucket package to represent the JSON responses from the API.

3. **Implement Pull Requests View UI**: Add `PullRequestsView` extending `ViewPart` in [`org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/pullrequest`](/home/phkurrle/git/egit/org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal), following patterns from [`ReflogView`](/home/phkurrle/git/egit/org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/reflog/ReflogView.java) with a `TreeViewer` to display PR list with title, author, and status columns.

4. **Register the view in plugin.xml**: Add view declaration in [`org.eclipse.egit.ui/plugin.xml`](/home/phkurrle/git/egit/org.eclipse.egit.ui/plugin.xml) under `org.eclipse.ui.views` extension point (around line 3646) with Git category, icon, and name.

5. **Add Bitbucket server preference page**: Create a preference page in [`org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/preferences`](/home/phkurrle/git/egit/org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/preferences) to configure Bitbucket server URL and project/repo mapping, using `CredentialsStore` for secure token storage.

6. **Add unit tests**: Create tests for the REST client in [`org.eclipse.egit.core.test`](/home/phkurrle/git/egit/org.eclipse.egit.core.test/src/org) using Hamcrest matchers, and UI tests in [`org.eclipse.egit.ui.test`](/home/phkurrle/git/egit/org.eclipse.egit.ui.test/src/org/eclipse/egit/ui) for the view.

## Further Considerations

1. **Bitbucket authentication**: Bitbucket Data Center supports personal access tokens. Should we support both tokens and username/password via the existing `CredentialsStore`? *Recommend: token-only for API access*

2. **Repository mapping**: How to automatically detect the Bitbucket project/repo from a Git remote URL? *Option A: Parse remote URL pattern / Option B: Manual configuration per repository*

3. **Polling vs manual refresh**: Should the view auto-refresh periodically, or only on user request? *Recommend: Manual refresh with optional auto-refresh interval preference*
