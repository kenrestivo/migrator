# Migrator

A utility to migrate [Hubzilla](https://github.com/redmatrix/hubzilla) hubs from one server to another, and to migrate Redmatrix hubs to Hubzilla.

## Status

Pre-alpha, still work in progress

## Requirements

- A Hubzilla/Redmatrix hub with the most recent version of the [Migrator Plugin](https://github.com/kenrestivo/migrator-plugin) installed, enabled, and correctly configured.
- A Hubzilla hub into which you want to put the new accounts
- Java Runtime (OpenJDK or similar)
- A couple hundred MB of RAM (probably not good to run on a VPS).
- Disk space for all the files
- Good reliable fast internet access to both the old and new server
- Time and patience

## Building

- Download Leiningen

- Build the uberjar
```sh
	lein uberjar
```

## Configuration

TODO document the config file format

## Usage

Run it from the command line as a jar, giving it a config file:

```sh
	java -jar target/migrator.jar path-to-your-config-file.edn
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
