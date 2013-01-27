arsa
====

Arduino Radio Spectrum Analyzer

An application using JFreeChart that visualizes the maximum and current signal strengths
read from a serial port, from a device which measures signal strengths on the 2.4GHz band.

It is designed for a custom built 2.4GHz radio spectrum analyzer built with an Atmega328 Microcontroller and CYWM6935 radio module.
The Arduino MCU can be programmed using the Arduino IDE and the CYWM6935 module, found at https://github.com/wa5znu/CYWM6935/

It can be used with any device which produces information about signal strength on a serial port.
The SerialReader class specifies how data is expected on the serial port. It can also be overridden
quite easily for devices which present the data in a different way on the serial port.

For more information also see http://nurdspace.nl/Arduino_Radio_Spectrum_Analyzer
