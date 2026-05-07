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
    booleanParam(name: 'BUILD_DOCKER_IMAGES', defaultValue: false, description: 'Build backend and frontend Docker images')
    booleanParam(name: 'PUSH_DOCKER_IMAGES', defaultValue: false, description: 'Push Docker images to GitHub Container Registry')
    string(name: 'DOCKER_NAMESPACE', defaultValue: 'mzahii', description: 'GHCR namespace/owner, lowercase recommended')
    string(name: 'IMAGE_TAG', defaultValue: '', description: 'Docker image tag. Empty uses build-${BUILD_NUMBER}')
  }

  environment {
    JAVA_HOME = tool(name: 'jdk17', type: 'jdk')
    PATH = "${JAVA_HOME}/bin:${env.PATH}"
    DOCKER_REGISTRY = 'ghcr.io'
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
        timeout(time: 30, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true
        }
      }
    }

    stage('Docker Build Images') {
      when {
        expression { return params.BUILD_DOCKER_IMAGES || params.PUSH_DOCKER_IMAGES }
      }
      steps {
        script {
          def imageTag = params.IMAGE_TAG?.trim()
          if (!imageTag) {
            imageTag = "build-${env.BUILD_NUMBER}"
          }
          env.IMAGE_TAG_EFFECTIVE = imageTag
          env.DOCKER_NAMESPACE_EFFECTIVE = params.DOCKER_NAMESPACE.trim().toLowerCase()
        }
        sh '''
          docker version

          docker build \
            -t nephropaidi-api-gateway:${IMAGE_TAG_EFFECTIVE} \
            -t ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE_EFFECTIVE}/nephropaidi-api-gateway:${IMAGE_TAG_EFFECTIVE} \
            -f BackEnd/api-gateway/Dockerfile \
            BackEnd/api-gateway

          docker build \
            -t nephropaidi-frontend:${IMAGE_TAG_EFFECTIVE} \
            -t ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE_EFFECTIVE}/nephropaidi-frontend:${IMAGE_TAG_EFFECTIVE} \
            -f FrontEnd/Dockerfile \
            FrontEnd
        '''
      }
    }

    stage('Docker Push Images') {
      when {
        expression { return params.PUSH_DOCKER_IMAGES }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: 'github-pat', usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_TOKEN')]) {
          sh '''
            set +x
            echo "$REGISTRY_TOKEN" | docker login ${DOCKER_REGISTRY} -u "$REGISTRY_USER" --password-stdin
            set -x

            docker push ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE_EFFECTIVE}/nephropaidi-api-gateway:${IMAGE_TAG_EFFECTIVE}
            docker push ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE_EFFECTIVE}/nephropaidi-frontend:${IMAGE_TAG_EFFECTIVE}

            docker logout ${DOCKER_REGISTRY}
          '''
        }
      }
    }
  }
}
