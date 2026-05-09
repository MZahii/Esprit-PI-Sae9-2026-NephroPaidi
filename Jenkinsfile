pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  parameters {
    booleanParam(name: 'RUN_FRONTEND_TESTS', defaultValue: false, description: 'Run Angular unit tests in CI')
    booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Run SonarQube analysis')
  }

  environment {
    JAVA_HOME = tool(name: 'jdk17', type: 'jdk')
    PATH = "${JAVA_HOME}/bin:${env.PATH}"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Backend Build + Tests') {
      steps {
        sh '''
          sed -i 's/\r$//' BackEnd/api-gateway/mvnw
          chmod +x BackEnd/api-gateway/mvnw
          BackEnd/api-gateway/mvnw -B -ntp -f BackEnd/pom.xml clean package -DskipTests
        '''
      }
    }

    stage('Frontend Install + Build') {
      steps {
        script {
          def nodeHome = tool(name: 'node20', type: 'jenkins.plugins.nodejs.tools.NodeJSInstallation')
          withEnv(["PATH+NODE=${nodeHome}/bin"]) {
            dir('FrontEnd') {
              sh 'npm ci'
              sh 'npm run build'
            }
          }
        }
      }
    }

    stage('Frontend Tests (Optional)') {
      when {
        expression { return params.RUN_FRONTEND_TESTS }
      }
      steps {
        script {
          def nodeHome = tool(name: 'node20', type: 'jenkins.plugins.nodejs.tools.NodeJSInstallation')
          withEnv(["PATH+NODE=${nodeHome}/bin"]) {
            dir('FrontEnd') {
              sh 'npm run test -- --watch=false --browsers=ChromeHeadless'
            }
          }
        }
      }
    }

    stage('SonarQube Analysis') {
      when {
        expression { return params.RUN_SONAR }
      }
      environment {
        SONAR_TOKEN = credentials('sonarqube-token')
      }
      steps {
        withSonarQubeEnv('sonarqube') {
          sh '''
            BackEnd/api-gateway/mvnw -B -ntp \
              -f BackEnd/pom.xml \
              org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
              -DskipTests \
              -Dsonar.token=$SONAR_TOKEN \
              -Dsonar.host.url=$SONAR_HOST_URL
          '''
        }
      }
    }

    stage('Quality Gate') {
      when {
        expression { return params.RUN_SONAR }
      }
      steps {
        timeout(time: 10, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true
        }
      }
    }
  }
}
