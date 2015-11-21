# Migrator

A utility to migrate [Hubzilla](https://github.com/redmatrix/hubzilla) hubs from one server to another, and to migrate Redmatrix hubs to Hubzilla.

## Status

Pre-alpha, still work in progress

## Requirements

- A Hubzilla/Redmatrix hub with the most recent version of the [Migrator Plugin](https://github.com/kenrestivo/migrator-plugin) installed, enabled, and correctly configured, and the most recent version of Hubzilla.
- A Hubzilla hub into which you want to put the new accounts, with the most recent version of the [Migrator Plugin](https://github.com/kenrestivo/migrator-plugin) installed, enabled, and correctly configured, and the most recent version of Hubzilla.
- An admin account user login and password on both the old and new server (they don't have to be the same accont or credentials at all).
- Java Runtime (OpenJDK or similar)
- A couple hundred MB of RAM (probably not good to run on a VPS).
- Disk space for all the files
- Good reliable fast internet access to both the old and new server
- Time and patience


## Preparing for migration

1. Assure you have met the requirements as above
2. Delete or expire any accounts from the old server that you don't want to migrate.

## Downloading

Pre-built JAR files are available from:
TODO: link

## Configuration

The basic configuration file is:
```yaml
fetch:
    base-url: http://hubzilla
    pw: test
    login: testes

push:
    base-url: http://anotherhubzilla
    pw: test
    login: testes

storage:
    save-directory: /mnt/sdcard/tmp/out

```
There are also parameters for connection timeout, number of retries, etc. which you might need to mess with if your server is crashing or your network is especially flaky. There's a complete example in the repository.

## Usage

Run it from the command line as a jar, giving it a config file:

```sh
	java -jar target/migrator.jar path-to-your-config-file.edn
```


## Building

- Download Leiningen

- Build the uberjar
```sh
	lein uberjar
```
## Development

You can run it without making a uberjar by just
```sh
	lein run path-to-your-config-file
```

## License

Copyright © 2015 ken restivo <ken@restivo.org>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
