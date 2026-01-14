pipeline {
    agent any

    options { disableConcurrentBuilds() }

    environment {
        AWS_REGION          = "us-west-1"
        AWS_DEFAULT_REGION  = "us-west-1"

        // ===== Bizportal(Spring) =====
        ECR_REPO_URI        = "118320467932.dkr.ecr.us-west-1.amazonaws.com/terraform-ecr"
        IMAGE_TAG           = "${BUILD_NUMBER}"

        // ===== FastAPI(별도 레포) =====
        FASTAPI_REPO_URL    = "https://github.com/Biz-portal-by-toUs/mlp-enterprise-approval-system-ai.git"   // ✅ 여길 너 레포로
        FASTAPI_BRANCH      = "main"                                                                    // ✅ 브랜치 확인
        FASTAPI_DIR         = "fastapi-repo"                                                             // workspace 안에 체크아웃될 폴더명

        // FastAPI는 ECR repo를 분리한다고 가정 (원하면 terraform-ecr 같이 써도 됨)
        FASTAPI_ECR_REPO_NAME = "terraform-ecr-fastapi"
        FASTAPI_ECR_REPO_URI  = "118320467932.dkr.ecr.us-west-1.amazonaws.com/terraform-ecr-fastapi"

        // FastAPI k8s 매니페스트 위치(기본: Bizportal 레포의 k8s/fastapi.yaml)
        FASTAPI_K8S_MANIFEST  = "k8s/fastapi.yaml"  // ✅ 너가 만든 fastapi.yaml 경로로 맞추기
        FASTAPI_DEPLOY_NAME   = "bizportal-fastapi" // ✅ fastapi.yaml의 Deployment name과 동일하게

        // ===== Cluster =====
        EKS_CLUSTER_NAME    = "terraform-eks-cluster"
        K8S_NAMESPACE       = "bizportal"
    }

    stages {

        stage('Checkout FastAPI Repo (2nd repo)') {
            steps {
                dir("${FASTAPI_DIR}") {
                    deleteDir()
                    // 공개레포면 credentials 없이도 OK
                    git branch: "${FASTAPI_BRANCH}", url: "${FASTAPI_REPO_URL}"
                    sh 'ls -la'
                }
            }
        }

        stage('Build (Bizportal)') {
            steps {
                sh '''
                  chmod +x gradlew
                  ./gradlew clean build -x test
                '''
            }
        }

        stage('Docker Build & Push (Bizportal -> ECR)') {
            steps {
                withCredentials([
                    string(credentialsId: 'AWS_ACCESS_KEY', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'AWS_SECRET_KEY', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh '''
                      set -e
                      aws sts get-caller-identity

                      aws ecr get-login-password --region ${AWS_REGION} \
                        | docker login --username AWS --password-stdin ${ECR_REPO_URI}

                      docker build -t ${ECR_REPO_URI}:${IMAGE_TAG} .
                      docker push ${ECR_REPO_URI}:${IMAGE_TAG}
                    '''
                }
            }
        }

        stage('Docker Build & Push (FastAPI -> ECR)') {
          steps {
            withCredentials([
              string(credentialsId: 'AWS_ACCESS_KEY', variable: 'AWS_ACCESS_KEY_ID'),
              string(credentialsId: 'AWS_SECRET_KEY', variable: 'AWS_SECRET_ACCESS_KEY')
            ]) {
              dir('fastapi-repo') {
                sh '''
                  set -e
                  ls -la   # Dockerfile 존재 확인용

                  aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER_NAME} || true

                  aws ecr get-login-password --region ${AWS_REGION} \
                    | docker login --username AWS --password-stdin ${FASTAPI_ECR_REPO_URI}

                  aws ecr describe-repositories --region ${AWS_REGION} --repository-names ${FASTAPI_ECR_REPO_NAME} \
                    || aws ecr create-repository --region ${AWS_REGION} --repository-name ${FASTAPI_ECR_REPO_NAME}

                  docker build -t ${FASTAPI_ECR_REPO_URI}:${IMAGE_TAG} .
                  docker push ${FASTAPI_ECR_REPO_URI}:${IMAGE_TAG}
                '''
              }
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
                      set -e
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
                      set -e
                      aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER_NAME}

                      kubectl apply -f k8s/app-configmap.yaml

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

        stage('Deploy FastAPI (K8S)') {
            steps {
                withCredentials([
                    string(credentialsId: 'AWS_ACCESS_KEY', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'AWS_SECRET_KEY', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh '''
                      set -e
                      aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER_NAME}

                      # ✅ fastapi.yaml 안에 이미지가 ${FASTAPI_ECR_REPO_URI}:${IMAGE_TAG} 형태로 들어있다고 가정
                      envsubst < ${FASTAPI_K8S_MANIFEST} | kubectl apply -f -

                      kubectl -n ${K8S_NAMESPACE} rollout status deploy/${FASTAPI_DEPLOY_NAME}
                      kubectl -n ${K8S_NAMESPACE} get pods -l app=${FASTAPI_DEPLOY_NAME} -o wide || true
                    '''
                }
            }
        }

        stage('Deploy Bizportal App (K8S)') {
            steps {
                withCredentials([
                    string(credentialsId: 'AWS_ACCESS_KEY', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'AWS_SECRET_KEY', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh '''
                      set -e
                      aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER_NAME}

                      envsubst < k8s/app-deploy.yaml | kubectl apply -f -

                      kubectl -n ${K8S_NAMESPACE} rollout status deploy/bizportal-api
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
                      set -e
                      aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER_NAME}

                      kubectl apply -f k8s/ingress.yaml
                      kubectl -n ${K8S_NAMESPACE} get ingress
                    '''
                }
            }
        }
    }
    post {
      success {
        slackSend(
          channel: "#bizportal",
          message: "✅ SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}\n${env.BUILD_URL}",
          tokenCredentialId: "SLACK_TOKEN"
        )
        echo "✅ Bizportal + FastAPI deploy success!"
      }
      failure {
        slackSend(
          channel: "#bizportal",
          message: "❌ FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}\n${env.BUILD_URL}",
          tokenCredentialId: "SLACK_TOKEN"
        )
        echo "❌ Bizportal + FastAPI deploy failed"
      }
    }
}