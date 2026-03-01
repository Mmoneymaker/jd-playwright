pipeline {
    agent any


    tools {
            maven 'maven3.9' // 对应全局配置里的 Name
            jdk 'jdk17'      // 对应全局配置里的 Name
        }
//    environment {
//        MAVEN_HOME = '/usr/local/maven'
//        JAVA_HOME = '/usr/local/java/openjdk'
//        APP_PORT = '9090'
//    }
        environment {
          APP_PORT = '9090'
        }


    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building the project...'
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Install Playwright Browsers') {
            steps {
                echo 'Installing Playwright chromium...'
                sh 'mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"'
            }
        }

        stage('Start Application') {
            steps {
                echo "Starting Spring Boot application on port ${APP_PORT}..."
                sh "mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=${APP_PORT} &"
                sleep 15
            }
        }

        stage('Run Tests') {
            steps {
                echo 'Running UI + API Hybrid tests...'
                sh 'mvn test'
            }
        }

        stage('Publish Test Results') {
            steps {
                echo 'Publishing test results...'
                junit 'target/surefire-reports/*.xml'
                archiveArtifacts 'target/surefire-reports/*.xml,target/surefire-reports/*.txt'
            }
        }
    }

    post {
        always {
            echo 'Cleaning up...'
            sh 'pkill -f "spring-boot:run" || true'
            sh 'pkill -f "jd-hybrid-test-demo" || true'
        }
        success {
            echo 'Build and tests passed!'
        }
        failure {
            echo 'Build or tests failed!'
        }
    }
}
