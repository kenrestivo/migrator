# Migrator

A utility to migrate [Hubzilla](https://github.com/redmatrix/hubzilla) hubs from one server to another, and to migrate Redmatrix hubs to Hubzilla.

The tool runs on your desktop and uses the Hubzilla built-in channel import/export capabilities and a plugin to pull down all of the accounts, channels, and items on a hub. It saves these to your local drive as JSON files, then uploads them to your new server.

This can be used for migrating hubs, and also for replicating them in a distributed manner without the users having to do any work.

## Status

First alpha release 0.1.0

## Requirements

- A Hubzilla hub, running version 1223 or higher, into which you want to put the new accounts, with version 6 or higher of the [Migrator Plugin](https://github.com/kenrestivo/migrator-plugin) installed, enabled, and correctly configured, and the most recent version of Hubzilla.
- A Hubzilla/Redmatrix hub with your old accounts, running version 1223 or higher, and version 6 or higher of the [Migrator Plugin](https://github.com/kenrestivo/migrator-plugin) installed, enabled, and correctly configured, and the most recent version of Hubzilla.
- An admin account user login and password on both the old and new server (they don't have to be the same accont or credentials at all).
- Java Runtime (OpenJDK or similar) version 8.
- A couple hundred MB of RAM (probably not good to run on a VPS).
- Disk space for all the files
- Good reliable fast internet access to both the old and new server
- Some means of automatically restarting the MySQL process on your server if(when) it crashes, especially if your server is memory-constrained.
- Time and patience


## Preparing for migration

1. Assure you have met the requirements as above
2. Delete or expire any accounts from the old server that you don't want to migrate.
3. Make sure your server is set to automatically restart MySQL if it crashes. Imports might cause it to OOM.

## Downloading

[A pre-built JAR file](https://hub.spaz.org/cloud/bamfic/migrator/migrator.jar) is available from the Hubzilla matrix


## Configuration

The basic configuration file is:
```yaml
fetch:  # Settings for your old server
    base-url: http://hubzilla  # What it says. You can include a http://host:port too.
    pw: test  
    login: test

push:  # Settings for your NEW server. Logins and passwords can be different.
    base-url: http://anotherhubzilla  # This had better be a different URL than your old server!
    seize: false # Relevant only for push. If true, will take control of the imported channels from other hubs.
    pw: test
    login: test

storage: # Where to save the JSON files. This must be readable and writable.
    save-directory: /mnt/sdcard/tmp/out

```
There are also parameters for connection timeout, number of retries, etc. which you might need to mess with if your server is crashing or your network is especially flaky. There's [a complete example](https://github.com/kenrestivo/migrator/blob/master/resources/config/complete-config.yml) in the repository.

## Usage

Run it from the command line as a jar, giving it a config file:

```sh
	java -jar target/migrator.jar path-to-your-config-file.yml
```

## Debugging
Hubzilla imports tend to crash MySQL with out-of-memory errors. The log file saves the history of the migrator as it's running. You can set debug level higher if needed, in the config file.


## Building

- Download Leiningen

- Build the uberjar
```sh
	lein uberjar
```
## Development

You can run it without making a uberjar by just
```sh
	lein run path-to-your-config-file.yml
```

## Known Bugs

- Directory doesn't seem to update after migration. Not sure why yet.
- MySQL crashes on the server during channel imports, which seems to be a Hubzilla resource-usage problem. You might try turning off your poller.php cron job while your imports are going; the poller appears likely to be locking up the system.

## License

Copyright © 2015 ken restivo <ken@restivo.org>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
