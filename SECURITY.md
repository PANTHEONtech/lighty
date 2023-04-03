# Security Policy

## Versioning 

Lighty.io version contains 3 numbers representing MAJOR.MINOR.PATCH (X.Y.Z) levels.

MAJOR number is mapped to major OpenDaylight release
| Lighty.io | OpenDaylight    |
|-----------|-----------------|
| 18.Y.Z    | Argon (18)      |
| 17.Y.Z    | Chlorine (17)   |
| ...       |                 |

MINOR number is mapped to OpenDaylight service release (SR1, SR2, SR3, ..)
| Lighty.io | OpenDaylight    |
|-----------|-----------------|
| 18.1.Z    | Argon SR1       |
| 18.2.Z    | Argon SR2       |
| ...       |                 |

PATCH number represents Lighty.io release, usually security & bug fixes.

## Supported Versions

Two most recent OpenDaylight versions (MAJOR) are supported by Lighty.io, always with the latest OpenDaylight service release (MINOR)
| Version              | Supported          |
| ------------------   | -------------------|
| MAJOR.MINOR          | :white_check_mark: |
| MAJOR.MINOR-(1..4)   | :x:                |
| MAJOR-1.MINOR        | :white_check_mark: |
| MAJOR-1.MINOR-(1..4) | :x:                |
| < MAJOR-1            | :x:                |

## Reporting a Vulnerability

Please report any discovered or suspected security vulnerabilities to PANTHEON.tech product security team at secalert@pantheon.tech.
