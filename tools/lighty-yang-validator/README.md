# Lighty-yang-validator

The idea is copied from pyang python validation tool

This tool validates the yang module using yangtools parser and if
there are any problems parsing the module it will show stacktrace
with problem to the corresponding module.

## Compile, Build and generate distribution
Go to the root directory (/lighty-yang-validator/) and use command:
**mvn clean install**

The distribution will be stored in the **"target"** directory as
file lighty-yang-validator-13.1.1-SNAPSHOT-bin.zip

## Running application from distribution
First unzip the distribution:

unzip lighty-yang-validator-13.1.1-SNAPSHOT-bin.zip

Enter the directory to which the distribution was extracted to.
Run the script with command

**./lyv \<options>**

To parse the module we need to have all the imported and included
modules of a testing module on the same path or we need to add a
**-p, --path** option with a path or column separated paths to the
modules that are needed. Use **-r, --recursive** option to search
for the files recursively within the file structure.

## Other options

Use **-o, --output** option to specify path to the output file directory
for logs.

Use **-a, --parse-all** option to parse all files within given directory.
 This option can be used with -p option.

Use **-p, --path** option to specify path as a colon (:) separated list of
directories to search for yang modules.

Use **-r, --recursive** option to specify recursive search of directories
specified by **-p, --path** option.

Use **-m, --module-name** option to search for file by module name
instead of specifying the whole path.

Use **-e, --features** option to prune the data model by removing
all nodes that are defined with a "if-feature".

Use **-h, --help** option to print help message and exit.

Use **-f, --format** option to specify output format. Supported
formats: tree, depend, yang, json-tree, jstree, name-revision.

Use **-s, --simplify** option to to simplify the yang file. Yang file
will be simplified based on the nodes used in xml file. Use with
-o to specify output directory otherwise it will be printed out to
stdout.

## Formats

* tree - tree is printed in following format *\<status>--\<flags> \<name>\<opts> \<type> <if-features>*

 \<status> is one of:

    +  for current
    x  for deprecated
    o  for obsolete

 \<flags> is one of:

    rw  for configuration data
    ro  for non-configuration data, output parameters to rpcs
       and actions, and notification parameters
    -w  for input parameters to rpcs and actions
    -x  for rpcs and actions
    -n  for notifications

 \<name> is the name of the node:

    (<name>) means that the node is a choice node
    :(<name>) means that the node is a case node

 \<opts> is one of:

    ?  for an optional leaf, choice
    *  for a leaf-list or list
    [<keys>] for a list's keys

 \<type> is the name of the type for leafs and leaf-lists.
  If the type is a leafref, the type is printed as "-> TARGET",
  whereTARGET is the leafref path, with prefixes removed if possible.

 \<if-features> is the list of features this node depends on, printed
     within curly brackets and a question mark "{...}?"

* name-revision - name-revision is printed in following format
\<module_name>@\<revision>

* depend - list of all the modules that validated module depends on

* json-tree - generates a json tree with all the node information

* jstree - generates a html with java script with a yang tree

* yang - generates a yang file (used with simplify will print
the simplified yang file)

## Examples

* To validate module only:

*./lyv \<path_to_the_yang_module>*

* To validate module which dependencies are on different path recursively:

*./lyv -r -p \<path_to_module_dependencies>
\<path_to_the_yang_module>*

* To create formatted yang tree:

*./lyv -f tree \<path_to_the_yang_module>*

* To create formatted yang jstree:

*./lyv -f jstree \<path_to_the_yang_module>*

* To simplify and print yang file based on xml:

*./lyv -o \<path_to_output_directory> -s
\<path_to_xml_files> -p \<path_to_yang_modules> -f yang*
