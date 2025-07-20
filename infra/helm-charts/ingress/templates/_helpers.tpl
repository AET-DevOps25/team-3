{{/*
StudyMate specific labels
*/}}
{{- define "studymate.labels" -}}
app.kubernetes.io/name: study-mate
app.kubernetes.io/instance: {{ .Values.teamid }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
StudyMate selector labels
*/}}
{{- define "studymate.selectorLabels" -}}
app.kubernetes.io/name: study-mate
app.kubernetes.io/instance: {{ .Values.teamid }}
{{- end }}

{{/*
StudyMate fullname
*/}}
{{- define "studymate.fullname" -}}
{{- .Values.teamid }}
{{- end }} 