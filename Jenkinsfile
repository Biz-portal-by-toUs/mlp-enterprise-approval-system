pipeline {
    agent any

    options { disableConcurrentBuilds() }

    environment {
        AWS_REGION       = "us-west-1"
        AWS_DEFAULT_REGION = "us-west-1"

        ECR_REPO_URI     = "118320467932.dkr.ecr.us-west-1.amazonaws.com/terraform-ecr"
        IMAGE_TAG        = "${BUILD_NUMBER}"

        EKS_CLUSTER_NAME = "terraform-eks-cluster"
        K8S_NAMESPACE    = "bizportal"
    }

    stages {
        stage('Build') {
            steps {
                sh '''
                  chmod +x gradlew
                  ./gradlew clean build -x test
                '''
            }
        }

        stage('Docker Build & Push (ECR)') {
            steps {
                withCredentials([
                    string(credentialsId: 'AWS_ACCESS_KEY', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'AWS_SECRET_KEY', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh '''
                      aws sts get-caller-identity

                      aws ecr get-login-password --region ${AWS_REGION} \
                        | docker login --username AWS --password-stdin ${ECR_REPO_URI}

                      docker build -t ${ECR_REPO_URI}:${IMAGE_TAG} .
                      docker push ${ECR_REPO_URI}:${IMAGE_TAG}
                    '''
                }
            }
        }

        stage('Deploy Infra (Redis / MongoDB / Elasticsearch / Weaviate)') {
            steps {
                withCredentials([
                    string(credentialsId: 'AWS_ACCESS_KEY', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'AWS_SECRET_KEY', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh '''
                      aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER_NAME}

                      kubectl apply -f k8s/00-namespace.yaml

                      kubectl apply -f k8s/redis.yaml
                      kubectl apply -f k8s/mongodb.yaml
                      kubectl apply -f k8s/elasticsearch.yaml
                      kubectl apply -f k8s/weaviate.yaml

                      kubectl -n ${K8S_NAMESPACE} get pods
                      kubectl -n ${K8S_NAMESPACE} get svc
                      kubectl -n ${K8S_NAMESPACE} get pvc || true
                    '''
                }
            }
        }

        stage('Apply App Config (ConfigMap + Secrets)') {
            steps {
                withCredentials([
                    string(credentialsId: 'AWS_ACCESS_KEY', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'AWS_SECRET_KEY', variable: 'AWS_SECRET_ACCESS_KEY'),

                    string(credentialsId: 'DB_PROD_URL', variable: 'DB_PROD_URL'),
                    string(credentialsId: 'DB_PROD_USER', variable: 'DB_PROD_USER'),
                    string(credentialsId: 'DB_PROD_PASSWORD', variable: 'DB_PROD_PASSWORD'),

                    string(credentialsId: 'JWT_SECRET', variable: 'JWT_SECRET'),
                    string(credentialsId: 'JWT_ISSUER', variable: 'JWT_ISSUER')
                ]) {
                    sh '''
                      aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER_NAME}

                      # ConfigMap(민감정보 없음)
                      kubectl apply -f k8s/app-configmap.yaml

                      # ✅ Secret 업서트(민감정보)
                      kubectl -n ${K8S_NAMESPACE} create secret generic bizportal-secrets \
                        --from-literal=DB_PROD_URL="${DB_PROD_URL}" \
                        --from-literal=DB_PROD_USER="${DB_PROD_USER}" \
                        --from-literal=DB_PROD_PASSWORD="${DB_PROD_PASSWORD}" \
                        --from-literal=JWT_SECRET="${JWT_SECRET}" \
                        --from-literal=JWT_ISSUER="${JWT_ISSUER}" \
                        --dry-run=client -o yaml | kubectl apply -f -
                    '''
                }
            }
        }

        stage('Deploy App') {
            steps {
                withCredentials([
                    string(credentialsId: 'AWS_ACCESS_KEY', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'AWS_SECRET_KEY', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh '''
                      aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER_NAME}

                      envsubst < k8s/app-deploy.yaml | kubectl apply -f -

                      kubectl -n ${K8S_NAMESPACE} get pods -o wide
                    '''
                }
            }
        }

        stage('Deploy Ingress (ALB)') {
            steps {
                withCredentials([
                    string(credentialsId: 'AWS_ACCESS_KEY', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'AWS_SECRET_KEY', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh '''
                      aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER_NAME}

                      kubectl apply -f k8s/ingress.yaml
                      kubectl -n ${K8S_NAMESPACE} get ingress
                    '''
                }
            }
        }
    }

    post {
        success { echo "✅ Bizportal deploy success!" }
        failure { echo "❌ Bizportal deploy failed" }
    }
}