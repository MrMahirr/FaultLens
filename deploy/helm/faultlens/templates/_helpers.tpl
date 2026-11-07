{{- define "faultlens.labels" -}}
app.kubernetes.io/part-of: faultlens
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end }}

{{- define "faultlens.tag" -}}
{{ .Values.image.tag | default .Chart.AppVersion }}
{{- end }}

{{- define "faultlens.podSecurity" -}}
securityContext:
  runAsNonRoot: true
  seccompProfile:
    type: RuntimeDefault
{{- end }}

{{- define "faultlens.containerSecurity" -}}
securityContext:
  allowPrivilegeEscalation: false
  capabilities:
    drop: ["ALL"]
{{- end }}
