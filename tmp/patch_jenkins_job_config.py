from pathlib import Path

path = Path("tmp/job-config.xml")
text = path.read_text(encoding="utf-8")

text = text.replace("<lightweight>true</lightweight>", "<lightweight>false</lightweight>")

tracker_old = """<string>RUN_FRONTEND_TESTS</string>
        <string>IMAGE_TAG</string>
        <string>RUN_SONAR</string>"""
tracker_new = """<string>RUN_FRONTEND_TESTS</string>
        <string>IMAGE_TAG</string>
        <string>RUN_SONAR</string>
        <string>RUN_SONAR_SERVICE_BREAKDOWN</string>"""
text = text.replace(tracker_old, tracker_new)

param_anchor = """<hudson.model.BooleanParameterDefinition>
          <name>RUN_SONAR</name>
          <description>Run SonarQube analysis</description>
          <defaultValue>true</defaultValue>
        </hudson.model.BooleanParameterDefinition>"""
param_insert = param_anchor + """
        <hudson.model.BooleanParameterDefinition>
          <name>RUN_SONAR_SERVICE_BREAKDOWN</name>
          <description>Also publish one SonarQube project per backend service</description>
          <defaultValue>false</defaultValue>
        </hudson.model.BooleanParameterDefinition>"""
text = text.replace(param_anchor, param_insert)

path.write_text(text, encoding="utf-8")
