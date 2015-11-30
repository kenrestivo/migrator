# Migrator

A utility to migrate [Hubzilla](https://github.com/redmatrix/hubzilla) hubs from one server to another, and to migrate Redmatrix hubs to Hubzilla.

The tool runs on your desktop and uses the Hubzilla built-in channel import/export capabilities and a plugin to pull down all of the accounts, channels, and items on a hub. It saves these to your local drive as JSON files, then uploads them to your new server.

This can be used for migrating hubs, and also for replicating them in a distributed manner without the users having to do any work.

## Status

First alpha release 0.1.0

## Requirements

- A Hubzilla hub, running version 1223 or higher, into which you want to put the new accounts, with version 6 or higher of the [Migrator Plugin](https://github.com/kenrestivo/migrator-plugin) installed, enabled, and correctly configured.
- A Hubzilla/Redmatrix hub with your old accounts, running version 1223 or higher, and version 6 or higher of the [Migrator Plugin](https://github.com/kenrestivo/migrator-plugin) installed, enabled, and correctly configured.
- An admin account user login and password on both the old and new server (they don't have to be the same accont or credentials at all).
- Java Runtime (OpenJDK or similar) version 8 (for the binary; the source should compile and run with Java 7)
- A couple hundred MB of RAM (probably not good to run on a VPS).
- Disk space for all the files
- Good reliable fast internet access to both the old and new server
- Some means of automatically restarting the MySQL process on your server if(when) it crashes, especially if your server is memory-constrained.
- Time and patience


## Preparing for migration

1. Assure you have met the requirements as above
2. Set your /etc/my.cnf and /etc/apache2/mods-enabled/mpm_prefork.conf as recommended in [the Hubzilla INSTALL.txt](https://github.com/redmatrix/hubzilla/blob/master/install/INSTALL.txt#L346) with regard to connections and prefork settings.
3. On the new server you are importing to, be sure to set your max_execution time and max_input_time to very large numbers, and your maximum post size very large too, i.e.
```conf
max_execution_time = 300
max_input_time = 300
post_max_size = 100M
upload_max_filesize = 100M

```

4. Delete or expire any accounts from the old server that you don't want to migrate.
5. Make sure your new server is set to automatically restart MySQL if it crashes. Imports might cause it to OOM.
6. If your server uses [SNI](https://en.wikipedia.org/wiki/Server_Name_Indication) (i.e. has more than one SSL site served off of the same IP address), then you'll need to set the
```yaml
insecure: true
```
in your Migrator conf file for that server

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
- MySQL crashes on the server during channel imports, which seems to be a Hubzilla resource-usage problem. You should try the advice in [the Hubzilla INSTALL.txt](https://github.com/redmatrix/hubzilla/blob/master/install/INSTALL.txt#L346) with regard to prefork and mysql settings.
- JVM stacktraces are huge and noisy, and Clojure stacktraces are huger and noisier. Digging through the noise to find the real source of the error is not always easy.
- The Java HTTPClient used by the Migrator doesn't support SNI. So you'll get certificate errors; use "insecure: true" in conf file to avoid/remedy this.

## License

Copyright © 2015 ken restivo <ken@restivo.org>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
