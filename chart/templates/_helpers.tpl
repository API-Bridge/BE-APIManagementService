{{/*
Expand the name of the chart.
*/}}
{{- define "api-management-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "api-management-service.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "api-management-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "api-management-service.labels" -}}
helm.sh/chart: {{ include "api-management-service.chart" . }}
{{ include "api-management-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "api-management-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "api-management-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "api-management-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "api-management-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the config map
*/}}
{{- define "api-management-service.configMapName" -}}
{{- printf "%s-config" (include "api-management-service.fullname" .) }}
{{- end }}

{{/*
Create the name of the secret
*/}}
{{- define "api-management-service.secretName" -}}
{{- printf "%s-secret" (include "api-management-service.fullname" .) }}
{{- end }}

{{/*
Create the name of the service monitor
*/}}
{{- define "api-management-service.serviceMonitorName" -}}
{{- printf "%s-monitor" (include "api-management-service.fullname" .) }}
{{- end }}

{{/*
Create the name of the network policy
*/}}
{{- define "api-management-service.networkPolicyName" -}}
{{- printf "%s-network-policy" (include "api-management-service.fullname" .) }}
{{- end }}

{{/*
Create the name of the pod disruption budget
*/}}
{{- define "api-management-service.podDisruptionBudgetName" -}}
{{- printf "%s-pdb" (include "api-management-service.fullname" .) }}
{{- end }}
