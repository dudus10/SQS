import java.time.LocalDateTime

def dt = LocalDateTime.now()

def tfstatebucket = ""
def tfstatepath = ""
def sqsparam = ""

pipeline {
    // timestamp = Date().getTime()
    // def bucket_name = "backet${timestamp}"
    
    environment { 
        
        AWS_ACCESS_KEY_ID = credentials('AWS_ACCESS_KEY_ID')
        AWS_SECRET_ACCESS_KEY = credentials('AWS_SECRET_ACCESS_KEY')
        AWS_DEFAULT_REGION = "eu-west-1"
        
    }    
    
    parameters {
        string(name: 'tf_state_bucket', defaultValue: '', description: "")
        string(name: 'tf_state_path', defaultValue: '', description: "")
        string(name: 'sqs_name', defaultValue: '', description: "")
    }
    
	agent any

	stages {
	    
        
        
	    stage('Cloning repo from github') {
            steps {
                sh 'pwd'

		        sh 'rm -rf TF/SQS'

                git branch: 'main', url: 'git@github.com:dudus10/SQS_private.git'
            }
        }
        

		stage("Create SQS Queue") {
		    steps {
		        script {
		            print(dt)
		            dir('TF/SQS') {
		                if (params.tf_state_bucket != "") {
		                    tfstatebucket = params.tf_state_bucket
		                }
		                
		                if (params.tf_state_path != "") {
		                    tfstatepath = params.tf_state_path
		                }
		                
		                if (params.sqs_name != "") {
		                    print("SQS Name provided, name is: " + params.sqs_name)
		                    sqsparam = '-var="sqs_name=' + params.sqs_name + '"'
		                }
		                // sqsparam = '-var="sqs_name=cc11"'
		                // tfstatebucket = params.tf_state_bucket
		                // tfstatepath = "project/s3statefile_sqs-30.tfstate"
		                
		            sh """#!/bin/bash -x
		                
		                echo "tf_state_bucket - ${tfstatebucket}"
		                echo "tf_state_path - ${tfstatepath}"
		                
		                terraform init -backend-config=\"key=${tfstatepath}\" -backend-config=\"bucket=${tfstatebucket}\"
		                
		                echo ${sqsparam}
		                
		                terraform plan ${sqsparam}
		                
		                terraform apply -auto-approve ${sqsparam}
		                
		                sleep 5
		                
		                terraform output -json > sqs_values_${env.BUILD_NUMBER}.json
		                aws s3 cp sqs_values_${env.BUILD_NUMBER}.json s3://${tfstatebucket}/cm-values/sqs_values_${env.BUILD_NUMBER}.json

		              """
		            }
		        }
		    }
		}
			
	}
}

