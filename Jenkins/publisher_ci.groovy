pipeline {
    environment { 
        registry_url = "https://index.docker.io/v1/"
        registry = "dudus/sqs_publisher"
        registryCredential = 'dockerhub_credentials'
        dockerImage = ""
        
        AWS_ACCESS_KEY_ID = credentials('AWS_ACCESS_KEY_ID')
        AWS_SECRET_ACCESS_KEY = credentials('AWS_SECRET_ACCESS_KEY')
        AWS_DEFAULT_REGION = "eu-west-1"
        
    }    
    
	agent any

	stages {
	    
	    stage('Cloning repo from github') {
            steps {
                git branch: 'main', url: 'git@github.com:dudus10/SQS_private.git'

            }
        }
        
        
		stage('Building docker image') { 
			steps { 
				script { 

						dockerImage = docker.build(registry + ":${env.BUILD_NUMBER}", "-t " + registry + ":latest ./Microservices/sqs_publisher/Docker") 
						print(dockerImage)
					}
				} 
			}
			
		stage('Push') {
			steps {
				script {
					docker.withRegistry(registry_url, registryCredential) {
						dockerImage.push()
						dockerImage.push('latest')
					}
				}
			}
		}
		
		stage('Set configmaps value') {
			steps {
				script {
			        print("Set configmaps value")
						dir('Microservices/sqs_publisher') {       
						sh """#!/bin/bash -x
							LAST_INFRA_BUILD=`cat ${WORKSPACE}/cm_values/last_build_number.txt`
							SQS_QUEUE_URL=\$(jq .sqs_url.value ${WORKSPACE}/cm_values/values_\$LAST_INFRA_BUILD/cm-values_\$LAST_INFRA_BUILD.json | tr -d '"') 
							S3_BUCKET_NAME=\$(jq .bucket_name.value ${WORKSPACE}/cm_values/values_\$LAST_INFRA_BUILD/cm-values_\$LAST_INFRA_BUILD.json | tr -d '"')                            
                            
							sed -i 's#SQS_QUEUE#'\$SQS_QUEUE_URL'#' ./Chart/values.yaml 
							#sed -i 's/SQS_MSGS_FILE/sqs_messages/sqs_messages.json/' ./Chart/values.yaml 
							sed -i 's/S3_BUCKET_NAME/'\$S3_BUCKET_NAME'/' ./Chart/values.yaml 
							sed -i 's/REGION/eu-west-1/' ./Chart/values.yaml 
						"""
						}
				}
			}
		}


		stage('Packing HELM chart') {
			steps {
				script {
				    dir('Microservices/sqs_publisher') {
				    
				    sh """#!/bin/bash -x
				        if git status | grep -q 'sqs_publisher/Docker'; then helm package Chart --app-version 1.0.${env.BUILD_NUMBER} --version 1.0.${env.BUILD_NUMBER}; else helm package Chart --version 1.0.${env.BUILD_NUMBER}; fi
						ls sqs-publisher-1.0.${env.BUILD_NUMBER}.tgz
						S3_INFRA_BUCKET=`cat ${WORKSPACE}/cm_values/infra_bucket.txt`
						aws s3 cp sqs-publisher-1.0.${env.BUILD_NUMBER}.tgz s3://\$S3_INFRA_BUCKET/helm_charts/sqs-publisher-1.0.${env.BUILD_NUMBER}.tgz
						rm sqs-publisher-1.0.${env.BUILD_NUMBER}.tgz
				    """
				    }
 	
				}
			}
		}
		
	}
}
