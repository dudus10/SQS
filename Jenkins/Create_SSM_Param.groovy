import java.time.LocalDateTime

def dt = LocalDateTime.now()
def param_value = ""
def param_name = ""
def tfstatebucket = ""
def tfstatepath = ""

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
        string(name: 'ssm_param_name', defaultValue: '', description: "")
        string(name: 'ssm_param_value', defaultValue: '', description: "")
    }
    
	agent any

	stages {
	    
      
        
	    stage('Cloning repo from github') {
            steps {

		        sh 'rm -rf TF/SSM'

                git branch: 'main', url: 'git@github.com:dudus10/SQS_private.git'
            }
        }
        

		stage("Create SSM Parameter") {
		    steps {
		        script {
		            print(dt)
		            dir('TF/SSM') {
		                if (params.tf_state_bucket != "") {
		                    tfstatebucket = params.tf_state_bucket
		              //  } else {
		              //      error("You must provide S3 bucket name for Terraform state file")
		                }
		                
		                if (params.tf_state_path != "") {
		                    tfstatepath = params.tf_state_path
		              //  } else {
		              //      error("You must provide Terraform state file location on S3 bucket")
		                }
		                
		                if (params.ssm_param_name != "") {
		                    print("SSM Paramter name provided, name is: " + params.ssm_param_name)
		                    param_name = '-var="param_name=' + params.ssm_param_name + '"'
		                }
		                
		                if (params.ssm_param_value != "") {
		                    print("SSM Paramter value provided, value is: " + params.ssm_param_value)
		                    param_value = '-var="param_value=' + params.ssm_param_value + '"'
		                }
		                
		            sh """#!/bin/bash -x
		                echo "tf_state_bucket - ${tfstatebucket}"
		                echo "tf_state_path - ${tfstatepath}"
		                
		                terraform init -backend-config=\"key=${tfstatepath}\" -backend-config=\"bucket=${tfstatebucket}\"

		                terraform plan ${param_name} ${param_value}
		                
		                terraform apply -auto-approve ${param_name} ${param_value}

      					sleep 5

    		            terraform output -json > ssm_values_${env.BUILD_NUMBER}.json
		                aws s3 cp ssm_values_${env.BUILD_NUMBER}.json s3://${tfstatebucket}/cm-values/ssm_values_${env.BUILD_NUMBER}.json
		  
		              """
		            }
		        }
		    }
		}

	}
}

