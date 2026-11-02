# Releasing nRF Toolbox

A release is made by the **Release** GitHub Action. It runs the unit tests, creates the tag and
deploys the app to Google Play (Internal track). The version of the release is the name of the tag,
e.g. `11.0.0`.

## How to release

1. Merge everything that should be released to `main`.
2. Open **Actions → Release → Run workflow**.
3. Choose the branch in **Use workflow from**, e.g. `main`.
4. Enter the **version**, e.g. `11.0.0`. To try the release first, tick **Dry run** (see
   [Dry run](#dry-run)). Run the workflow.

The workflow runs in stages. Each one is a job that depends on the previous one, so the graph of the
run shows which stage failed:

1. **Check.** Checks that it runs on a branch, that the version looks like `11.0.0` or
   `11.0.0-alpha01`, and that the tag does not exist yet.
2. **Test.** Runs the unit tests (`./gradlew testDebugUnitTest`), currently the tests of the
   data parsers (`profile_parsers`).
3. **Tag.** Creates the tag `<version>` on the tested commit and pushes it.
4. **Deploy.** Builds the signed app bundle from the tag and uploads it to the
   Internal track of Google Play using Fastlane (`bundle exec fastlane deployInternal`).
5. **GitHub release.** Creates a GitHub release for the tag with auto-generated notes (the pull
   requests merged since the previous tag), only after **Deploy** succeeded. A version with a suffix,
   e.g. `11.0.0-alpha01`, is marked as a pre-release.

Pushing a tag does not start the **Release** workflow, and the only way to release is to run it.

After the workflow finished, check that **Deploy** succeeded. Edit the notes of the GitHub release if
needed.

## Dry run

With **Dry run** ticked the workflow goes through all the stages, but nothing leaves the runner:

1. **Check** passes also for a version that was already released (it only warns that the tag exists).
2. **Test** runs the unit tests.
3. **Tag** creates the tag locally, on the runner, and does not push it.
4. **Deploy** builds and signs the app bundle, and Google Play only validates it (`validate_only`),
   nothing is uploaded.
5. **GitHub release** does not create the release, but prints the release notes it would use.

Use it to check a change of the release workflow, or the secrets, before a real release.

## How the version is set

The Nordic Gradle plugins use the version name from (in this order):

1. the `releaseVersion` Gradle property, e.g. `-PreleaseVersion=11.0.0`,
2. the `RELEASE_VERSION` environment variable,
3. the latest git tag,
4. `0.0.0` if none of the above is available.

The **Release** workflow sets `RELEASE_VERSION`. The version code of the app is based on the number of
tags, so **Deploy** checks out the whole history.

## Fastlane

The GitHub runners do not have Fastlane installed. **Deploy** installs it with `ruby/setup-ruby`
from `Gemfile` and `Gemfile.lock`, and runs it with `bundle exec`. To update Fastlane, run
`bundle update fastlane` and commit the `Gemfile.lock`.

## Troubleshooting

* **A stage failed.** Open the run, fix the cause (e.g. a failing test needs a pull request first),
  and use **Re-run failed jobs**. The stages that succeeded are not repeated; the tag stays.
  If the cause needs a change in the code, delete the tag, if it was created, and run the workflow
  again.
* **Check: the tag already exists.** The version was released, or a previous run failed after it
  created the tag. Delete the tag if nothing was deployed (`git push origin :refs/tags/<version>`),
  or use a new version.
* **Deploy failed.** Use **Re-run failed jobs**. The tag is built again.
