import java.time.LocalDateTime

def dt = LocalDateTime.now()

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
        string(name: 'tf_state_bucket', defaultValue: '', description: "Bucket to save TF state") 
        string(name: 'tf_state_path', defaultValue: '', description: "TF state file path to create for S3 TF state bucket ")
        string(name: 'eks_name', defaultValue: '', description: "EKS name")
        // string(name: 'sqs_bucket_name', defaultValue: '', description: "New bucket name SQS messages")
    }
    
	agent any

	stages {
      
	    stage('Cloning repo from github') {
	    steps {
                git branch: 'main', url: 'git@github.com:dudus10/SQS_private.git'
            }
	    }
		  
			stage("Create EKS Clutster") {
		    steps {
		        script {
		            print(dt)
		            dir('TF/EKS') {
		                if (params.tf_state_bucket != "") {
		                    tfstatebucket = params.tf_state_bucket
		                }
		                
		                if (params.tf_state_path != "") {
		                    tfstatepath = params.tf_state_path
		                }


		            sh """#!/bin/bash -x
		                pwd
		                ls -lta
                        EKS_NAME=${params.eks_name}
                        echo $EKS_NAME
                        sed -i "s#EKS-CLUSTER-NAME#${EKS_NAME}#" terraform.tfvars

                        sed -i "s#WORKERNODES-IAM-ROLE#${EKS_NAME}_workernodes_iam_role#" terraform.tfvars

                        sed -i "s#EKS-CLUSTER-IAM-ROLE#${EKS_NAME}_iam_role#" terraform.tfvars
                        sed -i "s#EKS-NODE-GROUP-NAME#${EKS_NAME}_node_group#" terraform.tfvars
                        sed -i "s#EKS-VPC-NAME#${EKS_NAME}_vpc#" terraform.tfvars

	                    cat terraform.tfvars
	                    
		                echo "tf_state_bucket - ${tfstatebucket}"
		                echo "tf_state_path - ${tfstatepath}"
		                
		                terraform init -backend-config=\"key=${tfstatepath}\" -backend-config=\"bucket=${tfstatebucket}\"

                        terraform plan

                        terraform apply -auto-approve

		                #sleep 5

		                terraform output -json > eks_values_${env.BUILD_NUMBER}.json
		                aws s3 cp eks_values_${env.BUILD_NUMBER}.json s3://${tfstatebucket}/cm-values/eks_values_${env.BUILD_NUMBER}.json

		              """
		            }
		        }
		    }
		}
	}
}






