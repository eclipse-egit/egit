# Fork / Upstream Workflow

EGit has built-in support for the common open-source contribution pattern where
you clone your personal fork of a project and want to keep it in sync with the
original ("upstream") repository.

## Automatic fork detection at clone time

When you clone a repository hosted on **GitHub**, **GitLab**, or **Bitbucket**,
EGit queries the platform's REST API to check whether the cloned repository is
a fork. If it is, EGit automatically adds a second remote called `upstream`
pointing to the parent repository, alongside the usual `origin` remote.

The `upstream` remote is configured with a fetch refspec that maps all upstream
branches into `refs/remotes/upstream/*`, exactly as `git fetch upstream` would.

> **Limitation:** Detection only works for public cloud instances
> (`github.com`, `gitlab.com`, `bitbucket.org`). Self-hosted GitLab,
> GitHub Enterprise, Gitea/Forgejo, and Azure DevOps are not supported
> because fork relationships are a hosting-platform concept — the Git
> protocol itself has no notion of a fork.

> **Proxy support:** The API calls use `java.net.HttpURLConnection`, which
> honours Eclipse's "Native" proxy mode including NTLM-authenticated
> system proxies on Windows.

## Push redirect

If a branch's configured push remote is `upstream` and a fork was detected,
EGit assumes you do not have write access to the upstream repository. Instead
of attempting the push and failing, it opens the **Push Branch** wizard
pre-filled with `origin` as the target remote and your local branch name as the
remote branch name, so you can push to your fork with one click.

## Fetch All

A **Fetch All** item is available in the **Team** context menu. It is
equivalent to `git fetch --all` and fetches from every configured remote
sequentially in a background job.

## Pull dialog — Fetch All option

The **Pull** wizard contains a **Fetch all remotes before pulling** checkbox.
When checked, EGit runs a fetch-all job first and chains the pull after it
completes. The checkbox is ticked automatically when a fork/upstream scenario
was detected at clone time, but can be toggled freely on each pull.
