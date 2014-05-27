# Qeo Open Source Project #

Welcome to the Qeo Open Source Project!

## About Qeo ##

Qeo is a software framework that allows devices to easily exchange data with other devices on the same network based on a publish-subscribe paradigm.

- Break the silos. Qeo defines a set of standard datamodels that allows your application to interact with a wide range of devices, regardless of their manufacturer.
- Secure. All communication between different devices is encrypted.
- Access control. The end-user has full control over what data can be accessed by which other user/device/application.
- Beyond the local network. Devices that are not in the local network can still exchange data with that network by connecting to a forwarder.

More information about Qeo can be found on the [qeo.org](http://www.qeo.org/) website.

## Supported Platforms ##

The open source version of Qeo is validated on Redhat Enterprice Linux version 6.1 and Ubuntu version 12 using GNU Make 3.81. Other distributions might be supported as well. 

Before building the source, make sure your system meets the following requirements:

- A 32-bit or 64-bit Linux system.
- 300MB of free disk space.

## Building ##

You can build the open source version using the `build.sh` script. This script takes one argument to specify the directory to which the resulting artifacts will be copied.

    $ ./build.sh install

## Documentation ##

The documentation of this project can be found on <http://qeo.github.io/>.

The Qeo Open Source Project Documentation is made available under the [GNU Free Documentation License V1.3](http://www.gnu.org/licenses/fdl-1.3.en.html).

Copyright (c) 2014 - Qeo LLC

## License ##

The Qeo Open Source Project is made available under the Clear BSD License, and the majority of the QEO Open Source Project components are therefore licensed under [Clear BSD License](http://directory.fsf.org/wiki/License:ClearBSD).

The Qeo Open Source Project also includes third party open source software components. See LICENSE file for more details.

Copyright (c) 2014 - Qeo LLC

## Trademark ##

Qeo is a Registered Trademark. For more information and terms of use, contact <opensource@qeo.org>.
