import java.time.LocalDateTime

def dt = LocalDateTime.now()

pipeline {
    // timestamp = Date().getTime()
    // def bucket_name = "backet${timestamp}"
    
    environment { 
        AWS_ACCESS_KEY_ID = credentials('AWS_ACCESS_KEY_ID')
        AWS_SECRET_ACCESS_KEY = credentials('AWS_SECRET_ACCESS_KEY')
        AWS_DEFAULT_REGION = "eu-west-1"
    }    
    
    parameters {
        // string(name: 'tf_state_bucket', defaultValue: '', description: "New bucket name for S3 TF state") //Exisiting Bucket name for S3 TF state")
        // string(name: 'tf_state_path', defaultValue: '', description: "TF state file path to create for S3 TF state bucket ")
        string(name: 'bucket_name', defaultValue: '', description: "New bucket name for S3 TF state")
        // string(name: 'sqs_bucket_name', defaultValue: '', description: "New bucket name SQS messages")
        string(name: 'sqs_name', defaultValue: '', description: "SQS name")
        string(name: 'ssm_param_name', defaultValue: '', description: "SSM Parameter name")
        string(name: 'ssm_param_value', defaultValue: '', description: "SSM Parameter value. Leave empty if you want to randomally generate")
    }
    
	agent any

	stages {
	    
        
        
	    stage('Cloning repo from github') {
	    steps {
                git branch: 'main', url: 'git@github.com:dudus10/SQS_private.git'

            }
	    }
		  
	stage("Create S3 bucket to save all infra TF state") {
		steps {
			script {
				print("Create S3 bucket to save all infra TF state")

				build job: 'Create_S3', parameters: [string(name: 'bucket_name', value: params.bucket_name),
													booleanParam(name: 'ignore_backend', value: true)]
				}
			}
		}
		
	stage("Create S3 bucket for SQS messages") {
            steps {
                script {
                    print("Create S3 bucket for SQS messages")
                    build job: 'Create_S3', parameters: [string(name: 'tf_state_bucket', value: params.bucket_name), 
                                                        string(name: 'tf_state_path', value: "project/s3statefile_for_s3forsqs-${env.BUILD_NUMBER}.tfstate"),
                                                        string(name: 'bucket_name', value: "sqs-messages-${env.BUILD_NUMBER}")]
                    }
                }
		}
		
	stage("Create SQS") {
            steps {
                script {
                    print("Create SQS")
                    build job: 'Create_SQS', parameters: [string(name: 'tf_state_bucket', value: params.bucket_name), 
                                                        string(name: 'tf_state_path', value: "project/s3statefile_sqs-${env.BUILD_NUMBER}.tfstate"),
                                                        string(name: 'sqs_name', value: params.sqs_name + "-${env.BUILD_NUMBER}" )]
                    }
                }
		}
		
	stage("Create SSM parameter") {
            steps {
                script {
                    print("Create SSM parameter")
                    build job: 'Create_SSM_Parameter', parameters: [string(name: 'tf_state_bucket', value: params.bucket_name), 
                                                        string(name: 'tf_state_path', value: "project/s3statefile_ssm-${env.BUILD_NUMBER}.tfstate"),
                                                        string(name: 'ssm_param_name', value: params.ssm_param_name + "-${env.BUILD_NUMBER}" )]
                                                        //string(name: 'ssm_param_value', value: "-${env.BUILD_NUMBER}")]
                    }
                }
		}
		
	stage("Merging JSON values files") {
            steps {
                script {
                    sh """#!/bin/bash -x
                        pwd
                        git status
                        ls -ltra
                        cd cm_values
                        mkdir values_${env.BUILD_NUMBER}
                        aws s3 sync s3://${params.bucket_name}/ /tmp/values_${env.BUILD_NUMBER}/
                        jq -s 'add' /tmp/values_${env.BUILD_NUMBER}/cm-values/*.json > ${WORKSPACE}/cm_values/values_${env.BUILD_NUMBER}/cm-values_${env.BUILD_NUMBER}.json
                    """
                    }
                }
	}

        stage('Push to GitHub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'github_credentials', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
                        sh """#!/bin/bash -x
                            pwd
                            cd cm_values
                            git config --global user.name "$GIT_USERNAME"
                            git config --global user.password "$GIT_PASSWORD"
                            git remote set-url origin https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/dudus10/SQS_private.git
                            git status
                            git add ./values_${env.BUILD_NUMBER}
                            echo "${env.BUILD_NUMBER}" > ./last_build_number.txt
                            echo "${params.bucket_name}" > ./infra_bucket.txt
                            git add ./last_build_number.txt
                            git add ./infra_bucket.txt
                            git status
                            git commit -s -m "Values for ConfigMaps Infra Build ${env.BUILD_NUMBER}"
                            git pull --rebase origin main
                            git push origin main
                        """
                    }
                }
            }
        }
	}
}






