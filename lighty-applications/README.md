# lighty.io applications

Looking for lighty.io applications to solve your SDN problems?
Check applications inside of this artifact.

Applications provide nice and small out-of-the-box microservice solutions
for certain use-cases. Depending on your needs they can be started
as a standalone java applications, as a Docker containers
or deployed into Kubernetes environment using Helm charts.

Or if our pre-prepared applications do not fit your needs, you can use
them as an inspiration to create your custom applications.

For more information about applications check their READMEs or code directly.

### App naming convention
Apps are organized in following folders and naming structure:

```
{{app-name}}-app-aggregator
|-- {{app-name}}-app
|-- {{app-name}}-app-docker
|-- {{app-name}}-app-helm
|   | -- helm
|   |    | -- {{app-name}}-app-helm
|   |    |    | -- values.yaml
|-- {{app-name}}-module
```
