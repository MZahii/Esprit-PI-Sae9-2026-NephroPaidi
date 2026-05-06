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
        dir('BackEnd') {
          sh '''
            sed -i 's/\r$//' mvnw
            chmod +x mvnw
            ./mvnw -B -ntp clean verify
          '''
        }
      }
    }

    stage('Frontend Install + Build') {
      steps {
        dir('FrontEnd') {
          sh 'npm ci'
          sh 'npm run build'
        }
      }
    }

    stage('Frontend Tests (Optional)') {
      when {
        expression { return params.RUN_FRONTEND_TESTS }
      }
      steps {
        dir('FrontEnd') {
          sh 'npm run test -- --watch=false --browsers=ChromeHeadless'
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
            sonar-scanner \
              -Dsonar.token=$SONAR_TOKEN
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
