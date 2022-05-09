# Security Policy

## Versioning 

lighty version contains 3 numbers representing MAJOR.MINOR.PATCH (X.Y.Z) levels.

MAJOR number is mapped to major OpenDaylight release
| lighty | OpenDaylight    |
|--------|-----------------|
| 16.Y.Z | Sulfur (16)     |
| 15.Y.Z | Phosphorus (15) |
| ...    |                 |

MINOR number is mapped to OpenDaylight service release (SR1, SR2, SR3, ..)
| lighty | OpenDaylight    |
|--------|-----------------|
| 16.1.Z | Sulfur SR1      |
| 16.2.Z | Sulfur SR2      |
| ...    |                 |

PATCH number represents lighty release, usually security & bug fixes.

## Supported Versions

Two most recent OpenDaylight versions (MAJOR) are supported by lighty, always with the latest OpenDaylight service release (MINOR)
| Version              | Supported          |
| ------------------   | -------------------|
| MAJOR.MINOR          | :white_check_mark: |
| MAJOR.MINOR-(1..4)   | :x:                |
| MAJOR-1.MINOR        | :white_check_mark: |
| MAJOR-1.MINOR-(1..4) | :x:                |
| < MAJOR-1            | :x:                |

## Reporting a Vulnerability

Please report any discovered or suspected security vulnerabilities to PANTHEON.tech product security team at secalert@pantheon.tech.

