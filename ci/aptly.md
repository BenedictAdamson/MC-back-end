# aptly set up

Jenkins uses Gradle to run `aptly` commands to publish the Debian package to an APT repository named `mc`.
This assumes that the repository has been created, as tje `jenkins` user, using the command
```
aptly repo create mc
```
It then must have been _published_:
```
aptly publish repo -batch -force-overwrite -passphrase-file=/home/jenkins/gpgpassphrase -distribution=bookworm -architectures=all mc mc
```
This allows the build commands to _update_ that repository.