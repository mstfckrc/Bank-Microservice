pipeline {
    agent {
        label 'ci-java21-node24'
    }

    options {
        skipDefaultCheckout()
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
    }

    stages {
        stage('Kaynak kodu al') {
            steps {
                checkout scm
                sh 'git rev-parse HEAD'
            }
        }

        stage('CI araclarini dogrula') {
            steps {
                sh '''
                    set -eu
                    java -version
                    javac -version
                    node --version
                    npm --version
                    git --version

                    java_major="$(javac -version 2>&1 | cut -d ' ' -f 2 | cut -d '.' -f 1)"
                    node_major="$(node -p 'process.versions.node.split(".")[0]')"

                    test "$java_major" = "21"
                    test "$node_major" = "24"
                '''
            }
        }

        stage('Frontend bagimliliklarini kur') {
            steps {
                dir('Bank-Demo-Frontend') {
                    sh 'npm ci'
                }
            }
        }

        stage('Frontend lint') {
            steps {
                dir('Bank-Demo-Frontend') {
                    sh 'npm run lint'
                }
            }
        }

        stage('Frontend TypeScript kontrolu') {
            steps {
                dir('Bank-Demo-Frontend') {
                    sh 'npm run typecheck'
                }
            }
        }

        stage('Frontend production build') {
            steps {
                dir('Bank-Demo-Frontend') {
                    sh 'npm run build'
                }
            }
        }

        stage('Bank-Demo-Backend verify') {
            steps {
                dir('Bank-Demo-Backend') {
                    sh 'sh ./mvnw -B -ntp clean verify'
                }
            }
        }

        stage('api-gateway verify') {
            steps {
                dir('api-gateway') {
                    sh 'sh ./mvnw -B -ntp clean verify'
                }
            }
        }

        stage('bank-auth-micro verify') {
            steps {
                dir('bank-auth-micro') {
                    sh 'sh ./mvnw -B -ntp clean verify'
                }
            }
        }

        stage('bank-corporate-micro verify') {
            steps {
                dir('bank-corporate-micro') {
                    sh 'sh ./mvnw -B -ntp clean verify'
                }
            }
        }

        stage('bill-service verify') {
            steps {
                dir('bill-service') {
                    sh 'sh ./mvnw -B -ntp clean verify'
                }
            }
        }

        stage('currency-service verify') {
            steps {
                dir('currency-service') {
                    sh 'sh ./mvnw -B -ntp clean verify'
                }
            }
        }

        stage('discovery-server verify') {
            steps {
                dir('discovery-server') {
                    sh 'sh ./mvnw -B -ntp clean verify'
                }
            }
        }

        stage('notification-service verify') {
            steps {
                dir('notification-service') {
                    sh 'sh ./mvnw -B -ntp clean verify'
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/*-reports/TEST-*.xml'
        }
    }
}
