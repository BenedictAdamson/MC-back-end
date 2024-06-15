# aptly set up

Jenkins uses Gradle to run `aptly` commands to publish the Debian package to an APT repository named `mc`.
This assumes that the repository has been created, as tje `jenkins` user, using the command
```
aptly repo create mc
```