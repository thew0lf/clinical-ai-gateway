{{- define "clinical-ai-gateway.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "clinical-ai-gateway.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "clinical-ai-gateway.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "clinical-ai-gateway.labels" -}}
app.kubernetes.io/name: {{ include "clinical-ai-gateway.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "clinical-ai-gateway.selectorLabels" -}}
app.kubernetes.io/name: {{ include "clinical-ai-gateway.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "clinical-ai-gateway.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "clinical-ai-gateway.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}
