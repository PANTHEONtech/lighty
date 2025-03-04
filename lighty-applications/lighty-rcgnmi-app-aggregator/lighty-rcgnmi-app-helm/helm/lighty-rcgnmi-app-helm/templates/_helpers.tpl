{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "lighty-rcgnmi-app-helm.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "lighty-rcgnmi-app-helm.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "lighty-rcgnmi-app-helm.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "lighty-rcgnmi-app-helm.labels" -}}
app.kubernetes.io/name: {{ include "lighty-rcgnmi-app-helm.name" . }}
helm.sh/chart: {{ include "lighty-rcgnmi-app-helm.chart" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "lighty-rcgnmi-app-helm.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
    {{ default (include "lighty-rcgnmi-app-helm.fullname" .) .Values.serviceAccount.name }}
{{- else -}}
    {{ default "default" .Values.serviceAccount.name }}
{{- end -}}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "lighty-rcgnmi-app-helm.selectorLabels" -}}
app.kubernetes.io/name: {{ include "lighty-rcgnmi-app-helm.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{ .Values.lighty.pekko.discovery.podSelectorName }}: {{ .Values.lighty.pekko.discovery.podSelectorValue }}
{{- end -}}
